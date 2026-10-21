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

package io.fireflysoftware.domain.insurance.policies.core.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sandbox is the default and opens no socket.
 * {@code firefly.insurance.customer-contract=core} selects the customer and contract SDKs.
 */
@Configuration
public class CustomerContractConfiguration {

    @Bean
    @ConditionalOnProperty(name = "firefly.insurance.customer-contract", havingValue = "sandbox", matchIfMissing = true)
    public CustomerContractLink sandboxCustomerContractLink() {
        return new SandboxCustomerContractLink();
    }

    @Bean
    @ConditionalOnProperty(name = "firefly.insurance.customer-contract", havingValue = "core")
    public CustomerContractLink sdkCustomerContractLink(
            @Value("${firefly.insurance.customer-base-url:http://localhost:8080}") String customerBaseUrl,
            @Value("${firefly.insurance.contract-base-url:http://localhost:8080}") String contractBaseUrl) {
        return new SdkCustomerContractLink(customerBaseUrl, contractBaseUrl);
    }
}
