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

package io.fireflysoftware.domain.insurance.policies.core.handlers;

import io.fireflysoftware.core.insurance.policies.sdk.api.PoliciesApi;
import io.fireflysoftware.core.insurance.policies.sdk.model.PolicyDTO;
import io.fireflysoftware.domain.insurance.policies.core.commands.ApplyPolicyStatusCommand;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Updates a policy, including its status, through the core.
 */
@Slf4j
@CommandHandlerComponent
public class ApplyPolicyStatusHandler extends CommandHandler<ApplyPolicyStatusCommand, PolicyDTO> {

    private final PoliciesApi policiesApi;

    public ApplyPolicyStatusHandler(PoliciesApi policiesApi) {
        this.policiesApi = policiesApi;
    }

    @Override
    protected Mono<PolicyDTO> doHandle(ApplyPolicyStatusCommand command) {
        log.debug("Updating policy status: policyId={}", command.policyId());
        return policiesApi.updatePolicy(command.policyId(), command.record(), UUID.randomUUID().toString());
    }
}
