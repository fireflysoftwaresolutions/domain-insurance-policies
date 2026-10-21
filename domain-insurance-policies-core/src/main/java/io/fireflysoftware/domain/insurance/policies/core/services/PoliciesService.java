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
import io.fireflysoftware.domain.insurance.policies.core.commands.UpdatePolicyCommand;
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

    /**
     * Updates fields on a policy and leaves its status unchanged.
     *
     * @param command the update
     * @return the record returned by the core
     */
    Mono<PolicyDTO> update(UpdatePolicyCommand command);

    /**
     * Cancels a policy when the policies core allows CANCELLED.
     *
     * @param policyId the core identifier
     * @param statusReason optional reason stored by the status saga when the core keeps one
     * @return the updated record
     */
    Mono<PolicyDTO> cancel(UUID policyId, String statusReason);

    /**
     * Lapses an in-force policy when the policies core allows LAPSED.
     *
     * @param policyId the core identifier
     * @param statusReason optional reason stored by the status saga when the core keeps one
     * @return the updated record
     */
    Mono<PolicyDTO> lapse(UUID policyId, String statusReason);

    /**
     * Expires a policy when the policies core allows EXPIRED.
     *
     * @param policyId the core identifier
     * @param statusReason optional reason stored by the status saga when the core keeps one
     * @return the updated record
     */
    Mono<PolicyDTO> expire(UUID policyId, String statusReason);

    /**
     * Deletes a policy when the core lifecycle allows removal.
     *
     * @param policyId the core identifier
     * @return completion when the core has deleted the record
     */
    Mono<Void> remove(UUID policyId);
}
