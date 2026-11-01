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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fireflysoftware.core.contract.sdk.api.ContractPartiesApi;
import io.fireflysoftware.core.contract.sdk.api.ContractsApi;
import io.fireflysoftware.core.customer.sdk.api.PartiesApi;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SdkCustomerContractLinkTest {
    @Test
    void nullContractCreatesAndLinksNewContractWithRealSdkRequests() {
        UUID party = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID product = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID contract = UUID.fromString("44444444-4444-4444-4444-444444444444");
        List<String> calls = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        ExchangeFunction transport = request -> {
            String path = request.url().getPath();
            calls.add(request.method() + " " + path);
            if (request.method() == HttpMethod.GET && path.equals("/api/v1/parties/" + party)) {
                return Mono.just(response(200, "{\"partyId\":\"" + party + "\"}"));
            }
            MockClientHttpRequest wire = new MockClientHttpRequest(request.method(), request.url());
            return request.writeTo(wire, ExchangeStrategies.withDefaults())
                    .then(Mono.defer(wire::getBodyAsString)).map(body -> {
                        try {
                            var json = mapper.readTree(body);
                            if (request.method() == HttpMethod.POST && path.equals("/api/v1/contracts")) {
                                assertThat(json.path("productId").asText()).isEqualTo(product.toString());
                                assertThat(json.path("currencyCode").asText()).isEqualTo("EUR");
                                assertThat(json.path("contractStatus").asText()).isEqualTo("DRAFT");
                                return response(201, "{\"contractId\":\"" + contract + "\"}");
                            }
                            assertThat(request.method()).isEqualTo(HttpMethod.POST);
                            assertThat(path).isEqualTo("/api/v1/contracts/" + contract + "/parties");
                            assertThat(json.path("contractId").asText()).isEqualTo(contract.toString());
                            assertThat(json.path("partyId").asText()).isEqualTo(party.toString());
                            assertThat(json.path("roleInContractId").asText())
                                    .isEqualTo(SdkCustomerContractLinkImpl.UNSPECIFIED_CONTRACT_ROLE.toString());
                            return response(201, "{}");
                        } catch (java.io.IOException error) {
                            throw new AssertionError(error);
                        }
                    });
        };
        WebClient web = WebClient.builder().exchangeFunction(transport).build();
        var customers = new io.fireflysoftware.core.customer.sdk.invoker.ApiClient(web);
        customers.setBasePath("http://synthetic.invalid");
        var agreements = new io.fireflysoftware.core.contract.sdk.invoker.ApiClient(web);
        agreements.setBasePath("http://synthetic.invalid");
        var link = new SdkCustomerContractLinkImpl(new PartiesApi(customers), new ContractsApi(agreements),
                new ContractPartiesApi(agreements));

        assertThat(link.bind(party, null, product, "EUR", null).block(Duration.ofSeconds(2)))
                .isEqualTo(contract);
        assertThat(calls).containsExactly("GET /api/v1/parties/" + party,
                "POST /api/v1/contracts", "POST /api/v1/contracts/" + contract + "/parties");
    }

    private static ClientResponse response(int status, String body) {
        return ClientResponse.create(org.springframework.http.HttpStatusCode.valueOf(status))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE).body(body).build();
    }
}
