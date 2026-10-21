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

package io.fireflysoftware.domain.insurance.policies.infra;

import io.fireflysoftware.core.insurance.policies.sdk.api.PoliciesApi;
import io.fireflysoftware.core.insurance.policies.sdk.invoker.ApiClient;
import io.fireflysoftware.core.insurance.policies.sdk.api.PolicyStatusHistoryApi;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Exposes the {@code core-insurance-policies} SDK clients used by the saga handlers.
 */
@Component
public class PolicyClientFactory {

    private final ApiClient apiClient;

    public PolicyClientFactory(PolicyCoreProperties properties) {
        this.apiClient = new ApiClient();
        this.apiClient.setBasePath(properties.getBasePath());
    }

    /**
     * Creates the primary core client.
     *
     * @return the API client
     */
    @Bean
    public PoliciesApi policiesApi() {
        return new PoliciesApi(apiClient);
    }

    /**
     * Creates the status history client.
     *
     * @return the status history API
     */
    @Bean
    public PolicyStatusHistoryApi policyStatusHistoryApi() {
        return new PolicyStatusHistoryApi(apiClient);
    }

}
