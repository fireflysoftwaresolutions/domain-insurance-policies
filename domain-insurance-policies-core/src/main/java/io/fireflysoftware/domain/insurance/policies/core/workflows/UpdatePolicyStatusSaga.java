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
import io.fireflysoftware.domain.insurance.policies.core.commands.GetPolicyQuery;
import io.fireflysoftware.domain.insurance.policies.core.commands.UpdatePolicyStatusCommand;

import io.fireflysoftware.core.insurance.policies.sdk.model.PolicyStatusHistoryDTO;
import io.fireflysoftware.domain.insurance.policies.core.commands.RecordPolicyStatusHistoryCommand;
import io.fireflysoftware.domain.insurance.policies.core.commands.RemovePolicyStatusHistoryCommand;
import java.time.LocalDateTime;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.COMPENSATE_REMOVE_STATUS_HISTORY;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.COMPENSATE_RESTORE;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.CTX_PREVIOUS_STATUS;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.EVENT_RETRIEVED;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.EVENT_STATUS_HISTORY_RECORDED;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.EVENT_STATUS_UPDATED;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.SAGA_UPDATE_STATUS;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.STEP_RECORD_STATUS_HISTORY;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.STEP_RETRIEVE;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.STEP_UPDATE_STATUS;

/**
 * Changes a policy status by reading it and writing the new status through the core.
 */
@Saga(name = SAGA_UPDATE_STATUS)
@Service
public class UpdatePolicyStatusSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Autowired
    public UpdatePolicyStatusSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = STEP_RETRIEVE)
    @StepEvent(type = EVENT_RETRIEVED)
    public Mono<PolicyDTO> retrievePolicy(GetPolicyQuery query, ExecutionContext ctx) {
        return queryBus.query(query)
                .doOnNext(record -> ctx.putVariable(CTX_RECORD, record));
    }

    @SagaStep(id = STEP_UPDATE_STATUS, dependsOn = STEP_RETRIEVE, compensate = COMPENSATE_RESTORE)
    @StepEvent(type = EVENT_STATUS_UPDATED)
    public Mono<PolicyDTO> updatePolicyStatus(UpdatePolicyStatusCommand command, ExecutionContext ctx) {
        PolicyDTO current = ctx.getVariableAs(CTX_RECORD, PolicyDTO.class);
        if (current == null || current.getPolicyId() == null) {
            return Mono.error(new IllegalStateException("Policy was not found"));
        }
        if (current.getPolicyStatus() == null) {
            return Mono.error(new IllegalStateException("Policy has no current status"));
        }
        if (command.status() == null || command.status().isBlank()) {
            return Mono.error(new IllegalArgumentException("Status is required"));
        }
        ctx.putVariable(CTX_PREVIOUS_STATUS, current.getPolicyStatus().getValue());
        current.setPolicyStatus(PolicyDTO.PolicyStatusEnum.fromValue(command.status()));
        return commandBus.send(new ApplyPolicyStatusCommand(current.getPolicyId(), current))
                .doOnNext(updated -> ctx.putVariable(CTX_RECORD, updated));
    }

    public Mono<Void> restorePolicyStatus(PolicyDTO updated, ExecutionContext ctx) {
        if (updated == null || updated.getPolicyId() == null) {
            return Mono.empty();
        }
        String previous = ctx.getVariableAs(CTX_PREVIOUS_STATUS, String.class);
        if (previous == null || previous.isBlank()) {
            return Mono.empty();
        }
        updated.setPolicyStatus(PolicyDTO.PolicyStatusEnum.fromValue(previous));
        return commandBus.send(new ApplyPolicyStatusCommand(updated.getPolicyId(), updated)).then();
    }

    @SagaStep(id = STEP_RECORD_STATUS_HISTORY, dependsOn = STEP_UPDATE_STATUS, compensate = COMPENSATE_REMOVE_STATUS_HISTORY)
    @StepEvent(type = EVENT_STATUS_HISTORY_RECORDED)
    public Mono<PolicyStatusHistoryDTO> recordStatusHistory(UpdatePolicyStatusCommand command, ExecutionContext ctx) {
        PolicyDTO current = ctx.getVariableAs(CTX_RECORD, PolicyDTO.class);
        if (current == null || current.getPolicyId() == null || current.getPolicyStatus() == null) {
            return Mono.error(new IllegalStateException("Policy status was not updated"));
        }
        PolicyStatusHistoryDTO history = new PolicyStatusHistoryDTO();
        history.setPolicyId(current.getPolicyId());
        history.setPolicyStatus(PolicyStatusHistoryDTO.PolicyStatusEnum.fromValue(current.getPolicyStatus().getValue()));
        String reason = command.statusReason();
        history.setStatusReason(reason == null || reason.isBlank()
                ? "Status updated by the domain service"
                : reason);
        history.setChangedAt(LocalDateTime.now());
        return commandBus.send(new RecordPolicyStatusHistoryCommand(history));
    }

    public Mono<Void> removeStatusHistory(PolicyStatusHistoryDTO saved) {
        if (saved == null || saved.getPolicyStatusHistoryId() == null || saved.getPolicyId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemovePolicyStatusHistoryCommand(saved.getPolicyId(), saved.getPolicyStatusHistoryId()));
    }

}
