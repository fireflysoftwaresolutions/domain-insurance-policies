package io.fireflysoftware.domain.insurance.policies.web.governed;

import io.fireflysoftware.domain.insurance.policies.core.services.governed.GovernedPolicyService;
import io.fireflysoftware.domain.insurance.policies.interfaces.governed.*;
import io.fireflysoftware.domain.insurance.policies.web.controllers.impl.GovernedPoliciesControllerImpl;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GovernedPolicyReceiverConfigurationTest {
    final ReactiveJwtDecoder decoder=mock(ReactiveJwtDecoder.class);final AtomicInteger calls=new AtomicInteger();
    final GovernedPolicyBindRequest request=new GovernedPolicyBindRequest(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,1,UUID.randomUUID(),"a".repeat(64),"a".repeat(64),UUID.randomUUID(),"SANDBOX-1",LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));
    Jwt token(String tenant){var now=Instant.now();return Jwt.withTokenValue("controlled-token").header("alg","RS256").issuer("https://issuer.invalid").subject("opaque subject é").audience(List.of("domain-policy")).issuedAt(now.minusSeconds(1)).notBefore(now.minusSeconds(1)).expiresAt(now.plusSeconds(30)).claim("tenant_id",tenant).claim("client_id","experience-policy").claim("principal_kind","HUMAN").claim("party_id",request.partyId().toString()).claim("scope","insurance:policies:bind").build();}
    WebTestClient client(){
        var service=mock(GovernedPolicyService.class);when(service.bind(any())).thenAnswer(invocation->ReactiveSecurityContextHolder.getContext().map(context->{
            calls.incrementAndGet();var authentication=assertInstanceOf(JwtAuthenticationToken.class,context.getAuthentication());var principal=assertInstanceOf(SecurityPrincipal.class,authentication.getPrincipal());
            assertEquals("controlled-token",authentication.getToken().getTokenValue());assertEquals("opaque subject é",principal.subject());assertEquals("opaque-platform-namespace",principal.tenantId());assertTrue(principal.hasScope("insurance:policies:bind"));
            return new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,request.actionId(),request.intentDigest(),null);
        }));var beans=new DefaultListableBeanFactory();beans.registerSingleton("governed",service);var config=new GovernedPolicyReceiverConfiguration();
        return WebTestClient.bindToController(new GovernedPoliciesControllerImpl(beans.getBeanProvider(GovernedPolicyService.class)))
                .webFilter(config.governedPolicyOnlyRoutes(),config.governedPolicyExceptionTranslation(),config.governedPolicyAuthentication(decoder),config.governedPolicyAuthorization()).build();
    }
    @Test void nativeReceiverRetainsVerifiedJwtAndRichOpaquePrincipal(){when(decoder.decode("controlled-token")).thenReturn(Mono.just(token("opaque-platform-namespace")));
        client().post().uri("/api/v1/policies/governed/bindings").headers(headers->headers.setBearerAuth("controlled-token")).bodyValue(request).exchange().expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("OUTCOME_UNKNOWN");assertEquals(1,calls.get());}
    @Test void missingBearerIsUnauthorizedBeforeBusiness(){client().post().uri("/api/v1/policies/governed/bindings").bodyValue(request).exchange().expectStatus().isUnauthorized();assertEquals(0,calls.get());verifyNoInteractions(decoder);}
    @Test void nativeDecoderRefusalIsUnauthorizedBeforeBusiness(){when(decoder.decode("controlled-token")).thenReturn(Mono.error(new BadJwtException("controlled refusal")));client().post().uri("/api/v1/policies/governed/bindings").headers(headers->headers.setBearerAuth("controlled-token")).bodyValue(request).exchange().expectStatus().isUnauthorized();assertEquals(0,calls.get());}
    @Test void malformedSignedTenantCannotBeNormalizedIntoAuthority(){when(decoder.decode("controlled-token")).thenReturn(Mono.just(token("bad\ntenant")));client().post().uri("/api/v1/policies/governed/bindings").headers(headers->headers.setBearerAuth("controlled-token")).bodyValue(request).exchange().expectStatus().isUnauthorized();assertEquals(0,calls.get());}
    @Test void governedModeClosesUnsupportedLegacyRouteBeforeAuthentication(){client().get().uri("/api/v1/policies/"+UUID.randomUUID()).exchange().expectStatus().isForbidden();assertEquals(0,calls.get());verifyNoInteractions(decoder);}
    @Test void defaultOffAddsNoNativeAuthenticationOrRouteGuardBeans(){new ApplicationContextRunner().withUserConfiguration(GovernedPolicyReceiverConfiguration.class).run(context->{assertNull(context.getStartupFailure());assertEquals(0,context.getBeanNamesForType(org.springframework.security.web.server.authentication.AuthenticationWebFilter.class).length);assertEquals(0,context.getBeanNamesForType(io.fireflysoftware.domain.insurance.policies.web.governed.impl.GovernedPolicyRouteGuardImpl.class).length);});}
}
