package io.fireflysoftware.domain.insurance.policies.interfaces.governed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

/** Selectors express intent; the Core owner independently obtains authority and accepted terms. */
@Schema(requiredProperties={"actionId","submissionId","partyId","productId","offerId","offerVersion","acceptedControlGeneration","acceptanceActionId","acceptanceIntentDigest","termsDigest","contractId","policyNumber","inceptionDate","expiryDate"})
public record GovernedPolicyBindRequest(UUID actionId,UUID submissionId,UUID partyId,UUID productId,
        UUID offerId,long offerVersion,long acceptedControlGeneration,UUID acceptanceActionId,
        String acceptanceIntentDigest,String termsDigest,UUID contractId,String policyNumber,LocalDate inceptionDate,LocalDate expiryDate) {
    public GovernedPolicyBindRequest {
        var checked=new io.fireflysoftware.core.insurance.policies.interfaces.binding.GovernedPolicyBindRequest(actionId,submissionId,partyId,productId,offerId,offerVersion,acceptedControlGeneration,acceptanceActionId,acceptanceIntentDigest,termsDigest,contractId,policyNumber,inceptionDate,expiryDate);
        policyNumber=checked.policyNumber();
    }
    @JsonIgnore @Schema(hidden=true)
    public io.fireflysoftware.core.insurance.policies.interfaces.binding.GovernedPolicyBindRequest ownerIntent(){
        return new io.fireflysoftware.core.insurance.policies.interfaces.binding.GovernedPolicyBindRequest(actionId,submissionId,partyId,productId,offerId,offerVersion,acceptedControlGeneration,acceptanceActionId,acceptanceIntentDigest,termsDigest,contractId,policyNumber,inceptionDate,expiryDate);
    }
    @JsonIgnore @Schema(hidden=true)
    public String intentDigest(){return ownerIntent().intentDigest();}
    @Override public String toString(){return "GovernedPolicyBindRequest[redacted]";}
}
