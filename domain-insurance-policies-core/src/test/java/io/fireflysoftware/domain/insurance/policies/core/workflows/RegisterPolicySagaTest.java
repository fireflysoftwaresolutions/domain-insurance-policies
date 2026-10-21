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
import io.fireflysoftware.domain.insurance.policies.core.commands.CreatePolicyCommand;
import io.fireflysoftware.domain.insurance.policies.core.commands.RemovePolicyCommand;
import io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants;

import io.fireflysoftware.core.insurance.policies.sdk.model.PolicyStatusHistoryDTO;
import io.fireflysoftware.domain.insurance.policies.core.commands.RecordPolicyStatusHistoryCommand;

import org.fireflyframework.cqrs.command.CommandBus;
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
 * Verifies the register saga stores the core result and can compensate by deleting it.
 */
@ExtendWith(MockitoExtension.class)
class RegisterPolicySagaTest {

    @Mock
    private CommandBus commandBus;

    private RegisterPolicySaga saga;

    @BeforeEach
    void setUp() {
        saga = new RegisterPolicySaga(commandBus);
    }

    @Test
    void register_storesTheRecordReturnedByTheCore() {
        UUID id = UUID.randomUUID();
        PolicyDTO created = new PolicyDTO(id);
        created.setPolicyStatus(PolicyDTO.PolicyStatusEnum.QUOTE);
        when(commandBus.send(any(CreatePolicyCommand.class))).thenReturn(Mono.just(created));

        ExecutionContext ctx = ExecutionContext.forSaga(PolicySagaConstants.SAGA_REGISTER, "test");
        PolicyDTO request = new PolicyDTO();
        request.setPolicyStatus(PolicyDTO.PolicyStatusEnum.QUOTE);

        StepVerifier.create(saga.registerPolicy(new CreatePolicyCommand(request), ctx))
                .expectNext(created)
                .verifyComplete();

        assertThat(ctx.getVariableAs(PolicySagaConstants.CTX_RECORD, PolicyDTO.class).getPolicyId()).isEqualTo(id);
    }

    @Test
    void remove_sendsDeleteForTheCreatedRecord() {
        UUID id = UUID.randomUUID();
        when(commandBus.send(any(RemovePolicyCommand.class))).thenReturn(Mono.empty());

        StepVerifier.create(saga.removePolicy(new PolicyDTO(id)))
                .verifyComplete();

        ArgumentCaptor<RemovePolicyCommand> captor = ArgumentCaptor.forClass(RemovePolicyCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().policyId()).isEqualTo(id);
    }

    @Test
    void recordInitialStatus_copiesIdAndStatusFromTheCreatedRecord() {
        UUID id = UUID.randomUUID();
        PolicyDTO created = new PolicyDTO(id);
        created.setPolicyStatus(PolicyDTO.PolicyStatusEnum.QUOTE);
        PolicyStatusHistoryDTO saved = new PolicyStatusHistoryDTO(UUID.randomUUID());
        when(commandBus.send(any(RecordPolicyStatusHistoryCommand.class))).thenReturn(Mono.just(saved));

        ExecutionContext ctx = ExecutionContext.forSaga(PolicySagaConstants.SAGA_REGISTER, "test");
        ctx.putVariable(PolicySagaConstants.CTX_RECORD, created);

        StepVerifier.create(saga.recordInitialStatus(new CreatePolicyCommand(created), ctx))
                .expectNext(saved)
                .verifyComplete();

        ArgumentCaptor<RecordPolicyStatusHistoryCommand> captor = ArgumentCaptor.forClass(RecordPolicyStatusHistoryCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().history().getPolicyId()).isEqualTo(id);
        assertThat(captor.getValue().history().getPolicyStatus().getValue()).isEqualTo("QUOTE");
    }

}
