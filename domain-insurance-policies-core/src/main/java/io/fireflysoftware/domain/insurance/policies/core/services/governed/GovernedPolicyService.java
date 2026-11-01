package io.fireflysoftware.domain.insurance.policies.core.services.governed;

import io.fireflysoftware.domain.insurance.policies.interfaces.governed.*;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** Orchestration only; authority and durable policy writes belong to the Core owner. */
public interface GovernedPolicyService {
    default Mono<GovernedPolicyPreparedIntent> prepare(GovernedPolicyIntentRequest request){return Mono.error(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Trusted owner intent preparation required"));}
    Mono<GovernedPolicyCommandResponse> bind(GovernedPolicyBindRequest request);
    Mono<GovernedPolicyCommandResponse> issue(UUID policyId,GovernedPolicyIssueRequest request);
    Mono<GovernedPolicyRecoveryResponse> recover(UUID actionId,String intentDigest);
}
