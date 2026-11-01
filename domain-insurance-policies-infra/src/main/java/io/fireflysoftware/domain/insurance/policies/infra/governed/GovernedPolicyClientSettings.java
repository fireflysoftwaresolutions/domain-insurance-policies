package io.fireflysoftware.domain.insurance.policies.infra.governed;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Explicit server target and dataset binding; opaque platform namespace is never converted to a business UUID. */
@ConfigurationProperties("insurance.policies.governed-client")
public record GovernedPolicyClientSettings(String issuer,URI jwkSetUri,String domainAudience,String inboundClientId,
        String signedTenant,UUID resourceTenantId,UUID bindingId,long bindingVersion,String bindingDigest,Instant notAfter,
        URI endpoint,String targetAudience,String delegatingClientId,String registrationId) {
    public GovernedPolicyClientSettings {
        issuer=text(issuer);https(URI.create(issuer));https(jwkSetUri);https(endpoint);
        domainAudience=text(domainAudience);inboundClientId=text(inboundClientId);signedTenant=text(signedTenant);
        targetAudience=text(targetAudience);delegatingClientId=text(delegatingClientId);registrationId=text(registrationId);
        id(resourceTenantId);id(bindingId);Objects.requireNonNull(notAfter);
        if(bindingVersion<1 || bindingDigest==null || !bindingDigest.matches("[0-9a-f]{64}"))throw invalid();
    }
    static URI https(URI value){
        if(value==null || !"https".equals(value.getScheme()) || value.getHost()==null || value.getRawUserInfo()!=null
                || value.getRawQuery()!=null || value.getRawFragment()!=null || !value.normalize().equals(value))throw invalid();return value;
    }
    static String text(String value){
        if(value==null || value.isBlank() || value.length()>512 || value.codePoints().anyMatch(Character::isISOControl))throw invalid();return value;
    }
    private static void id(UUID value){if(value==null || value.equals(new UUID(0,0)))throw invalid();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Explicit governed policy client configuration required");}
    @Override public String toString(){return "GovernedPolicyClientSettings[redacted]";}
}
