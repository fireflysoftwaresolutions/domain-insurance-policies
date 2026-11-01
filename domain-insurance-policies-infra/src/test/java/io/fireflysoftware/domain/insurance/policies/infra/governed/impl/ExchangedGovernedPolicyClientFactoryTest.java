package io.fireflysoftware.domain.insurance.policies.infra.governed.impl;

import io.fireflysoftware.domain.insurance.policies.infra.governed.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.*;
import org.springframework.mock.http.client.reactive.*;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ExchangedGovernedPolicyClientFactoryTest {
    final Instant now=Instant.parse("2027-01-01T00:00:00Z");final Clock clock=Clock.fixed(now,ZoneOffset.UTC);
    final UUID party=UUID.randomUUID();final Set<String> bind=Set.of("insurance:policies:bind");
    final GovernedPolicyClientSettings settings=new GovernedPolicyClientSettings("https://issuer.invalid",URI.create("https://issuer.invalid/jwks"),"domain-policy","experience-policy","opaque-platform-namespace",UUID.randomUUID(),UUID.randomUUID(),1,"a".repeat(64),now.plusSeconds(60),URI.create("https://core-policy.invalid"),"core-policy","domain-policy-exchange","policy-exchange");
    final ReactiveJwtDecoder decoder=mock(ReactiveJwtDecoder.class);final AtomicInteger calls=new AtomicInteger();final AtomicReference<String> form=new AtomicReference<>(),reply=new AtomicReference<>();
    @BeforeEach void tokenReply(){reply.set("{\"access_token\":\"exchanged-token\",\"token_type\":\"Bearer\",\"expires_in\":30,\"scope\":\"insurance:policies:bind\",\"issued_token_type\":\"urn:ietf:params:oauth:token-type:access_token\"}");when(decoder.decode("exchanged-token")).thenReturn(Mono.just(jwt("exchanged-token","HUMAN","opaque subject","core-policy","domain-policy-exchange",party,"opaque-platform-namespace",now.plusSeconds(30),bind)));}
    Jwt jwt(String token,String kind,String subject,String audience,String client,UUID signedParty,String tenant,Instant expiry,Set<String> scopes){
        return Jwt.withTokenValue(token).header("alg","RS256").issuer(settings.issuer()).subject(subject).audience(List.of(audience)).issuedAt(now.minusSeconds(1)).notBefore(now.minusSeconds(1)).expiresAt(expiry)
                .claim("client_id",client).claim("principal_kind",kind).claim("party_id",signedParty.toString()).claim("tenant_id",tenant).claim("scope",String.join(" ",scopes)).build();
    }
    Jwt original(){return jwt("original-subject-token","HUMAN","opaque subject","domain-policy","experience-policy",party,"opaque-platform-namespace",now.plusSeconds(60),bind);}
    JwtAuthenticationToken auth(Jwt jwt){var principal=SecurityPrincipal.builder().subject(jwt.getSubject()).issuer(settings.issuer()).tenantId(jwt.getClaimAsString("tenant_id")).scopes(Set.copyOf(Arrays.asList(jwt.getClaimAsString("scope").split(" ")))).claims(jwt.getClaims()).build();return new JwtAuthenticationToken(jwt,principal,List.of());}
    <T> Mono<T> context(Mono<T> work,JwtAuthenticationToken auth){return work.contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));}
    ClientRegistration registration(AuthorizationGrantType grant){return ClientRegistration.withRegistrationId("policy-exchange").clientId("domain-policy-exchange").clientSecret("synthetic-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).authorizationGrantType(grant).tokenUri("https://issuer.invalid/oauth/token").issuerUri(settings.issuer()).scope(bind).build();}
    ClientHttpConnector connector(){return (method,uri,callback)->{
        calls.incrementAndGet();var request=new MockClientHttpRequest(method,uri);return callback.apply(request).then(Mono.defer(request::getBodyAsString)).map(body->{form.set(body);var response=new MockClientHttpResponse(HttpStatus.OK);response.getHeaders().setContentType(MediaType.APPLICATION_JSON);response.setBody(reply.get());return (ClientHttpResponse)response;});
    };}
    ExchangedGovernedPolicyClientFactoryImpl factory(){return new ExchangedGovernedPolicyClientFactoryImpl(settings,id->Mono.just(registration(AuthorizationGrantType.TOKEN_EXCHANGE)),decoder,connector(),clock,System::nanoTime);}
    @Test void actualNativeExchangeCarriesOriginalSubjectAndReturnsACallerOwnedGeneratedClient(){
        StepVerifier.create(context(factory().requireCurrent(bind),auth(original()))).assertNext(client->{assertNotNull(client.api());assertTrue(client.remaining().compareTo(Duration.ofSeconds(5))<=0);var bearer=(io.fireflysoftware.core.insurance.policies.sdk.invoker.auth.HttpBearerAuth)client.api().getApiClient().getAuthentication("bearerAuth");assertEquals("exchanged-token",bearer.getBearerToken());}).verifyComplete();
        assertEquals(1,calls.get());assertTrue(form.get().contains("subject_token=original-subject-token"));assertTrue(form.get().contains("audience=core-policy"));assertFalse(form.get().contains("actor_token="));
    }
    @Test void missingNativeVerifiedContextCannotStartAnExchange(){StepVerifier.create(factory().requireCurrent(bind)).expectErrorSatisfies(error->assertEquals(HttpStatus.UNAUTHORIZED,((ResponseStatusException)error).getStatusCode())).verify();assertEquals(0,calls.get());verifyNoInteractions(decoder);}
    @Test void aJwtWithoutTheNativeRichPrincipalCannotStartAnExchange(){StepVerifier.create(context(factory().requireCurrent(bind),new JwtAuthenticationToken(original(),List.of()))).expectError(ResponseStatusException.class).verify();assertEquals(0,calls.get());}
    @Test void staffCannotBorrowTheHumanProfile(){var staff=jwt("staff","STAFF","opaque subject","domain-policy","experience-policy",party,"opaque-platform-namespace",now.plusSeconds(60),bind);StepVerifier.create(context(factory().requireCurrent(bind),auth(staff))).expectError(ResponseStatusException.class).verify();assertEquals(0,calls.get());}
    @Test void wrongOriginalAudienceClientOrTenantRefusesBeforeExchange(){
        for(var jwt:List.of(jwt("wrong-aud","HUMAN","opaque subject","wrong","experience-policy",party,"opaque-platform-namespace",now.plusSeconds(60),bind),jwt("wrong-client","HUMAN","opaque subject","domain-policy","wrong",party,"opaque-platform-namespace",now.plusSeconds(60),bind),jwt("wrong-tenant","HUMAN","opaque subject","domain-policy","experience-policy",party,"another-namespace",now.plusSeconds(60),bind))){StepVerifier.create(context(factory().requireCurrent(bind),auth(jwt))).expectError(ResponseStatusException.class).verify();}
        assertEquals(0,calls.get());verifyNoInteractions(decoder);
    }
    @Test void anotherReturnedSubjectIsRefusedBeforeClientPublication(){when(decoder.decode("exchanged-token")).thenReturn(Mono.just(jwt("exchanged-token","HUMAN","another subject","core-policy","domain-policy-exchange",party,"opaque-platform-namespace",now.plusSeconds(30),bind)));StepVerifier.create(context(factory().requireCurrent(bind),auth(original()))).expectError(ResponseStatusException.class).verify();assertEquals(1,calls.get());}
    @Test void anotherReturnedAudienceOrPartyCannotPublishAClient(){
        for(var target:List.of(jwt("exchanged-token","HUMAN","opaque subject","another-audience","domain-policy-exchange",party,"opaque-platform-namespace",now.plusSeconds(30),bind),jwt("exchanged-token","HUMAN","opaque subject","core-policy","domain-policy-exchange",UUID.randomUUID(),"opaque-platform-namespace",now.plusSeconds(30),bind))){when(decoder.decode("exchanged-token")).thenReturn(Mono.just(target));StepVerifier.create(context(factory().requireCurrent(bind),auth(original()))).expectError(ResponseStatusException.class).verify();}assertEquals(2,calls.get());
    }
    @Test void missingIssuedTokenTypeIsRefusedByProductionExtractor(){reply.set(reply.get().replace(",\"issued_token_type\":\"urn:ietf:params:oauth:token-type:access_token\"",""));StepVerifier.create(context(factory().requireCurrent(bind),auth(original()))).expectError(ResponseStatusException.class).verify();assertEquals(1,calls.get());verifyNoInteractions(decoder);}
    @Test void emptyNativeDecoderCannotBecomeSuccessfulAcquisition(){when(decoder.decode(anyString())).thenReturn(Mono.empty());StepVerifier.create(context(factory().requireCurrent(bind),auth(original()))).expectError(ResponseStatusException.class).verify();}
    @Test void clientCredentialsCannotReplaceTheHumanSubjectGrant(){var factory=new ExchangedGovernedPolicyClientFactoryImpl(settings,id->Mono.just(registration(AuthorizationGrantType.CLIENT_CREDENTIALS)),decoder,connector(),clock,System::nanoTime);StepVerifier.create(context(factory.requireCurrent(bind),auth(original()))).expectError(ResponseStatusException.class).verify();assertEquals(0,calls.get());}
    @Test void anAcquiredHandleRejectsAChangedNativeCaller(){
        var first=auth(original());var other=auth(jwt("other","HUMAN","other subject","domain-policy","experience-policy",UUID.randomUUID(),"opaque-platform-namespace",now.plusSeconds(60),bind));
        StepVerifier.create(context(factory().requireCurrent(bind).flatMap(client->context(client.revalidate(),other)),first)).expectError(ResponseStatusException.class).verify();assertEquals(1,calls.get());
    }
    @Test void aWallClockRollbackCannotExtendTheOriginalExchangedTokenWindow(){
        var wall=new AtomicReference<>(now);var monotonic=new AtomicLong();var movingClock=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return wall.get();}};
        when(decoder.decode("exchanged-token")).thenReturn(Mono.just(jwt("exchanged-token","HUMAN","opaque subject","core-policy","domain-policy-exchange",party,"opaque-platform-namespace",now.plusMillis(500),bind)));
        var factory=new ExchangedGovernedPolicyClientFactoryImpl(settings,id->Mono.just(registration(AuthorizationGrantType.TOKEN_EXCHANGE)),decoder,connector(),movingClock,monotonic::get);
        StepVerifier.create(context(factory.requireCurrent(bind),auth(original()))).assertNext(client->{monotonic.set(Duration.ofMillis(600).toNanos());wall.set(now.minusSeconds(60));assertThrows(ResponseStatusException.class,client::remaining);}).verifyComplete();assertEquals(1,calls.get());
    }
    @Test void preparationVerifiesNativeHumanWithoutExchangeDecoderOrCoreTransport(){
        StepVerifier.create(context(factory().requireIntentCaller(bind).flatMap(caller->caller.revalidate().thenReturn(caller.partyId())),auth(original())))
                .expectNext(party).verifyComplete();assertEquals(0,calls.get());verifyNoInteractions(decoder);
    }
    @Test void preparationCallerCannotBeTransferredToAnotherNativeHuman(){
        var first=auth(original());var other=auth(jwt("other","HUMAN","other subject","domain-policy","experience-policy",UUID.randomUUID(),"opaque-platform-namespace",now.plusSeconds(60),bind));
        StepVerifier.create(context(factory().requireIntentCaller(bind).flatMap(caller->context(caller.revalidate(),other)),first)).expectError(ResponseStatusException.class).verify();assertEquals(0,calls.get());verifyNoInteractions(decoder);
    }
    @Test void staffCannotPrepareTheHumanPolicyIntent(){
        var staff=jwt("staff","STAFF","opaque subject","domain-policy","experience-policy",party,"opaque-platform-namespace",now.plusSeconds(60),bind);
        StepVerifier.create(context(factory().requireIntentCaller(bind),auth(staff))).expectError(ResponseStatusException.class).verify();assertEquals(0,calls.get());verifyNoInteractions(decoder);
    }
}
