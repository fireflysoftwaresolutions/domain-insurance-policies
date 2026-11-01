package io.fireflysoftware.domain.insurance.policies.core.services.governed.impl;

import io.fireflysoftware.domain.insurance.policies.interfaces.governed.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

class GovernedPolicyIntentPreparationTest {
    final UUID party=UUID.randomUUID();final String digest="a".repeat(64);
    GovernedPolicyBindRequest bind(){return new GovernedPolicyBindRequest(UUID.randomUUID(),UUID.randomUUID(),party,UUID.randomUUID(),UUID.randomUUID(),1,1,UUID.randomUUID(),digest,digest,UUID.randomUUID(),"SANDBOX-1",LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));}
    final class TransportFixtureImpl implements GovernedPolicyTransport {
        final AtomicInteger preparations=new AtomicInteger(),mutations=new AtomicInteger(),validations=new AtomicInteger();
        UUID signedParty=party;RuntimeException authorityFailure;int revokeAt=Integer.MAX_VALUE;
        @Override public Mono<CallerBound> requireCurrent(Intent intent){mutations.incrementAndGet();return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));}
        @Override public Mono<IntentCaller> requireIntentCaller(Action action){return Mono.defer(()->{
            preparations.incrementAndGet();if(authorityFailure!=null)return Mono.error(authorityFailure);
            return Mono.just(new IntentCaller(){
                @Override public UUID partyId(){return signedParty;}
                @Override public Mono<Void> revalidate(){return Mono.defer(()->validations.incrementAndGet()>=revokeAt?Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)):Mono.empty());}
            });
        });}
    }
    @Test void bindPreparationUsesTheSingleActualOwnerCodecWithoutMutation(){
        var transport=new TransportFixtureImpl();var bind=bind();var request=new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,bind,null,null);
        StepVerifier.create(new GovernedPolicyServiceImpl(transport).prepare(request)).assertNext(value->{assertEquals(GovernedPolicyPreparedIntent.Operation.BIND,value.operation());assertEquals(bind.actionId(),value.actionId());assertEquals(bind.ownerIntent().intentDigest(),value.intentDigest());}).verifyComplete();assertEquals(1,transport.preparations.get());assertEquals(0,transport.mutations.get());assertEquals(2,transport.validations.get());
    }
    @Test void activationPreparationBindsTheExistingPolicyAndOriginalAction(){
        var transport=new TransportFixtureImpl();var policy=UUID.randomUUID();var issue=new GovernedPolicyIssueRequest(UUID.randomUUID(),1,digest);
        StepVerifier.create(new GovernedPolicyServiceImpl(transport).prepare(new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.ISSUE,null,policy,issue)))
                .assertNext(value->{assertEquals(issue.actionId(),value.actionId());assertEquals(issue.intentDigest(policy),value.intentDigest());assertEquals(GovernedPolicyPreparedIntent.Operation.ISSUE,value.operation());}).verifyComplete();assertEquals(0,transport.mutations.get());
    }
    @Test void missingCurrentAuthorityCannotPrepareAnIntent(){
        var transport=new TransportFixtureImpl();transport.authorityFailure=new ResponseStatusException(HttpStatus.FORBIDDEN);
        StepVerifier.create(new GovernedPolicyServiceImpl(transport).prepare(new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,bind(),null,null))).expectErrorSatisfies(error->assertSame(transport.authorityFailure,error)).verify();assertEquals(0,transport.mutations.get());
    }
    @Test void bodyPartyCannotReplaceTheCurrentSignedParty(){
        var transport=new TransportFixtureImpl();transport.signedParty=UUID.randomUUID();
        StepVerifier.create(new GovernedPolicyServiceImpl(transport).prepare(new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,bind(),null,null))).expectErrorSatisfies(error->assertEquals(HttpStatus.FORBIDDEN,((ResponseStatusException)error).getStatusCode())).verify();assertEquals(0,transport.mutations.get());
    }
    @Test void expiredCurrentCallerCannotReceiveThePreparedDigest(){
        var transport=new TransportFixtureImpl();transport.revokeAt=2;
        StepVerifier.create(new GovernedPolicyServiceImpl(transport).prepare(new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,bind(),null,null))).expectError(ResponseStatusException.class).verify();assertEquals(0,transport.mutations.get());
    }
    @Test void preparingAnIntentDoesNotGrantOrReserveItsMutation(){
        var transport=new TransportFixtureImpl();var service=new GovernedPolicyServiceImpl(transport);var bind=bind();
        StepVerifier.create(service.prepare(new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,bind,null,null)).flatMap(prepared->service.bind(bind))).expectErrorSatisfies(error->assertEquals(HttpStatus.FORBIDDEN,((ResponseStatusException)error).getStatusCode())).verify();assertEquals(1,transport.preparations.get());assertEquals(1,transport.mutations.get());
    }
}
