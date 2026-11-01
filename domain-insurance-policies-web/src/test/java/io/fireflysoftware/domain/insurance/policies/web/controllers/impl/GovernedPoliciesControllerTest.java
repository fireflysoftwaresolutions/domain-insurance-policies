package io.fireflysoftware.domain.insurance.policies.web.controllers.impl;

import io.fireflysoftware.domain.insurance.policies.core.services.governed.GovernedPolicyService;
import io.fireflysoftware.domain.insurance.policies.interfaces.governed.*;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GovernedPoliciesControllerTest {
    final UUID action=UUID.randomUUID();
    final String digest="a".repeat(64);
    GovernedPolicyBindRequest request(){return new GovernedPolicyBindRequest(action,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,1,UUID.randomUUID(),digest,digest,UUID.randomUUID(),"SANDBOX-1",LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));}
    WebTestClient client(GovernedPolicyService service){
        var beans=new DefaultListableBeanFactory();
        if(service!=null)beans.registerSingleton("governedPolicyService",service);
        return WebTestClient.bindToController(new GovernedPoliciesControllerImpl(beans.getBeanProvider(GovernedPolicyService.class))).build();
    }
    @Test void missingTrustedServiceRefusesBindBeforeAnyBusinessCall(){
        client(null).post().uri("/api/v1/policies/governed/bindings").bodyValue(request()).exchange().expectStatus().isEqualTo(503);
    }
    @Test void missingTrustedServiceRefusesRecoveryBeforeAnyBusinessCall(){
        client(null).get().uri("/api/v1/policies/governed/recovery/"+action+"?intentDigest="+digest).exchange().expectStatus().isEqualTo(503);
    }
    @Test void missingTrustedServiceRefusesActivationBeforeAnyBusinessCall(){
        client(null).post().uri("/api/v1/policies/"+UUID.randomUUID()+"/governed/issue").bodyValue(new GovernedPolicyIssueRequest(action,1,digest)).exchange().expectStatus().isEqualTo(503);
    }
    @Test void uncertainBindRetainsOriginalIdentityWithoutInventingAPolicy(){
        var service=mock(GovernedPolicyService.class);var body=request();
        when(service.bind(body)).thenReturn(Mono.just(new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,action,body.intentDigest(),null)));
        client(service).post().uri("/api/v1/policies/governed/bindings").bodyValue(body).exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.status").isEqualTo("OUTCOME_UNKNOWN").jsonPath("$.actionId").isEqualTo(action.toString())
                .jsonPath("$.intentDigest").isEqualTo(body.intentDigest()).jsonPath("$.result").doesNotExist();
        verify(service).bind(body);verifyNoMoreInteractions(service);
    }
    @Test void recoveryObservesTheSameActionWithoutStartingAnotherMutation(){
        var service=mock(GovernedPolicyService.class);
        when(service.recover(action,digest)).thenReturn(Mono.just(new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,action,digest,null)));
        client(service).get().uri("/api/v1/policies/governed/recovery/"+action+"?intentDigest="+digest).exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.state").isEqualTo("NOT_OBSERVED_AT_THIS_READ").jsonPath("$.actionId").isEqualTo(action.toString())
                .jsonPath("$.intentDigest").isEqualTo(digest).jsonPath("$.result").doesNotExist();
        verify(service).recover(action,digest);verifyNoMoreInteractions(service);
    }
    @Test void activationRetainsExistingPolicyAndOriginalAction(){
        var service=mock(GovernedPolicyService.class);var policy=UUID.randomUUID();var body=new GovernedPolicyIssueRequest(action,1,digest);
        when(service.issue(policy,body)).thenReturn(Mono.just(new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,action,body.intentDigest(policy),null)));
        client(service).post().uri("/api/v1/policies/"+policy+"/governed/issue").bodyValue(body).exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.status").isEqualTo("COMMITTED_RESPONSE_UNAVAILABLE").jsonPath("$.actionId").isEqualTo(action.toString())
                .jsonPath("$.intentDigest").isEqualTo(body.intentDigest(policy)).jsonPath("$.result").doesNotExist();
        verify(service).issue(policy,body);verifyNoMoreInteractions(service);
    }
    @Test void currentAuthorityRefusalIsPreservedWithoutARetryOrSuccess(){
        var service=mock(GovernedPolicyService.class);
        when(service.bind(any())).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));
        client(service).post().uri("/api/v1/policies/governed/bindings").bodyValue(request()).exchange().expectStatus().isForbidden();
        verify(service).bind(any());verifyNoMoreInteractions(service);
    }
}
