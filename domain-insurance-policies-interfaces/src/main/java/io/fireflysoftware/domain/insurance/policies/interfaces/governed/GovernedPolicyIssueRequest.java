package io.fireflysoftware.domain.insurance.policies.interfaces.governed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(requiredProperties={"actionId","expectedBindingVersion","bindingReceiptDigest"})
public record GovernedPolicyIssueRequest(UUID actionId,long expectedBindingVersion,String bindingReceiptDigest) {
    public GovernedPolicyIssueRequest {
        new io.fireflysoftware.core.insurance.policies.interfaces.binding.GovernedPolicyIssueRequest(actionId,expectedBindingVersion,bindingReceiptDigest);
    }
    @JsonIgnore @Schema(hidden=true)
    public String intentDigest(UUID policyId){
        return new io.fireflysoftware.core.insurance.policies.interfaces.binding.GovernedPolicyIssueRequest(actionId,expectedBindingVersion,bindingReceiptDigest).intentDigest(policyId);
    }
    @Override public String toString(){return "GovernedPolicyIssueRequest[redacted]";}
}
