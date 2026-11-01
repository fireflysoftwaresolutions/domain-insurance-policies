package io.fireflysoftware.domain.insurance.policies.interfaces.governed;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import java.util.UUID;

/** A Core receipt for local sandbox state, not a carrier or premium/accounting effect. */
@Schema(requiredProperties={"policyId","policyNumber","policyStatus","bindingVersion","bindingReceiptDigest","acceptanceActionId","acceptanceId","offerId","offerVersion","termsDigest"})
public record GovernedPolicyBindingResult(UUID policyId,String policyNumber,PolicyState policyStatus,long bindingVersion,
        String bindingReceiptDigest,UUID acceptanceActionId,UUID acceptanceId,UUID offerId,long offerVersion,String termsDigest) {
    public enum PolicyState {BOUND,IN_FORCE}
    public GovernedPolicyBindingResult {
        Objects.requireNonNull(policyStatus);
        var checked=new io.fireflysoftware.core.insurance.policies.interfaces.binding.GovernedPolicyBindingResult(policyId,policyNumber,
                io.fireflysoftware.core.insurance.policies.interfaces.enums.PolicyStatus.valueOf(policyStatus.name()),bindingVersion,
                bindingReceiptDigest,acceptanceActionId,acceptanceId,offerId,offerVersion,termsDigest);
        policyNumber=checked.policyNumber();
    }
    @Override public String toString(){return "GovernedPolicyBindingResult[redacted]";}
}
