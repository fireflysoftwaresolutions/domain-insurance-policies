package io.fireflysoftware.domain.insurance.policies.interfaces.governed;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GovernedPolicyIntentContractTest {
    final String digest="a".repeat(64);final UUID action=UUID.randomUUID();
    GovernedPolicyBindRequest bind(){return new GovernedPolicyBindRequest(action,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,1,UUID.randomUUID(),digest,digest,UUID.randomUUID(),"SANDBOX-1",LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));}
    @Test void bindAndIssueAreMutuallyExclusiveTypedSelectors(){
        var issue=new GovernedPolicyIssueRequest(action,1,digest);var policy=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,bind(),policy,issue));
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.ISSUE,bind(),policy,issue));
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.ISSUE,null,new UUID(0,0),issue));
    }
    @Test void preparedDigestCannotBeMissingOrInventedAsAMalformedValue(){
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyPreparedIntent(GovernedPolicyPreparedIntent.Operation.BIND,action,"invented"));
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyPreparedIntent(GovernedPolicyPreparedIntent.Operation.ISSUE,new UUID(0,0),digest));
    }
    @Test void preparedValuesDoNotExposeSelectorsOrDigestInDiagnostics(){
        assertEquals("GovernedPolicyIntentRequest[BIND]",new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,bind(),null,null).toString());
        assertEquals("GovernedPolicyPreparedIntent[BIND]",new GovernedPolicyPreparedIntent(GovernedPolicyPreparedIntent.Operation.BIND,action,digest).toString());
    }
}
