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
import io.fireflysoftware.domain.insurance.policies.core.commands.GetPolicyQuery;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Loads one policy from the core.
 */
@Slf4j
@QueryHandlerComponent
public class GetPolicyHandler extends QueryHandler<GetPolicyQuery, PolicyDTO> {

    private final PoliciesApi policiesApi;

    public GetPolicyHandler(PoliciesApi policiesApi) {
        this.policiesApi = policiesApi;
    }

    @Override
    protected Mono<PolicyDTO> doHandle(GetPolicyQuery query) {
        log.debug("Fetching policy: policyId={}", query.policyId());
        return policiesApi.getPolicyById(query.policyId(), UUID.randomUUID().toString());
    }
}
