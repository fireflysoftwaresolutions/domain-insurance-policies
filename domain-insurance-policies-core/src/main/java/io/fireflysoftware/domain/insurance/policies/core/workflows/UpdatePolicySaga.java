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
import io.fireflysoftware.domain.insurance.policies.core.commands.UpdatePolicyCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.COMPENSATE_RESTORE_RECORD;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.CTX_PREVIOUS_RECORD;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.EVENT_RECORD_UPDATED;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.EVENT_RETRIEVED;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.SAGA_UPDATE_RECORD;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.STEP_RETRIEVE_FOR_UPDATE;
import static io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants.STEP_UPDATE_RECORD;

/**
 * Updates a policy on {@code core-insurance-policies} and compensates by writing the previous record back.
 * Status stays on the status saga.
 */
@Saga(name = SAGA_UPDATE_RECORD)
@Service
public class UpdatePolicySaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Autowired
    public UpdatePolicySaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = STEP_RETRIEVE_FOR_UPDATE)
    @StepEvent(type = EVENT_RETRIEVED)
    public Mono<PolicyDTO> retrievePolicyForUpdate(GetPolicyQuery query, ExecutionContext ctx) {
        return queryBus.query(query)
                .doOnNext(record -> ctx.putVariable(CTX_RECORD, record));
    }

    @SagaStep(id = STEP_UPDATE_RECORD, dependsOn = STEP_RETRIEVE_FOR_UPDATE, compensate = COMPENSATE_RESTORE_RECORD)
    @StepEvent(type = EVENT_RECORD_UPDATED)
    public Mono<PolicyDTO> updatePolicy(UpdatePolicyCommand command, ExecutionContext ctx) {
        PolicyDTO current = ctx.getVariableAs(CTX_RECORD, PolicyDTO.class);
        if (current == null || current.getPolicyId() == null) {
            return Mono.error(new IllegalStateException("Policy was not found"));
        }
        PolicyDTO incoming = command.record();
        if (incoming == null) {
            return Mono.error(new IllegalArgumentException("Policy is required"));
        }
        if (current.getPolicyStatus() != null) {
            incoming.setPolicyStatus(current.getPolicyStatus());
        }
        ctx.putVariable(CTX_PREVIOUS_RECORD, current);
        return commandBus.send(new ApplyPolicyStatusCommand(current.getPolicyId(), incoming))
                .doOnNext(updated -> ctx.putVariable(CTX_RECORD, updated));
    }

    public Mono<Void> restorePolicy(PolicyDTO updated, ExecutionContext ctx) {
        PolicyDTO previous = ctx.getVariableAs(CTX_PREVIOUS_RECORD, PolicyDTO.class);
        if (previous == null || previous.getPolicyId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new ApplyPolicyStatusCommand(previous.getPolicyId(), previous)).then();
    }
}
