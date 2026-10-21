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

import io.fireflysoftware.core.insurance.policies.sdk.model.PolicyStatusHistoryDTO;
import io.fireflysoftware.domain.insurance.policies.core.commands.RecordPolicyStatusHistoryCommand;
import io.fireflysoftware.domain.insurance.policies.core.commands.RemovePolicyStatusHistoryCommand;
import java.time.LocalDateTime;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.COMPENSATE_REMOVE;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.COMPENSATE_REMOVE_INITIAL_STATUS;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.EVENT_INITIAL_STATUS_RECORDED;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.EVENT_REGISTERED;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.SAGA_REGISTER;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.STEP_RECORD_INITIAL_STATUS;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.STEP_REGISTER;

/**
 * Creates a policy on {@code core-insurance-policies} and compensates by deleting it.
 */
@Saga(name = SAGA_REGISTER)
@Service
public class RegisterPolicySaga {

    private final CommandBus commandBus;

    @Autowired
    public RegisterPolicySaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_REGISTER, compensate = COMPENSATE_REMOVE)
    @StepEvent(type = EVENT_REGISTERED)
    public Mono<PolicyDTO> registerPolicy(CreatePolicyCommand command, ExecutionContext ctx) {
        return commandBus.send(command)
                .doOnNext(created -> ctx.putVariable(CTX_RECORD, created));
    }

    public Mono<Void> removePolicy(PolicyDTO created) {
        if (created == null || created.getPolicyId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemovePolicyCommand(created.getPolicyId()));
    }

    @SagaStep(id = STEP_RECORD_INITIAL_STATUS, dependsOn = STEP_REGISTER, compensate = COMPENSATE_REMOVE_INITIAL_STATUS)
    @StepEvent(type = EVENT_INITIAL_STATUS_RECORDED)
    public Mono<PolicyStatusHistoryDTO> recordInitialStatus(CreatePolicyCommand command, ExecutionContext ctx) {
        PolicyDTO created = ctx.getVariableAs(CTX_RECORD, PolicyDTO.class);
        if (created == null || created.getPolicyId() == null) {
            return Mono.error(new IllegalStateException("Policy was not created"));
        }
        if (created.getPolicyStatus() == null) {
            return Mono.error(new IllegalStateException("Policy status is required before history can be recorded"));
        }
        PolicyStatusHistoryDTO history = new PolicyStatusHistoryDTO();
        history.setPolicyId(created.getPolicyId());
        history.setPolicyStatus(PolicyStatusHistoryDTO.PolicyStatusEnum.fromValue(created.getPolicyStatus().getValue()));
        history.setStatusReason("Initial status recorded by the domain service");
        history.setChangedAt(LocalDateTime.now());
        return commandBus.send(new RecordPolicyStatusHistoryCommand(history));
    }

    public Mono<Void> removeInitialStatus(PolicyStatusHistoryDTO saved) {
        if (saved == null || saved.getPolicyStatusHistoryId() == null || saved.getPolicyId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemovePolicyStatusHistoryCommand(saved.getPolicyId(), saved.getPolicyStatusHistoryId()));
    }

}
