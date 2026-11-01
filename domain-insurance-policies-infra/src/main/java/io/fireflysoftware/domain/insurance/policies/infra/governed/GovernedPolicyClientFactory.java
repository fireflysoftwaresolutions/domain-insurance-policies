package io.fireflysoftware.domain.insurance.policies.infra.governed;

import io.fireflysoftware.core.insurance.policies.sdk.api.GovernedPoliciesApi;
import java.time.Duration;
import java.util.Set;
import reactor.core.publisher.Mono;

/** A fresh generated client obtained through the current native HUMAN caller's supported exchange. */
public interface GovernedPolicyClientFactory {
    Mono<Client> requireCurrent(Set<String> requiredScopes);
    default Mono<IntentCaller> requireIntentCaller(Set<String> requiredScopes){return Mono.error(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Trusted intent caller required"));}
    interface IntentCaller {
        java.util.UUID partyId();
        Mono<Void> revalidate();
    }
    interface Client {
        GovernedPoliciesApi api();
        Mono<Void> revalidate();
        Duration remaining();
    }
}
