package io.fireflysoftware.domain.insurance.policies.core.services.governed.impl;

import io.fireflysoftware.domain.insurance.policies.infra.governed.GovernedPolicyClientFactory;
import io.fireflysoftware.domain.insurance.policies.interfaces.governed.*;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Uses only the actual original-generator Policy API and typed models. */
public final class GeneratedGovernedPolicyTransportImpl implements GovernedPolicyTransport {
    private final GovernedPolicyClientFactory clients;
    public GeneratedGovernedPolicyTransportImpl(GovernedPolicyClientFactory clients){this.clients=Objects.requireNonNull(clients);}
    @Override public Mono<IntentCaller> requireIntentCaller(Action action){return Mono.defer(()->{
        Set<String> scopes=switch(action){case BIND->Set.of("insurance:policies:bind");case ISSUE->Set.of("insurance:policies:issue");default->throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mutation intent operation required");};
        return clients.requireIntentCaller(scopes).map(client->new IntentCaller(){
            @Override public UUID partyId(){return client.partyId();}
            @Override public Mono<Void> revalidate(){return Mono.defer(client::revalidate);}
        });
    });}
    @Override public Mono<CallerBound> requireCurrent(Intent intent){return Mono.defer(()->{
        Objects.requireNonNull(intent);
        Set<String> scopes=switch(intent.action()){
            case BIND->Set.of("insurance:policies:bind");
            case ISSUE->Set.of("insurance:policies:issue");
            case RECOVER->Set.of("insurance:policies:recover","insurance:policies:read");
        };
        return clients.requireCurrent(scopes).map(client->new CallerBound(){
            @Override public Mono<Void> revalidate(){return Mono.defer(client::revalidate);}
            @Override public Mono<GovernedPolicyCommandResponse> bind(GovernedPolicyBindRequest request){return Mono.defer(()->{
                requireIntent(intent,Action.BIND,request.actionId(),request.intentDigest(),null);
                var input=new io.fireflysoftware.core.insurance.policies.sdk.model.GovernedPolicyBindRequest()
                        .actionId(request.actionId()).submissionId(request.submissionId()).partyId(request.partyId()).productId(request.productId())
                        .offerId(request.offerId()).offerVersion(request.offerVersion()).acceptedControlGeneration(request.acceptedControlGeneration())
                        .acceptanceActionId(request.acceptanceActionId()).acceptanceIntentDigest(request.acceptanceIntentDigest()).termsDigest(request.termsDigest())
                        .contractId(request.contractId()).policyNumber(request.policyNumber()).inceptionDate(request.inceptionDate()).expiryDate(request.expiryDate());
                return write(client,()->client.api().bindGovernedPolicyWithHttpInfo(input));
            });}
            @Override public Mono<GovernedPolicyCommandResponse> issue(UUID policyId,GovernedPolicyIssueRequest request){return Mono.defer(()->{
                requireIntent(intent,Action.ISSUE,request.actionId(),request.intentDigest(policyId),policyId);
                var input=new io.fireflysoftware.core.insurance.policies.sdk.model.GovernedPolicyIssueRequest()
                        .actionId(request.actionId()).expectedBindingVersion(request.expectedBindingVersion()).bindingReceiptDigest(request.bindingReceiptDigest());
                return write(client,()->client.api().issueGovernedPolicyWithHttpInfo(policyId,input));
            });}
            @Override public Mono<GovernedPolicyRecoveryResponse> recover(UUID actionId,String digest){return Mono.defer(()->{
                requireIntent(intent,Action.RECOVER,actionId,digest,null);var remaining=client.remaining();
                return Mono.defer(()->client.api().recoverGovernedPolicyBindingWithHttpInfo(actionId,digest,null)).timeout(remaining)
                        .flatMap(reply->reply.getStatusCode().value()==200 && reply.getBody()!=null?Mono.just(reply.getBody()):Mono.error(invalid()))
                        .map(value->new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.valueOf(value.getState().getValue()),value.getActionId(),value.getIntentDigest(),result(value.getResult())))
                        .switchIfEmpty(Mono.error(invalid())).onErrorMap(GeneratedGovernedPolicyTransportImpl::readFailure);
            });}
        });
    });}
    private static Mono<GovernedPolicyCommandResponse> write(GovernedPolicyClientFactory.Client client,
            Supplier<Mono<ResponseEntity<io.fireflysoftware.core.insurance.policies.sdk.model.GovernedPolicyCommandResponse>>> dispatch){return Mono.defer(()->{
        var remaining=client.remaining();
        return Mono.defer(dispatch).timeout(remaining)
                .flatMap(reply->reply.getStatusCode().value()==200 && reply.getBody()!=null?Mono.just(reply.getBody()):Mono.error(new GovernedPolicyOutcomeUnknownException()))
                .map(value->new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.valueOf(value.getStatus().getValue()),value.getActionId(),value.getIntentDigest(),result(value.getResult())))
                .switchIfEmpty(Mono.error(new GovernedPolicyOutcomeUnknownException())).onErrorMap(error->{
                    if(error instanceof WebClientResponseException response && response.getStatusCode().value()!=503 && knownRefusal(response))return new ResponseStatusException(response.getStatusCode(),"Governed policy owner request refused");
                    return new GovernedPolicyOutcomeUnknownException();
                });
    });}
    private static GovernedPolicyBindingResult result(io.fireflysoftware.core.insurance.policies.sdk.model.GovernedPolicyBindingResult value){
        if(value==null)return null;
        return new GovernedPolicyBindingResult(value.getPolicyId(),value.getPolicyNumber(),GovernedPolicyBindingResult.PolicyState.valueOf(value.getPolicyStatus().getValue()),
                value.getBindingVersion(),value.getBindingReceiptDigest(),value.getAcceptanceActionId(),value.getAcceptanceId(),value.getOfferId(),value.getOfferVersion(),value.getTermsDigest());
    }
    private static void requireIntent(Intent intent,Action action,UUID actionId,String digest,UUID policyId){
        if(!intent.equals(new Intent(action,actionId,digest,policyId)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Original governed policy intent required");
    }
    private static boolean knownRefusal(WebClientResponseException response){return response.getStatusCode().is4xxClientError() && response.getStatusCode().value()!=408 || response.getStatusCode().value()==503;}
    private static Throwable readFailure(Throwable error){
        if(error instanceof ResponseStatusException)return error;
        if(error instanceof WebClientResponseException response && knownRefusal(response))return new ResponseStatusException(response.getStatusCode(),"Governed policy recovery refused");
        return invalid();
    }
    private static ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Governed policy observation unavailable");}
}
