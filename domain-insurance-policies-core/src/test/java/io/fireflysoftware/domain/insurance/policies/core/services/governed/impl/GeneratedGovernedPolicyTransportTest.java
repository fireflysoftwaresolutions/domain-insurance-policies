package io.fireflysoftware.domain.insurance.policies.core.services.governed.impl;

import io.fireflysoftware.core.insurance.policies.sdk.api.GovernedPoliciesApi;
import io.fireflysoftware.core.insurance.policies.sdk.invoker.ApiClient;
import io.fireflysoftware.domain.insurance.policies.infra.governed.GovernedPolicyClientFactory;
import io.fireflysoftware.domain.insurance.policies.interfaces.governed.*;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.http.client.reactive.*;
import org.springframework.mock.http.client.reactive.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedGovernedPolicyTransportTest {
    static final String CALLER="native-fixture-caller",DIGEST="a".repeat(64);
    GovernedPolicyBindRequest request(){return new GovernedPolicyBindRequest(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,1,UUID.randomUUID(),DIGEST,DIGEST,UUID.randomUUID(),"SANDBOX-1",LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));}
    record Sent(String path,String query,List<String> authorization,String body,String retryKey){}
    static final class FactoryFixtureImpl implements GovernedPolicyClientFactory {
        final List<Sent> sent=new CopyOnWriteArrayList<>();
        final List<Set<String>> requestedScopes=new CopyOnWriteArrayList<>();
        final List<GovernedPoliciesApi> apis=new CopyOnWriteArrayList<>();
        HttpStatus status=HttpStatus.OK;String response;boolean connectionLost,acquisitionUnavailable;
        @Override public Mono<Client> requireCurrent(Set<String> scopes){return Mono.deferContextual(context->{
            if(acquisitionUnavailable)return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
            String caller=context.get(CALLER);requestedScopes.add(Set.copyOf(scopes));
            ClientHttpConnector connector=(method,uri,callback)->{
                var request=new MockClientHttpRequest(method,uri);
                return callback.apply(request).then(Mono.defer(request::getBodyAsString)).flatMap(body->{
                    sent.add(new Sent(uri.getPath(),uri.getRawQuery(),List.copyOf(request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION)),body,request.getHeaders().getFirst("X-Idempotency-Key")));
                    if(connectionLost)return Mono.error(new IOException("controlled loss after possible dispatch"));
                    var reply=new MockClientHttpResponse(status);reply.getHeaders().setContentType(MediaType.APPLICATION_JSON);reply.setBody(response);return Mono.just((ClientHttpResponse)reply);
                });
            };
            var apiClient=new ApiClient(ApiClient.buildWebClientBuilder().clientConnector(connector).build()).setBasePath("https://synthetic-policy.invalid");apiClient.setBearerToken(caller);
            var api=new GovernedPoliciesApi(apiClient);apis.add(api);
            return Mono.just(new Client(){
                @Override public GovernedPoliciesApi api(){return api;}
                @Override public Duration remaining(){return Duration.ofSeconds(2);}
                @Override public Mono<Void> revalidate(){return Mono.deferContextual(current->caller.equals(current.getOrDefault(CALLER,""))?Mono.empty():Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));}
            });
        });}
    }
    <T> Mono<T> caller(Mono<T> work,String value){return work.contextWrite(context->context.put(CALLER,value));}
    GovernedPolicyServiceImpl service(FactoryFixtureImpl factory){return new GovernedPolicyServiceImpl(new GeneratedGovernedPolicyTransportImpl(factory));}
    String command(UUID action,String digest,String status){return "{\"status\":\""+status+"\",\"actionId\":\""+action+"\",\"intentDigest\":\""+digest+"\"}";}
    @Test void genuineGeneratedCoreApiAndModelsComeFromTheSelectedSourceReactor()throws Exception{
        var expected=java.nio.file.Path.of("..","..","core-insurance-policies","core-insurance-policies-sdk","target","classes").toRealPath();
        for(Class<?> type:List.of(GovernedPoliciesApi.class,io.fireflysoftware.core.insurance.policies.sdk.model.GovernedPolicyCommandResponse.class,io.fireflysoftware.core.insurance.policies.sdk.model.GovernedPolicyRecoveryResponse.class))
            FreshSourceSdkFixture.require(type,expected);
    }
    @Test void actualGeneratedBindCarriesTheCompleteImmutableIntentAndBearer(){
        var factory=new FactoryFixtureImpl();var request=request();factory.response=command(request.actionId(),request.intentDigest(),"OUTCOME_UNKNOWN");
        StepVerifier.create(caller(service(factory).bind(request),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(request.actionId(),value.actionId());assertEquals(request.intentDigest(),value.intentDigest());}).verifyComplete();
        assertEquals(1,factory.sent.size());var sent=factory.sent.getFirst();assertEquals("/api/v1/policies/governed/bindings",sent.path());assertEquals(List.of("Bearer human-A"),sent.authorization());assertNull(sent.retryKey());assertEquals(List.of(Set.of("insurance:policies:bind")),factory.requestedScopes);
        assertDoesNotThrow(()->{
            var json=ApiClient.createDefaultMapper(null).readTree(sent.body());assertEquals(14,json.size());
            assertEquals(request.actionId().toString(),json.path("actionId").asText());assertEquals(request.submissionId().toString(),json.path("submissionId").asText());
            assertEquals(request.partyId().toString(),json.path("partyId").asText());assertEquals(request.productId().toString(),json.path("productId").asText());assertEquals(request.offerId().toString(),json.path("offerId").asText());
            assertEquals(1,json.path("offerVersion").asLong());assertEquals(1,json.path("acceptedControlGeneration").asLong());assertEquals(request.acceptanceActionId().toString(),json.path("acceptanceActionId").asText());
            assertEquals(DIGEST,json.path("acceptanceIntentDigest").asText());assertEquals(DIGEST,json.path("termsDigest").asText());assertEquals(request.contractId().toString(),json.path("contractId").asText());
            assertEquals("SANDBOX-1",json.path("policyNumber").asText());assertEquals("2027-01-01",json.path("inceptionDate").asText());assertEquals("2028-01-01",json.path("expiryDate").asText());
        });
    }
    @Test void actualGeneratedActivationKeepsTheExistingPolicyAndReceipt(){
        var factory=new FactoryFixtureImpl();var policy=UUID.randomUUID();var request=new GovernedPolicyIssueRequest(UUID.randomUUID(),1,DIGEST);factory.response=command(request.actionId(),request.intentDigest(policy),"COMMITTED_RESPONSE_UNAVAILABLE");
        StepVerifier.create(caller(service(factory).issue(policy,request),"human-A")).assertNext(value->assertEquals(GovernedPolicyCommandResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,value.status())).verifyComplete();
        assertEquals(1,factory.sent.size());assertEquals("/api/v1/policies/"+policy+"/governed/issue",factory.sent.getFirst().path());assertEquals(List.of(Set.of("insurance:policies:issue")),factory.requestedScopes);
        assertDoesNotThrow(()->{var json=ApiClient.createDefaultMapper(null).readTree(factory.sent.getFirst().body());assertEquals(3,json.size());assertEquals(request.actionId().toString(),json.path("actionId").asText());assertEquals(1,json.path("expectedBindingVersion").asLong());assertEquals(DIGEST,json.path("bindingReceiptDigest").asText());});
    }
    @Test void actualGeneratedCommittedReceiptMapsAllTypedFields(){
        var factory=new FactoryFixtureImpl();var request=request();var policy=UUID.randomUUID();var acceptance=UUID.randomUUID();
        factory.response="{\"status\":\"COMMITTED\",\"actionId\":\""+request.actionId()+"\",\"intentDigest\":\""+request.intentDigest()+"\",\"result\":{\"policyId\":\""+policy+"\",\"policyNumber\":\"SANDBOX-1\",\"policyStatus\":\"BOUND\",\"bindingVersion\":1,\"bindingReceiptDigest\":\""+DIGEST+"\",\"acceptanceActionId\":\""+request.acceptanceActionId()+"\",\"acceptanceId\":\""+acceptance+"\",\"offerId\":\""+request.offerId()+"\",\"offerVersion\":1,\"termsDigest\":\""+DIGEST+"\"}}";
        StepVerifier.create(caller(service(factory).bind(request),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.COMMITTED,value.status());
            assertEquals(new GovernedPolicyBindingResult(policy,"SANDBOX-1",GovernedPolicyBindingResult.PolicyState.BOUND,1,DIGEST,request.acceptanceActionId(),acceptance,request.offerId(),1,DIGEST),value.result());}).verifyComplete();assertEquals(1,factory.sent.size());
    }
    @Test void actualGeneratedRecoveryUsesTheObservedThirdNullParameterWithoutARetryKey(){
        var factory=new FactoryFixtureImpl();var action=UUID.randomUUID();factory.response="{\"state\":\"NOT_OBSERVED_AT_THIS_READ\",\"actionId\":\""+action+"\",\"intentDigest\":\""+DIGEST+"\"}";
        StepVerifier.create(caller(service(factory).recover(action,DIGEST),"human-A")).assertNext(value->{assertEquals(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,value.state());assertEquals(action,value.actionId());}).verifyComplete();
        assertEquals(1,factory.sent.size());assertEquals("/api/v1/policies/governed/recovery/"+action,factory.sent.getFirst().path());assertEquals("intentDigest="+DIGEST,factory.sent.getFirst().query());assertNull(factory.sent.getFirst().retryKey());assertEquals(List.of(Set.of("insurance:policies:recover","insurance:policies:read")),factory.requestedScopes);
    }
    @Test void generatedTransportLossKeepsUnknownAndNeverRepeatsTheMutation(){
        var factory=new FactoryFixtureImpl();var request=request();factory.connectionLost=true;
        StepVerifier.create(caller(service(factory).bind(request),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(request.actionId(),value.actionId());assertEquals(request.intentDigest(),value.intentDigest());assertNull(value.result());}).verifyComplete();assertEquals(1,factory.sent.size());
    }
    @Test void generatedForbiddenAndPreworkUnavailableRemainFailures(){
        var refused=new FactoryFixtureImpl();refused.status=HttpStatus.FORBIDDEN;refused.response="{}";
        StepVerifier.create(caller(service(refused).bind(request()),"human-A")).expectErrorSatisfies(error->assertEquals(HttpStatus.FORBIDDEN,((ResponseStatusException)error).getStatusCode())).verify();assertEquals(1,refused.sent.size());
        var unavailable=new FactoryFixtureImpl();unavailable.acquisitionUnavailable=true;
        StepVerifier.create(caller(service(unavailable).bind(request()),"human-A")).expectErrorSatisfies(error->assertEquals(HttpStatus.SERVICE_UNAVAILABLE,((ResponseStatusException)error).getStatusCode())).verify();assertTrue(unavailable.sent.isEmpty());
    }
    @Test void malformedSuccessAfterPossibleDispatchRequiresOriginalRecovery(){
        var factory=new FactoryFixtureImpl();var request=request();factory.response="{\"status\":\"COMMITTED\"}";
        StepVerifier.create(caller(service(factory).bind(request),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(request.actionId(),value.actionId());assertNull(value.result());}).verifyComplete();assertEquals(1,factory.sent.size());
    }
    @Test void aHandleCannotDispatchAnotherIntent(){
        var factory=new FactoryFixtureImpl();var original=request();var replacement=request();var transport=new GeneratedGovernedPolicyTransportImpl(factory);
        StepVerifier.create(caller(transport.requireCurrent(new GovernedPolicyTransport.Intent(GovernedPolicyTransport.Action.BIND,original.actionId(),original.intentDigest(),null)).flatMap(client->client.bind(replacement)),"human-A"))
                .expectErrorSatisfies(error->assertEquals(HttpStatus.CONFLICT,((ResponseStatusException)error).getStatusCode())).verify();assertTrue(factory.sent.isEmpty());
    }
    @Test void overlappingCallersRetainDistinctGenuineGeneratedClientCredentials(){
        var factory=new FactoryFixtureImpl();var action=UUID.randomUUID();factory.response="{\"state\":\"NOT_OBSERVED_AT_THIS_READ\",\"actionId\":\""+action+"\",\"intentDigest\":\""+DIGEST+"\"}";var service=service(factory);
        StepVerifier.create(Mono.zip(caller(service.recover(action,DIGEST),"human-A"),caller(service.recover(action,DIGEST),"human-B"))).expectNextCount(1).verifyComplete();
        assertEquals(2,factory.apis.size());assertNotSame(factory.apis.getFirst().getApiClient(),factory.apis.getLast().getApiClient());assertEquals(Set.of(List.of("Bearer human-A"),List.of("Bearer human-B")),Set.of(factory.sent.getFirst().authorization(),factory.sent.getLast().authorization()));
    }
}
