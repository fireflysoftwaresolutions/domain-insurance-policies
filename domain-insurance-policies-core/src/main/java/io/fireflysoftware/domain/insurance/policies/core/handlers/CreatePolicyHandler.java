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
import io.fireflysoftware.domain.insurance.policies.core.commands.CreatePolicyCommand;
import io.fireflysoftware.domain.insurance.policies.core.shared.CustomerContractLink;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Creates a policy by calling {@code core-insurance-policies}.
 */
@Slf4j
@CommandHandlerComponent
public class CreatePolicyHandler extends CommandHandler<CreatePolicyCommand, PolicyDTO> {

    private final PoliciesApi policiesApi;
    private final CustomerContractLink customerContract;

    public CreatePolicyHandler(PoliciesApi policiesApi, CustomerContractLink customerContract) {
        this.policiesApi = policiesApi;
        this.customerContract = customerContract;
    }

    @Override
    protected Mono<PolicyDTO> doHandle(CreatePolicyCommand command) {
        log.debug("Creating policy");
        PolicyDTO record = command.record();
        return customerContract.bind(command.partyId(), record.getContractId(), record.getProductId(), record.getCurrency(), null)
                .flatMap(contractId -> {
                    record.setContractId(contractId);
                    return policiesApi.createPolicy(record, UUID.randomUUID().toString());
                });
    }
}
