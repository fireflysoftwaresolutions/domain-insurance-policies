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

package io.fireflysoftware.domain.insurance.policies.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.fireflysoftware.core.insurance.policies.sdk.model.PolicyDTO;
import io.fireflysoftware.domain.insurance.policies.core.commands.CreatePolicyCommand;
import io.fireflysoftware.domain.insurance.policies.core.services.PoliciesService;
import io.fireflysoftware.domain.insurance.policies.interfaces.dtos.PolicyCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyCreateContractTest {
    private final AtomicReference<CreatePolicyCommand> captured = new AtomicReference<>();
    private final PoliciesService service = (PoliciesService) Proxy.newProxyInstance(
            PoliciesService.class.getClassLoader(), new Class<?>[]{PoliciesService.class},
            (proxy, method, args) -> {
                if (!method.getName().equals("create")) throw new AssertionError(method.getName());
                captured.set((CreatePolicyCommand) args[0]);
                return Mono.just(new PolicyDTO(UUID.fromString("11111111-1111-1111-1111-111111111111")));
            });
    private final WebTestClient client = WebTestClient.bindToController(new PoliciesController(
            service, new ObjectMapper().registerModule(new JavaTimeModule()))).build();

    @Test
    void missingPartyIsRejectedBeforeCommand() {
        post("{}").expectStatus().isBadRequest();
        assertThat(captured.get()).isNull();
    }

    @Test
    void nullPartyIsRejectedBeforeCommand() {
        post("{\"partyId\":null}").expectStatus().isBadRequest();
        assertThat(captured.get()).isNull();
    }

    @Test
    void malformedPartyIsRejectedBeforeCommand() {
        post("{\"partyId\":\"not-a-uuid\"}").expectStatus().isBadRequest();
        assertThat(captured.get()).isNull();
    }

    @Test
    void flatRequestPreservesPartyAndPolicyFields() {
        post("""
                {"partyId":"22222222-2222-2222-2222-222222222222",
                 "tenantId":"33333333-3333-3333-3333-333333333333",
                 "contractId":"44444444-4444-4444-4444-444444444444",
                 "productId":"55555555-5555-5555-5555-555555555555",
                 "policyNumber":"PROPERTY-1","lineOfBusiness":"PROPERTY","policyStatus":"QUOTE",
                 "currency":"EUR","inceptionDate":"2026-10-31","expiryDate":"2027-10-31",
                 "renewalDate":"2027-10-01","createdAt":"2026-10-31T09:10:11",
                 "updatedAt":"2026-10-31T09:10:12"}
                """).expectStatus().isCreated();
        CreatePolicyCommand command = captured.get();
        assertThat(command.partyId()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        PolicyDTO record = command.record();
        assertThat(record.getTenantId()).hasToString("33333333-3333-3333-3333-333333333333");
        assertThat(record.getContractId()).hasToString("44444444-4444-4444-4444-444444444444");
        assertThat(record.getProductId()).hasToString("55555555-5555-5555-5555-555555555555");
        assertThat(record.getPolicyNumber()).isEqualTo("PROPERTY-1");
        assertThat(record.getLineOfBusiness()).isEqualTo(PolicyDTO.LineOfBusinessEnum.PROPERTY);
        assertThat(record.getPolicyStatus()).isEqualTo(PolicyDTO.PolicyStatusEnum.QUOTE);
        assertThat(record.getCurrency()).isEqualTo("EUR");
        assertThat(record.getInceptionDate()).hasToString("2026-10-31");
        assertThat(record.getExpiryDate()).hasToString("2027-10-31");
        assertThat(record.getRenewalDate()).hasToString("2027-10-01");
        assertThat(record.getCreatedAt()).hasToString("2026-10-31T09:10:11");
        assertThat(record.getUpdatedAt()).hasToString("2026-10-31T09:10:12");
    }

    @Test
    void producerRequestIsTypedAndDeclaresParty() throws Exception {
        Class<?> type = java.util.Arrays.stream(PoliciesController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("create")).findFirst().orElseThrow()
                .getParameterTypes()[0];
        assertThat(Map.class.isAssignableFrom(type)).isFalse();
        assertThat(type.getMethod("getPartyId").getReturnType()).isEqualTo(UUID.class);
    }

    @Test
    void requestRoundTripsPartyAndUsesSeparateCorePolicyProjection() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        PolicyCreateRequest request = new PolicyCreateRequest();
        UUID party = UUID.fromString("22222222-2222-2222-2222-222222222222");
        request.setPartyId(party);
        request.setPolicyNumber("PROPERTY-1");
        PolicyCreateRequest decoded = mapper.readValue(mapper.writeValueAsBytes(request), PolicyCreateRequest.class);
        assertThat(decoded.getPartyId()).isEqualTo(party);
        assertThat(decoded.getPolicyNumber()).isEqualTo("PROPERTY-1");
        new PoliciesController(service, mapper).create(decoded).block();
        decoded.setPolicyNumber("CHANGED");
        decoded.setPartyId(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        assertThat(captured.get().partyId()).isEqualTo(party);
        assertThat(captured.get().record().getPolicyNumber()).isEqualTo("PROPERTY-1");
        assertThat(mapper.readTree(mapper.writeValueAsBytes(captured.get().record())).has("partyId")).isFalse();
    }

    @Test
    void producerSchemaDeclaresThePartyAsARequiredUuid() {
        io.swagger.v3.oas.models.media.Schema<?> schema = io.swagger.v3.core.converter.ModelConverters
                .getInstance().readAll(PolicyCreateRequest.class).get("PolicyCreateRequest");
        assertThat(schema.getRequired()).contains("partyId", "tenantId", "productId",
                "policyNumber", "lineOfBusiness", "policyStatus", "inceptionDate");
        assertThat(schema.getRequired()).doesNotContain("contractId");
        assertThat(schema.getProperties().get("contractId").getNullable()).isTrue();
        io.swagger.v3.oas.models.media.Schema<?> party = schema.getProperties().get("partyId");
        assertThat(party.getType()).isEqualTo("string");
        assertThat(party.getFormat()).isEqualTo("uuid");
    }

    @Test
    void absentContractReachesTheCommandAsCreateNewContractIntent() {
        post("{\"partyId\":\"22222222-2222-2222-2222-222222222222\"}")
                .expectStatus().isCreated();
        assertThat(captured.get().record().getContractId()).isNull();
        assertThat(captured.get().partyId()).hasToString("22222222-2222-2222-2222-222222222222");
        post("{\"partyId\":\"22222222-2222-2222-2222-222222222222\",\"contractId\":null}")
                .expectStatus().isCreated();
        assertThat(captured.get().record().getContractId()).isNull();
    }

    @Test
    void callerPolicyIdentityIsExcludedFromCreateInput() {
        post("{\"partyId\":\"22222222-2222-2222-2222-222222222222\","
                + "\"policyId\":\"99999999-9999-9999-9999-999999999999\"}")
                .expectStatus().isCreated();
        assertThat(captured.get().record().getPolicyId()).isNull();
    }

    private WebTestClient.ResponseSpec post(String json) {
        return client.post().uri("/api/v1/policies").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json).exchange();
    }
}
