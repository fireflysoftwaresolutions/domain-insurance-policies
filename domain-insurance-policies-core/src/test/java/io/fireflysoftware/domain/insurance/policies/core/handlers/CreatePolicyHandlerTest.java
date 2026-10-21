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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The policy row is written only after the party is confirmed and the contract is linked.
 */
@ExtendWith(MockitoExtension.class)
class CreatePolicyHandlerTest {

    @Mock
    private PoliciesApi policiesApi;

    @Test
    void bindsThePartyAndContractBeforeWritingThePolicy() {
        UUID partyId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PolicyDTO record = new PolicyDTO();
        record.setProductId(productId);
        record.setCurrency("EUR");
        PolicyDTO created = new PolicyDTO(UUID.randomUUID());
        when(policiesApi.createPolicy(eq(record), any())).thenReturn(Mono.just(created));
        CustomerContractLink link = (party, contract, product, currency, role) -> {
            assertThat(party).isEqualTo(partyId);
            assertThat(product).isEqualTo(productId);
            assertThat(currency).isEqualTo("EUR");
            return Mono.just(contractId);
        };
        CreatePolicyHandler handler = new CreatePolicyHandler(policiesApi, link);

        StepVerifier.create(handler.doHandle(new CreatePolicyCommand(record, partyId)))
                .expectNext(created)
                .verifyComplete();

        assertThat(record.getContractId()).isEqualTo(contractId);
        verify(policiesApi).createPolicy(eq(record), any());
    }

    @Test
    void missingPartyWritesNothing() {
        CustomerContractLink link = (party, contract, product, currency, role) ->
                Mono.error(new IllegalArgumentException("Party is required"));
        CreatePolicyHandler handler = new CreatePolicyHandler(policiesApi, link);

        StepVerifier.create(handler.doHandle(new CreatePolicyCommand(new PolicyDTO(), null)))
                .expectErrorMessage("Party is required")
                .verify();

        verify(policiesApi, never()).createPolicy(any(), any());
    }
}
