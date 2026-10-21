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
import io.fireflysoftware.domain.insurance.policies.core.commands.UpdatePolicyStatusCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Application service for policy orchestration.
 */
public interface PoliciesService {

    /**
     * Creates a policy through the register saga.
     *
     * @param command the create command
     * @return the record returned by the core
     */
    Mono<PolicyDTO> create(CreatePolicyCommand command);

    /**
     * Moves a policy to a new status through the core.
     *
     * @param command the status change
     * @return the updated record
     */
    Mono<PolicyDTO> updateStatus(UpdatePolicyStatusCommand command);

    /**
     * Reads one policy.
     *
     * @param policyId the core identifier
     * @return the record, or empty when the core has none
     */
    Mono<PolicyDTO> get(UUID policyId);
}
