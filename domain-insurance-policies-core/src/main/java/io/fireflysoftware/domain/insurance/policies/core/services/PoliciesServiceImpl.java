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

package io.fireflysoftware.domain.insurance.policies.core.services;

import io.fireflysoftware.core.insurance.policies.sdk.model.PolicyDTO;
import io.fireflysoftware.domain.insurance.policies.core.commands.CreatePolicyCommand;
import io.fireflysoftware.domain.insurance.policies.core.commands.GetPolicyQuery;
import io.fireflysoftware.domain.insurance.policies.core.commands.UpdatePolicyStatusCommand;
import io.fireflysoftware.domain.insurance.policies.core.constants.PolicySagaConstants;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Runs the policy sagas and reads through the query bus.
 */
@Service
public class PoliciesServiceImpl implements PoliciesService {

    private final SagaEngine sagaEngine;
    private final QueryBus queryBus;

    public PoliciesServiceImpl(SagaEngine sagaEngine, QueryBus queryBus) {
        this.sagaEngine = sagaEngine;
        this.queryBus = queryBus;
    }

    @Override
    public Mono<PolicyDTO> create(CreatePolicyCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(PolicySagaConstants.STEP_REGISTER, command)
                .forStepId(PolicySagaConstants.STEP_RECORD_INITIAL_STATUS, command)
        .build();
        return sagaEngine.execute(PolicySagaConstants.SAGA_REGISTER, inputs)
                .flatMap(result -> outcome(result, PolicySagaConstants.STEP_REGISTER, PolicyDTO.class));
    }

    @Override
    public Mono<PolicyDTO> updateStatus(UpdatePolicyStatusCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(PolicySagaConstants.STEP_RETRIEVE, new GetPolicyQuery(command.policyId()))
                .forStepId(PolicySagaConstants.STEP_UPDATE_STATUS, command)
                .forStepId(PolicySagaConstants.STEP_RECORD_STATUS_HISTORY, command)
        .build();
        return sagaEngine.execute(PolicySagaConstants.SAGA_UPDATE_STATUS, inputs)
                .flatMap(result -> outcome(result, PolicySagaConstants.STEP_UPDATE_STATUS, PolicyDTO.class));
    }

    @Override
    public Mono<PolicyDTO> get(UUID policyId) {
        return queryBus.query(new GetPolicyQuery(policyId));
    }

    private static <T> Mono<T> outcome(SagaResult result, String stepId, Class<T> type) {
        if (result.isFailed()) {
            Throwable error = result.error().orElseGet(() ->
                    new IllegalStateException("Saga " + result.sagaName() + " failed"));
            return Mono.error(error);
        }
        return Mono.justOrEmpty(result.resultOf(stepId, type))
                .switchIfEmpty(Mono.error(new IllegalStateException("Saga step " + stepId + " produced no result")));
    }
}
