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

package io.fireflysoftware.domain.insurance.policies.interfaces.dtos;

import io.fireflysoftware.core.insurance.policies.sdk.model.PolicyDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Flat policy creation input with the party needed for customer-contract linking.
 */
public class PolicyCreateRequest {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID partyId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID tenantId;

    @Schema(nullable = true)
    private UUID contractId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID productId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String policyNumber;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PolicyDTO.LineOfBusinessEnum lineOfBusiness;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PolicyDTO.PolicyStatusEnum policyStatus;

    private String currency;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate inceptionDate;

    private LocalDate expiryDate;

    private LocalDate renewalDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public UUID getPartyId() {
        return partyId;
    }

    public void setPartyId(UUID partyId) {
        this.partyId = partyId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getContractId() {
        return contractId;
    }

    public void setContractId(UUID contractId) {
        this.contractId = contractId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public PolicyDTO.LineOfBusinessEnum getLineOfBusiness() {
        return lineOfBusiness;
    }

    public void setLineOfBusiness(PolicyDTO.LineOfBusinessEnum lineOfBusiness) {
        this.lineOfBusiness = lineOfBusiness;
    }

    public PolicyDTO.PolicyStatusEnum getPolicyStatus() {
        return policyStatus;
    }

    public void setPolicyStatus(PolicyDTO.PolicyStatusEnum policyStatus) {
        this.policyStatus = policyStatus;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getInceptionDate() {
        return inceptionDate;
    }

    public void setInceptionDate(LocalDate inceptionDate) {
        this.inceptionDate = inceptionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDate getRenewalDate() {
        return renewalDate;
    }

    public void setRenewalDate(LocalDate renewalDate) {
        this.renewalDate = renewalDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
