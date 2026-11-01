package io.fireflysoftware.domain.insurance.policies.interfaces.governed;

import io.fireflysoftware.core.insurance.policies.interfaces.binding.PolicyBindingValues;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import java.util.UUID;

/** A pure owner-codec correlation value; grants no authority, reserves nothing and proves no policy state. */
@Schema(requiredProperties={"operation","actionId","intentDigest"})
public record GovernedPolicyPreparedIntent(Operation operation,UUID actionId,String intentDigest) {
    public enum Operation {BIND,ISSUE}
    public GovernedPolicyPreparedIntent {Objects.requireNonNull(operation);PolicyBindingValues.id(actionId);PolicyBindingValues.digest(intentDigest);}
    @Override public String toString(){return "GovernedPolicyPreparedIntent["+operation+"]";}
}
