package io.fireflysoftware.domain.insurance.policies.interfaces.governed;

import io.fireflysoftware.core.insurance.policies.interfaces.binding.PolicyBindingValues;
import java.util.Objects;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** Implementations acquire a fresh native caller-bound Core client; they must not retain shared bearer state. */
public interface GovernedPolicyTransport {
    Mono<CallerBound> requireCurrent(Intent intent);
    default Mono<IntentCaller> requireIntentCaller(Action action){return Mono.error(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Trusted intent caller required"));}

    interface IntentCaller {
        UUID partyId();
        Mono<Void> revalidate();
    }

    enum Action {BIND,ISSUE,RECOVER}
    record Intent(Action action,UUID actionId,String intentDigest,UUID policyId) {
        public Intent {
            Objects.requireNonNull(action);PolicyBindingValues.id(actionId);PolicyBindingValues.digest(intentDigest);
            if(action==Action.ISSUE)PolicyBindingValues.id(policyId);
            else if(policyId!=null)throw new IllegalArgumentException("Exact governed policy transport intent required");
        }
        @Override public String toString(){return "GovernedPolicyTransportIntent["+action+"]";}
    }

    /** Revalidation refuses a changed/expired caller or server binding before dispatch and receipt disclosure. */
    interface CallerBound {
        Mono<Void> revalidate();
        Mono<GovernedPolicyCommandResponse> bind(GovernedPolicyBindRequest request);
        Mono<GovernedPolicyCommandResponse> issue(UUID policyId,GovernedPolicyIssueRequest request);
        Mono<GovernedPolicyRecoveryResponse> recover(UUID actionId,String intentDigest);
    }
}
