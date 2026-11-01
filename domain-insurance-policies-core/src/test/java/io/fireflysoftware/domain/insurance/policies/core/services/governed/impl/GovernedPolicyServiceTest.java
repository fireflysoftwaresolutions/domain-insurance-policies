package io.fireflysoftware.domain.insurance.policies.core.services.governed.impl;

import io.fireflysoftware.domain.insurance.policies.interfaces.governed.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

class GovernedPolicyServiceTest {
    static final String CALLER="verified-test-caller",DIGEST="a".repeat(64);
    GovernedPolicyBindRequest request(){return new GovernedPolicyBindRequest(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,1,UUID.randomUUID(),DIGEST,DIGEST,UUID.randomUUID(),"SANDBOX-1",LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));}
    GovernedPolicyBindingResult receipt(GovernedPolicyBindRequest request){return new GovernedPolicyBindingResult(UUID.randomUUID(),request.policyNumber(),GovernedPolicyBindingResult.PolicyState.BOUND,1,DIGEST,request.acceptanceActionId(),UUID.randomUUID(),request.offerId(),request.offerVersion(),request.termsDigest());}
    GovernedPolicyCommandResponse outcome(GovernedPolicyBindRequest request,GovernedPolicyCommandResponse.Status status){return new GovernedPolicyCommandResponse(status,request.actionId(),request.intentDigest(),status==GovernedPolicyCommandResponse.Status.COMMITTED?receipt(request):null);}

    static final class TransportFixtureImpl implements GovernedPolicyTransport {
        final List<Intent> intents=new CopyOnWriteArrayList<>();
        final List<String> acquiredCallers=new CopyOnWriteArrayList<>(),dispatchedCallers=new CopyOnWriteArrayList<>();
        final List<GovernedPolicyBindRequest> binds=new CopyOnWriteArrayList<>();
        final List<UUID> issuedPolicies=new CopyOnWriteArrayList<>(),recoveries=new CopyOnWriteArrayList<>();
        final List<GovernedPolicyIssueRequest> issues=new CopyOnWriteArrayList<>();
        final List<String> recoveryDigests=new CopyOnWriteArrayList<>();
        Function<GovernedPolicyBindRequest,Mono<GovernedPolicyCommandResponse>> binding;
        Function<GovernedPolicyIssueRequest,Mono<GovernedPolicyCommandResponse>> issuing;
        Function<Intent,Mono<GovernedPolicyRecoveryResponse>> recovering;
        RuntimeException acquisitionFailure,validationFailure;
        boolean revokeAfterDispatch;
        Mono<Void> gate=Mono.empty();
        @Override public Mono<CallerBound> requireCurrent(Intent intent){return Mono.deferContextual(context->{
            if(acquisitionFailure!=null)return Mono.error(acquisitionFailure);
            String caller=context.get(CALLER);intents.add(intent);acquiredCallers.add(caller);
            return Mono.just(new CallerBound(){
                boolean dispatched;
                @Override public Mono<Void> revalidate(){return Mono.deferContextual(current->{
                    if(!caller.equals(current.getOrDefault(CALLER,"")))return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    if(validationFailure!=null)return Mono.error(validationFailure);
                    if(dispatched && revokeAfterDispatch)return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    return Mono.empty();
                });}
                @Override public Mono<GovernedPolicyCommandResponse> bind(GovernedPolicyBindRequest request){return Mono.defer(()->{
                    dispatched=true;binds.add(request);dispatchedCallers.add(caller);return gate.then(Mono.defer(()->binding.apply(request)));
                });}
                @Override public Mono<GovernedPolicyCommandResponse> issue(UUID policyId,GovernedPolicyIssueRequest request){return Mono.defer(()->{
                    dispatched=true;issuedPolicies.add(policyId);issues.add(request);dispatchedCallers.add(caller);return issuing.apply(request);
                });}
                @Override public Mono<GovernedPolicyRecoveryResponse> recover(UUID actionId,String digest){return Mono.defer(()->{
                    dispatched=true;recoveries.add(actionId);recoveryDigests.add(digest);return recovering.apply(intent);
                });}
            });
        });}
    }
    <T> Mono<T> caller(Mono<T> work,String caller){return work.contextWrite(context->context.put(CALLER,caller));}
    @Test void eachCoreOutcomeRetainsTheExactOriginalRequestActionAndOwnerDigest(){
        var transport=new TransportFixtureImpl();var service=new GovernedPolicyServiceImpl(transport);var body=request();
        for(var status:GovernedPolicyCommandResponse.Status.values()){
            transport.binding=request->Mono.just(outcome(request,status));
            StepVerifier.create(caller(service.bind(body),"human-A")).assertNext(value->{assertEquals(status,value.status());assertEquals(body.actionId(),value.actionId());assertEquals(body.intentDigest(),value.intentDigest());}).verifyComplete();
        }
        assertEquals(4,transport.binds.size());transport.binds.forEach(value->assertSame(body,value));
        transport.intents.forEach(value->{assertEquals(GovernedPolicyTransport.Action.BIND,value.action());assertEquals(body.actionId(),value.actionId());assertEquals(body.intentDigest(),value.intentDigest());assertNull(value.policyId());});
    }
    @Test void possibleDispatchLossReturnsUnknownWithTheSameActionWithoutRetry(){
        var transport=new TransportFixtureImpl();var body=request();transport.binding=request->Mono.error(new GovernedPolicyOutcomeUnknownException());
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).bind(body),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(body.actionId(),value.actionId());assertEquals(body.intentDigest(),value.intentDigest());assertNull(value.result());}).verifyComplete();
        assertEquals(List.of(body),transport.binds);assertTrue(transport.recoveries.isEmpty());
    }
    @Test void configurationFailureBeforeClientAcquisitionIsNotAnUnknownMutation(){
        var transport=new TransportFixtureImpl();transport.acquisitionFailure=new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).bind(request()),"human-A")).expectErrorSatisfies(error->assertSame(transport.acquisitionFailure,error)).verify();assertTrue(transport.binds.isEmpty());
    }
    @Test void authorityRefusalBeforeDispatchCannotCallCore(){
        var transport=new TransportFixtureImpl();transport.validationFailure=new ResponseStatusException(HttpStatus.FORBIDDEN);
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).bind(request()),"human-A")).expectErrorSatisfies(error->assertSame(transport.validationFailure,error)).verify();assertTrue(transport.binds.isEmpty());
    }
    @Test void emptyClientAcquisitionRefusesBeforeAnyMutation(){
        StepVerifier.create(new GovernedPolicyServiceImpl(intent->Mono.empty()).bind(request())).expectErrorSatisfies(error->assertEquals(HttpStatus.SERVICE_UNAVAILABLE,((ResponseStatusException)error).getStatusCode())).verify();
    }
    @Test void anUnrelatedCommandResponseCannotBecomeTheOriginalActionSuccess(){
        var transport=new TransportFixtureImpl();var original=request();transport.binding=request->Mono.just(outcome(request(),GovernedPolicyCommandResponse.Status.COMMITTED));
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).bind(original),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(original.actionId(),value.actionId());assertEquals(original.intentDigest(),value.intentDigest());assertNull(value.result());}).verifyComplete();assertEquals(1,transport.binds.size());
    }
    @Test void emptyMutationResponseRequiresOriginalActionRecovery(){
        var transport=new TransportFixtureImpl();var original=request();transport.binding=request->Mono.empty();
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).bind(original),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(original.actionId(),value.actionId());}).verifyComplete();assertEquals(1,transport.binds.size());
    }
    @Test void aReceiptForAnotherAcceptedOfferCannotBecomeBoundSuccess(){
        var transport=new TransportFixtureImpl();var original=request();
        transport.binding=body->Mono.just(new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.COMMITTED,body.actionId(),body.intentDigest(),receipt(request())));
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).bind(original),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(original.actionId(),value.actionId());assertNull(value.result());}).verifyComplete();assertEquals(1,transport.binds.size());
    }
    @Test void coreHttpRefusalRemainsTheOriginalFailureWithoutRetry(){
        var transport=new TransportFixtureImpl();var refused=new ResponseStatusException(HttpStatus.FORBIDDEN);transport.binding=request->Mono.error(refused);
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).bind(request()),"human-A")).expectErrorSatisfies(error->assertSame(refused,error)).verify();assertEquals(1,transport.binds.size());
    }
    @Test void activationCapturesExistingPolicyActionAndOwnerDigestBeforeTransport(){
        var transport=new TransportFixtureImpl();var policy=UUID.randomUUID();var request=new GovernedPolicyIssueRequest(UUID.randomUUID(),1,DIGEST);
        transport.issuing=body->Mono.just(new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,body.actionId(),body.intentDigest(policy),null));
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).issue(policy,request),"human-A")).assertNext(value->{assertEquals(request.actionId(),value.actionId());assertEquals(request.intentDigest(policy),value.intentDigest());}).verifyComplete();
        assertEquals(List.of(policy),transport.issuedPolicies);assertEquals(List.of(request),transport.issues);assertEquals(new GovernedPolicyTransport.Intent(GovernedPolicyTransport.Action.ISSUE,request.actionId(),request.intentDigest(policy),policy),transport.intents.getFirst());assertTrue(transport.binds.isEmpty());
    }
    @Test void recoveryOnlyObservesTheExactOriginalActionAndDigest(){
        var transport=new TransportFixtureImpl();var action=UUID.randomUUID();transport.recovering=intent->Mono.just(new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,intent.actionId(),intent.intentDigest(),null));
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).recover(action,DIGEST),"human-A")).assertNext(value->{assertEquals(action,value.actionId());assertEquals(DIGEST,value.intentDigest());assertEquals(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,value.state());}).verifyComplete();
        assertEquals(List.of(action),transport.recoveries);assertEquals(List.of(DIGEST),transport.recoveryDigests);assertTrue(transport.binds.isEmpty());assertTrue(transport.issues.isEmpty());
    }
    @Test void activationCannotDiscloseAReceiptForAnotherPolicy(){
        var transport=new TransportFixtureImpl();var policy=UUID.randomUUID();var issue=new GovernedPolicyIssueRequest(UUID.randomUUID(),1,DIGEST);
        transport.issuing=body->Mono.just(new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.COMMITTED,body.actionId(),body.intentDigest(policy),receipt(request())));
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).issue(policy,issue),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(issue.actionId(),value.actionId());assertNull(value.result());}).verifyComplete();assertEquals(List.of(policy),transport.issuedPolicies);
    }
    @Test void recoveryFailureCannotBecomeAnAbsenceObservation(){
        var transport=new TransportFixtureImpl();var failure=new GovernedPolicyOutcomeUnknownException();transport.recovering=intent->Mono.error(failure);
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).recover(UUID.randomUUID(),DIGEST),"human-A")).expectErrorSatisfies(error->assertSame(failure,error)).verify();assertEquals(1,transport.recoveries.size());
    }
    @Test void aRecoveryResponseForAnotherIntentIsRefused(){
        var transport=new TransportFixtureImpl();transport.recovering=intent->Mono.just(new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,UUID.randomUUID(),DIGEST,null));
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).recover(UUID.randomUUID(),DIGEST),"human-A")).expectErrorSatisfies(error->assertEquals(HttpStatus.BAD_GATEWAY,((ResponseStatusException)error).getStatusCode())).verify();assertEquals(1,transport.recoveries.size());
    }
    @Test void recoveryRechecksTheCapturedCallerBeforeDisclosingAnObservation(){
        var transport=new TransportFixtureImpl();transport.revokeAfterDispatch=true;transport.recovering=intent->Mono.just(new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,intent.actionId(),intent.intentDigest(),null));
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).recover(UUID.randomUUID(),DIGEST),"human-A")).expectErrorSatisfies(error->assertEquals(HttpStatus.FORBIDDEN,((ResponseStatusException)error).getStatusCode())).verify();assertEquals(1,transport.recoveries.size());
    }
    @Test void aCommittedReceiptIsWithheldIfTheCapturedCallerIsNoLongerCurrent(){
        var transport=new TransportFixtureImpl();var request=request();transport.revokeAfterDispatch=true;transport.binding=body->Mono.just(outcome(body,GovernedPolicyCommandResponse.Status.COMMITTED));
        StepVerifier.create(caller(new GovernedPolicyServiceImpl(transport).bind(request),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,value.status());assertEquals(request.actionId(),value.actionId());assertEquals(request.intentDigest(),value.intentDigest());assertNull(value.result());}).verifyComplete();assertEquals(1,transport.binds.size());
    }
    @Test void overlappingHumansAcquireSeparateCallerBoundHandles(){
        var transport=new TransportFixtureImpl();var gate=Sinks.<Void>empty();transport.gate=gate.asMono();transport.binding=body->Mono.just(outcome(body,GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN));
        var service=new GovernedPolicyServiceImpl(transport);var left=request();var right=request();
        StepVerifier.create(Mono.zip(caller(service.bind(left),"human-A"),caller(service.bind(right),"human-B"))).expectSubscription()
                .then(()->{assertEquals(List.of("human-A","human-B"),transport.acquiredCallers);assertEquals(List.of("human-A","human-B"),transport.dispatchedCallers);gate.tryEmitEmpty();})
                .assertNext(values->{assertEquals(left.actionId(),values.getT1().actionId());assertEquals(right.actionId(),values.getT2().actionId());assertEquals(left.intentDigest(),values.getT1().intentDigest());assertEquals(right.intentDigest(),values.getT2().intentDigest());}).verifyComplete();
        assertEquals(List.of(left,right),transport.binds);
    }
}
