/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.domain.insurance.policies.core.workflows;

import io.fireflysoftware.core.insurance.policies.sdk.model.PolicyDTO;
import io.fireflysoftware.domain.insurance.policies.core.commands.ApplyPolicyStatusCommand;
import io.fireflysoftware.domain.insurance.policies.core.commands.UpdatePolicyStatusCommand;
import io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies a status change is written through the core and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdatePolicyStatusSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdatePolicyStatusSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdatePolicyStatusSaga(commandBus, queryBus);
    }

    @Test
    void updateStatus_sendsTheRequestedStatusToTheCore() {
        UUID id = UUID.randomUUID();
        PolicyDTO current = new PolicyDTO(id);
        current.setPolicyStatus(PolicyDTO.PolicyStatusEnum.QUOTE);
        when(commandBus.send(any(ApplyPolicyStatusCommand.class))).thenReturn(Mono.just(current));

        ExecutionContext ctx = ExecutionContext.forSaga(PolicySagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(PolicySagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updatePolicyStatus(new UpdatePolicyStatusCommand(id, "BOUND", "Caller request"), ctx))
                .expectNext(current)
                .verifyComplete();

        ArgumentCaptor<ApplyPolicyStatusCommand> captor = ArgumentCaptor.forClass(ApplyPolicyStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().policyId()).isEqualTo(id);
        assertThat(captor.getValue().record().getPolicyStatus()).isEqualTo(PolicyDTO.PolicyStatusEnum.BOUND);
        assertThat(ctx.getVariableAs(PolicySagaConstants.CTX_PREVIOUS_STATUS, String.class)).isEqualTo("QUOTE");
    }

    @Test
    void restore_writesThePreviousStatusBack() {
        UUID id = UUID.randomUUID();
        PolicyDTO updated = new PolicyDTO(id);
        updated.setPolicyStatus(PolicyDTO.PolicyStatusEnum.BOUND);
        when(commandBus.send(any(ApplyPolicyStatusCommand.class))).thenReturn(Mono.just(updated));

        ExecutionContext ctx = ExecutionContext.forSaga(PolicySagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(PolicySagaConstants.CTX_PREVIOUS_STATUS, "QUOTE");

        StepVerifier.create(saga.restorePolicyStatus(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyPolicyStatusCommand> captor = ArgumentCaptor.forClass(ApplyPolicyStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record().getPolicyStatus()).isEqualTo(PolicyDTO.PolicyStatusEnum.QUOTE);
    }
}
