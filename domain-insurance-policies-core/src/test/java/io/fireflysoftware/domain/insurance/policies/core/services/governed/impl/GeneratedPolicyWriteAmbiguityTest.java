package io.fireflysoftware.domain.insurance.policies.core.services.governed.impl;

import io.fireflysoftware.domain.insurance.policies.interfaces.governed.GovernedPolicyCommandResponse;
import io.fireflysoftware.domain.insurance.policies.interfaces.governed.GovernedPolicyIssueRequest;
import io.fireflysoftware.domain.insurance.policies.interfaces.governed.GovernedPolicyRecoveryResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedPolicyWriteAmbiguityTest {
    private final GeneratedGovernedPolicyTransportTest fixtures=new GeneratedGovernedPolicyTransportTest();
    private static final String DIGEST=GeneratedGovernedPolicyTransportTest.DIGEST;
    @Test void received503AfterBindDispatchPreservesTheOriginalRecoveryIntent(){
        var factory=new GeneratedGovernedPolicyTransportTest.FactoryFixtureImpl();var request=fixtures.request();factory.status=HttpStatus.SERVICE_UNAVAILABLE;factory.response="{}";
        StepVerifier.create(fixtures.caller(fixtures.service(factory).bind(request),"human-A")).assertNext(value->{
            assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());
            assertEquals(request.actionId(),value.actionId());assertEquals(request.intentDigest(),value.intentDigest());assertNull(value.result());
        }).verifyComplete();
        assertEquals(1,factory.sent.size());assertEquals("/api/v1/policies/governed/bindings",factory.sent.getFirst().path());assertNull(factory.sent.getFirst().retryKey());
    }
    @Test void received503AfterIssueDispatchPreservesTheOriginalRecoveryIntent(){
        var factory=new GeneratedGovernedPolicyTransportTest.FactoryFixtureImpl();var policy=UUID.randomUUID();var request=new GovernedPolicyIssueRequest(UUID.randomUUID(),1,DIGEST);
        factory.status=HttpStatus.SERVICE_UNAVAILABLE;factory.response="{}";
        StepVerifier.create(fixtures.caller(fixtures.service(factory).issue(policy,request),"human-A")).assertNext(value->{
            assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());
            assertEquals(request.actionId(),value.actionId());assertEquals(request.intentDigest(policy),value.intentDigest());assertNull(value.result());
        }).verifyComplete();
        assertEquals(1,factory.sent.size());assertEquals("/api/v1/policies/"+policy+"/governed/issue",factory.sent.getFirst().path());assertNull(factory.sent.getFirst().retryKey());
    }
    @Test void aNewServiceRecoversOnlyTheRetainedOriginalIntentAfterAmbiguousWrite(){
        var first=new GeneratedGovernedPolicyTransportTest.FactoryFixtureImpl();var request=fixtures.request();first.status=HttpStatus.SERVICE_UNAVAILABLE;first.response="{}";
        var original=fixtures.caller(fixtures.service(first).bind(request),"human-A").block();assertNotNull(original);
        assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,original.status());assertEquals(1,first.sent.size());
        var restarted=new GeneratedGovernedPolicyTransportTest.FactoryFixtureImpl();
        restarted.response="{\"state\":\"NOT_OBSERVED_AT_THIS_READ\",\"actionId\":\""+original.actionId()+"\",\"intentDigest\":\""+original.intentDigest()+"\"}";
        StepVerifier.create(fixtures.caller(fixtures.service(restarted).recover(original.actionId(),original.intentDigest()),"human-A")).assertNext(value->{
            assertEquals(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,value.state());
            assertEquals(request.actionId(),value.actionId());assertEquals(request.intentDigest(),value.intentDigest());assertNull(value.result());
        }).verifyComplete();
        assertEquals(1,restarted.sent.size());assertEquals("/api/v1/policies/governed/recovery/"+request.actionId(),restarted.sent.getFirst().path());
        assertEquals("intentDigest="+request.intentDigest(),restarted.sent.getFirst().query());assertNull(restarted.sent.getFirst().retryKey());assertEquals(1,first.sent.size());
    }
}
