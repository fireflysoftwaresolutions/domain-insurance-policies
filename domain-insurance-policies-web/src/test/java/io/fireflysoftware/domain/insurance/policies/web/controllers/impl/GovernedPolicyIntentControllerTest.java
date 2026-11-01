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

class GovernedPolicyIntentControllerTest {
    final String digest="a".repeat(64);final UUID action=UUID.randomUUID();
    GovernedPolicyIntentRequest request(){return new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,new GovernedPolicyBindRequest(action,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,1,UUID.randomUUID(),digest,digest,UUID.randomUUID(),"SANDBOX-1",LocalDate.of(2027,1,1),LocalDate.of(2028,1,1)),null,null);}
    WebTestClient client(GovernedPolicyService service){var beans=new DefaultListableBeanFactory();if(service!=null)beans.registerSingleton("governed",service);return WebTestClient.bindToController(new GovernedPoliciesControllerImpl(beans.getBeanProvider(GovernedPolicyService.class))).build();}
    @Test void absentTrustedPreparationServiceIsPreworkUnavailable(){client(null).post().uri("/api/v1/policies/governed/intents").bodyValue(request()).exchange().expectStatus().isEqualTo(503);}
    @Test void prepareReturnsOnlyTheTypedOriginalIntentWithoutStartingACommand(){
        var service=mock(GovernedPolicyService.class);var request=request();when(service.prepare(request)).thenReturn(Mono.just(new GovernedPolicyPreparedIntent(request.operation(),action,request.bindRequest().intentDigest())));
        client(service).post().uri("/api/v1/policies/governed/intents").bodyValue(request).exchange().expectStatus().isOk().expectBody().jsonPath("$.operation").isEqualTo("BIND").jsonPath("$.actionId").isEqualTo(action.toString()).jsonPath("$.intentDigest").isEqualTo(request.bindRequest().intentDigest()).jsonPath("$.result").doesNotExist().jsonPath("$.status").doesNotExist();verify(service).prepare(request);verifyNoMoreInteractions(service);
    }
    @Test void currentPreparationAuthorityRefusalIsPreservedWithoutMutation(){var service=mock(GovernedPolicyService.class);when(service.prepare(any())).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));client(service).post().uri("/api/v1/policies/governed/intents").bodyValue(request()).exchange().expectStatus().isForbidden();verify(service).prepare(any());verifyNoMoreInteractions(service);}
}
