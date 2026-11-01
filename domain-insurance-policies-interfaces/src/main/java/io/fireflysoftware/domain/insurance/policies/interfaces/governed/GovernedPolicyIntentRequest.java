package io.fireflysoftware.domain.insurance.policies.interfaces.governed;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fireflysoftware.core.insurance.policies.interfaces.binding.PolicyBindingValues;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import java.util.UUID;

/** Exactly one typed operation selector; caller and deployment scope come from current trusted context. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(requiredProperties={"operation"})
public record GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation operation,GovernedPolicyBindRequest bindRequest,
        UUID policyId,GovernedPolicyIssueRequest issueRequest) {
    public GovernedPolicyIntentRequest {
        Objects.requireNonNull(operation);
        if(operation==GovernedPolicyPreparedIntent.Operation.BIND){Objects.requireNonNull(bindRequest);if(policyId!=null || issueRequest!=null)throw invalid();}
        else{PolicyBindingValues.id(policyId);Objects.requireNonNull(issueRequest);if(bindRequest!=null)throw invalid();}
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Exactly one typed governed policy intent required");}
    @Override public String toString(){return "GovernedPolicyIntentRequest["+operation+"]";}
}
