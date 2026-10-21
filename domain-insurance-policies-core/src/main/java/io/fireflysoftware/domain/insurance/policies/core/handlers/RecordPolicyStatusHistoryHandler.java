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

import io.fireflysoftware.core.insurance.policies.sdk.api.PolicyStatusHistoryApi;
import io.fireflysoftware.core.insurance.policies.sdk.model.PolicyStatusHistoryDTO;
import io.fireflysoftware.domain.insurance.policies.core.commands.RecordPolicyStatusHistoryCommand;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Appends a status history row on {@code core-insurance-policies}.
 */
@Slf4j
@CommandHandlerComponent
public class RecordPolicyStatusHistoryHandler extends CommandHandler<RecordPolicyStatusHistoryCommand, PolicyStatusHistoryDTO> {

    private final PolicyStatusHistoryApi policyStatusHistoryApi;

    public RecordPolicyStatusHistoryHandler(PolicyStatusHistoryApi policyStatusHistoryApi) {
        this.policyStatusHistoryApi = policyStatusHistoryApi;
    }

    @Override
    protected Mono<PolicyStatusHistoryDTO> doHandle(RecordPolicyStatusHistoryCommand command) {
        log.debug("Recording policy status history");
        return policyStatusHistoryApi.createPolicyStatusHistory(
                command.history().getPolicyId(),
                command.history(),
                UUID.randomUUID().toString());
    }
}
