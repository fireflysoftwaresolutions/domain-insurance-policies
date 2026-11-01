package io.fireflysoftware.domain.insurance.policies.web.openapi;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fireflysoftware.domain.insurance.policies.core.services.governed.GovernedPolicyService;
import io.fireflysoftware.domain.insurance.policies.infra.governed.GovernedPolicyClientFactory;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("openapi-gen")
@SpringBootTest(classes=OpenApiGenApplication.class,webEnvironment=SpringBootTest.WebEnvironment.MOCK,properties={"spring.main.web-application-type=reactive","springdoc.api-docs.enabled=true","springdoc.api-docs.version=OPENAPI_3_0"})
class GovernedPolicyProducerContractTest {
    @Autowired ApplicationContext context;
    JsonNode document(String name)throws Exception{
        var client=WebTestClient.bindToApplicationContext(context).configureClient().responseTimeout(Duration.ofSeconds(3)).build();
        byte[] json=client.get().uri("/v3/api-docs").exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        byte[] yaml=client.get().uri("/v3/api-docs.yaml").exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        assertNotNull(json);assertNotNull(yaml);assertTrue(json.length<=4*1024*1024 && yaml.length<=4*1024*1024);
        Files.createDirectories(Path.of("target"));Files.write(Path.of("target",name+"-governed-domain-policy-openapi.json"),json);Files.write(Path.of("target",name+"-governed-domain-policy-openapi.yml"),yaml);
        var value=new ObjectMapper().readTree(json);assertEquals(value,new ObjectMapper(new YAMLFactory()).readTree(yaml));return value;
    }
    @Test void originalProducerRetainsAllElevenOperationsAndAddsOneTypedPreparation()throws Exception{
        var doc=document("typed");var observed=new HashSet<String>();
        doc.path("paths").forEach(path->path.fields().forEachRemaining(method->{if(Set.of("get","post","put","patch","delete","head","options","trace").contains(method.getKey()))assertTrue(observed.add(method.getValue().path("operationId").asText()));}));
        assertEquals(Set.of("updateStatus","get","update","remove","lapse","expire","cancel","create","bindGovernedPolicy","recoverGovernedPolicyBinding","issueGovernedPolicy","prepareGovernedPolicyIntent"),observed);
        for(var expected:List.of(new String[]{"/api/v1/policies/governed/intents","post","prepareGovernedPolicyIntent","GovernedPolicyPreparedIntent"},new String[]{"/api/v1/policies/governed/bindings","post","bindGovernedPolicy","GovernedPolicyCommandResponse"},new String[]{"/api/v1/policies/governed/recovery/{actionId}","get","recoverGovernedPolicyBinding","GovernedPolicyRecoveryResponse"},new String[]{"/api/v1/policies/{policyId}/governed/issue","post","issueGovernedPolicy","GovernedPolicyCommandResponse"})){
            var operation=doc.path("paths").path(expected[0]).path(expected[1]);assertEquals(expected[2],operation.path("operationId").asText());assertEquals("#/components/schemas/"+expected[3],operation.path("responses").path("200").path("content").path("application/json").path("schema").path("$ref").asText());assertTrue(operation.path("security").toString().contains("bearerAuth"));
        }
    }
    @Test void originalProducerKeepsAllFourOutcomesAndSelectorsWithoutBodyAuthority()throws Exception{
        var doc=document("outcome");var schema=doc.path("components").path("schemas").path("GovernedPolicyCommandResponse");var required=new HashSet<String>();schema.path("required").forEach(value->required.add(value.asText()));assertTrue(required.containsAll(Set.of("status","actionId","intentDigest")));
        var status=schema.path("properties").path("status");if(status.has("$ref"))status=doc.at(status.path("$ref").asText().substring(1));var values=new HashSet<String>();status.path("enum").forEach(value->values.add(value.asText()));assertEquals(Set.of("COMMITTED","COMMITTED_RESPONSE_UNAVAILABLE","OUTCOME_UNKNOWN","REFUSED"),values);
        var properties=doc.path("components").path("schemas").path("GovernedPolicyBindRequest").path("properties");assertEquals(14,properties.size());for(String forbidden:List.of("tenantId","actor","issuer","clientId","premium","acceptedTerms","intentDigest","ownerIntent"))assertFalse(properties.has(forbidden));
    }
    @Test void originalProducerDoesNotActivateARealPolicyTransportOrBusinessService()throws Exception{
        document("no-business");assertEquals(0,context.getBeanNamesForType(GovernedPolicyClientFactory.class,false,false).length);
        assertEquals(0,context.getBeanNamesForType(GovernedPolicyService.class,false,false).length);
    }
}
