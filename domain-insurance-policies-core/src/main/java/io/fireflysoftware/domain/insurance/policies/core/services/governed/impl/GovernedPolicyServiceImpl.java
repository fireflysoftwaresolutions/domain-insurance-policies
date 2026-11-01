package io.fireflysoftware.domain.insurance.policies.core.services.governed.impl;

import io.fireflysoftware.core.insurance.policies.interfaces.binding.PolicyBindingValues;
import io.fireflysoftware.domain.insurance.policies.core.services.governed.GovernedPolicyService;
import io.fireflysoftware.domain.insurance.policies.interfaces.governed.*;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** One caller-bound transport invocation per subscription; Core owns every durable effect. */
public final class GovernedPolicyServiceImpl implements GovernedPolicyService {
    private final GovernedPolicyTransport transport;
    public GovernedPolicyServiceImpl(GovernedPolicyTransport transport){this.transport=Objects.requireNonNull(transport);}

    @Override public Mono<GovernedPolicyPreparedIntent> prepare(GovernedPolicyIntentRequest request){return Mono.defer(()->{
        Objects.requireNonNull(request);
        var action=request.operation()==GovernedPolicyPreparedIntent.Operation.BIND?GovernedPolicyTransport.Action.BIND:GovernedPolicyTransport.Action.ISSUE;
        return Mono.defer(()->transport.requireIntentCaller(action)).switchIfEmpty(Mono.error(unavailable())).flatMap(caller->
                Mono.defer(caller::revalidate).then(Mono.fromSupplier(()->{
                    if(request.operation()==GovernedPolicyPreparedIntent.Operation.BIND){
                        var bind=request.bindRequest();if(!bind.partyId().equals(caller.partyId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Current signed policy party required");
                        return new GovernedPolicyPreparedIntent(request.operation(),bind.actionId(),bind.intentDigest());
                    }
                    var issue=request.issueRequest();return new GovernedPolicyPreparedIntent(request.operation(),issue.actionId(),issue.intentDigest(request.policyId()));
                })).flatMap(value->Mono.defer(caller::revalidate).thenReturn(value)));
    });}

    @Override public Mono<GovernedPolicyCommandResponse> bind(GovernedPolicyBindRequest request){return Mono.defer(()->{
        Objects.requireNonNull(request);
        var intent=new GovernedPolicyTransport.Intent(GovernedPolicyTransport.Action.BIND,request.actionId(),request.intentDigest(),null);
        return current(intent).flatMap(client->Mono.defer(client::revalidate).then(command(client,intent,()->client.bind(request),result->
                request.policyNumber().equals(result.policyNumber()) && request.offerId().equals(result.offerId())
                && request.offerVersion()==result.offerVersion() && request.termsDigest().equals(result.termsDigest())
                && request.acceptanceActionId().equals(result.acceptanceActionId()))));
    });}

    @Override public Mono<GovernedPolicyCommandResponse> issue(UUID policyId,GovernedPolicyIssueRequest request){return Mono.defer(()->{
        Objects.requireNonNull(request);
        var intent=new GovernedPolicyTransport.Intent(GovernedPolicyTransport.Action.ISSUE,request.actionId(),request.intentDigest(policyId),policyId);
        return current(intent).flatMap(client->Mono.defer(client::revalidate).then(command(client,intent,()->client.issue(policyId,request),result->policyId.equals(result.policyId()))));
    });}

    @Override public Mono<GovernedPolicyRecoveryResponse> recover(UUID actionId,String digest){return Mono.defer(()->{
        PolicyBindingValues.id(actionId);PolicyBindingValues.digest(digest);
        var intent=new GovernedPolicyTransport.Intent(GovernedPolicyTransport.Action.RECOVER,actionId,digest,null);
        return current(intent).flatMap(client->Mono.defer(client::revalidate).then(Mono.defer(()->client.recover(actionId,digest)))
                .switchIfEmpty(Mono.error(unavailable())).flatMap(value->{
                    if(!actionId.equals(value.actionId()) || !digest.equals(value.intentDigest()))return Mono.error(invalidResponse());
                    return Mono.defer(client::revalidate).thenReturn(value);
                }));
    });}

    private Mono<GovernedPolicyTransport.CallerBound> current(GovernedPolicyTransport.Intent intent){
        return Mono.defer(()->transport.requireCurrent(intent)).switchIfEmpty(Mono.error(unavailable()));
    }

    private Mono<GovernedPolicyCommandResponse> command(GovernedPolicyTransport.CallerBound client,GovernedPolicyTransport.Intent intent,
            Supplier<Mono<GovernedPolicyCommandResponse>> dispatch,Predicate<GovernedPolicyBindingResult> matches){
        var unknown=new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,intent.actionId(),intent.intentDigest(),null);
        return Mono.defer(dispatch).switchIfEmpty(Mono.just(unknown)).map(value->{
            if(!intent.actionId().equals(value.actionId()) || !intent.intentDigest().equals(value.intentDigest()))return unknown;
            if(value.result()!=null && !matches.test(value.result()))return unknown;
            return value;
        }).onErrorResume(GovernedPolicyOutcomeUnknownException.class,error->Mono.just(unknown)).flatMap(value->{
            if(value.status()!=GovernedPolicyCommandResponse.Status.COMMITTED)return Mono.just(value);
            return Mono.defer(client::revalidate).thenReturn(value).onErrorReturn(new GovernedPolicyCommandResponse(
                    GovernedPolicyCommandResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,intent.actionId(),intent.intentDigest(),null));
        });
    }

    private static ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Trusted governed policy transport is not configured");}
    private static ResponseStatusException invalidResponse(){return new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Exact governed policy response identity required");}
}
