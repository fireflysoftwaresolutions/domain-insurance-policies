package io.fireflysoftware.domain.insurance.policies.interfaces.governed;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GovernedPolicyContractTest {
    final UUID action=UUID.randomUUID(),submission=UUID.randomUUID(),party=UUID.randomUUID(),product=UUID.randomUUID(),offer=UUID.randomUUID(),acceptance=UUID.randomUUID(),contract=UUID.randomUUID();
    final String digest="a".repeat(64);
    GovernedPolicyBindRequest request(String number){return new GovernedPolicyBindRequest(action,submission,party,product,offer,1,1,acceptance,digest,digest,contract,number,LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));}
    GovernedPolicyBindingResult receipt(){return new GovernedPolicyBindingResult(UUID.randomUUID(),"SANDBOX-1",GovernedPolicyBindingResult.PolicyState.BOUND,1,digest,acceptance,UUID.randomUUID(),offer,1,digest);}
    @Test void bindDigestUsesTheActualOwnerContractBeforeAnyTransport(){
        var value=request("SANDBOX-1");var owner=new io.fireflysoftware.core.insurance.policies.interfaces.binding.GovernedPolicyBindRequest(action,submission,party,product,offer,1,1,acceptance,digest,digest,contract,"SANDBOX-1",LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));
        assertEquals(owner,value.ownerIntent());assertEquals(owner.intentDigest(),value.intentDigest());assertEquals(value.intentDigest(),value.intentDigest());assertNotEquals(value.intentDigest(),request("SANDBOX-2").intentDigest());
    }
    @Test void issueDigestUsesTheExistingPolicyAndActualOwnerVersion(){
        var policy=UUID.randomUUID();var value=new GovernedPolicyIssueRequest(action,1,digest);
        assertEquals(new io.fireflysoftware.core.insurance.policies.interfaces.binding.GovernedPolicyIssueRequest(action,1,digest).intentDigest(policy),value.intentDigest(policy));
        assertNotEquals(value.intentDigest(policy),value.intentDigest(UUID.randomUUID()));
    }
    @Test void malformedSelectorsCannotBecomeAnAcceptedIntent(){
        assertThrows(IllegalArgumentException.class,()->request("bad\nnumber"));assertThrows(IllegalArgumentException.class,()->request(new String(new char[]{'b','a','d',(char)0xd800})));
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyIssueRequest(new UUID(0,0),1,digest));
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyIssueRequest(action,0,digest));
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyIssueRequest(action,1,"not-a-digest"));
    }
    @Test void uncertainCommandKeepsIdentityAndCannotInventAReceipt(){
        for(var status:new GovernedPolicyCommandResponse.Status[]{GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,GovernedPolicyCommandResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,GovernedPolicyCommandResponse.Status.REFUSED}){
            var value=new GovernedPolicyCommandResponse(status,action,digest,null);assertEquals(action,value.actionId());assertEquals(digest,value.intentDigest());assertNull(value.result());
            assertThrows(IllegalArgumentException.class,()->new GovernedPolicyCommandResponse(status,action,digest,receipt()));
        }
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.COMMITTED,action,digest,null));
    }
    @Test void recoveryAbsenceIsAnObservationAndFoundRequiresAnExactReceipt(){
        var absent=new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,action,digest,null);assertEquals(action,absent.actionId());assertNull(absent.result());
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.FOUND,action,digest,null));
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.INTENT_CONFLICT,action,digest,receipt()));
        assertNotNull(new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.FOUND,action,digest,receipt()).result());
    }
    @Test void valueRenderingDoesNotExposeIntentOrPolicyIdentifiers(){
        assertEquals("GovernedPolicyBindRequest[redacted]",request("PRIVATE-POLICY-SENTINEL").toString());
        assertEquals("GovernedPolicyIssueRequest[redacted]",new GovernedPolicyIssueRequest(action,1,digest).toString());
        assertEquals("GovernedPolicyBindingResult[redacted]",receipt().toString());
        assertFalse(new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,action,digest,null).toString().contains(action.toString()));
    }
}
