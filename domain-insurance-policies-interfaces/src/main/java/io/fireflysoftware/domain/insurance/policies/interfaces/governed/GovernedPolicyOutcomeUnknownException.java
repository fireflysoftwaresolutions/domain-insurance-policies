package io.fireflysoftware.domain.insurance.policies.interfaces.governed;

/** Only an adapter that may already have dispatched a mutation may emit this signal. */
public final class GovernedPolicyOutcomeUnknownException extends RuntimeException {
    public GovernedPolicyOutcomeUnknownException(){super("Original governed policy action requires recovery");}
}
