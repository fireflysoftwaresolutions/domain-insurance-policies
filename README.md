<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(max-width: 640px) and (prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-dark.svg">
  <source media="(max-width: 640px)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Domain Insurance - Policies" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Domain Insurance - Policies

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

Domain layer microservice for insurance policy records within the Firefly platform. This service orchestrates create, update, status change, close, and query against `core-insurance-policies` with compensating saga steps.

Repository: [https://github.com/fireflysoftwaresolutions/domain-insurance-policies](https://github.com/fireflysoftwaresolutions/domain-insurance-policies)

---

## Overview

`domain-insurance-policies` sits between experience services and `core-insurance-policies`. Write operations run through the FireflyFramework saga engine. Queries use the CQRS query bus. The domain depends on the core SDK only. It does not import another core's internal packages. Identifiers that point at parties, contracts, products, or policies stay UUIDs owned by those cores.

It creates, updates, changes status, and closes policies through the policies core. It does not post policy transactions, does not call a sanctions provider, and does not certify IFRS 17 or Solvency II.

### Key features

- **Create** -- register a policy through `core-insurance-policies`, with compensation that deletes the core write
- **Update** -- read the current policy and write the new field values through `updatePolicy` on `core-insurance-policies`. Status is left unchanged. Compensation writes the previous record back.
- **Status change** -- read the current policy and update its status through `core-insurance-policies`. The create and status-change sagas also append a status history row on the same core. Compensation restores the previous status and, when a history row was added, deletes that row.
- **Close** -- Cancel, lapse, and expire run the status saga and are refused when the policies core lifecycle does not allow that move. Remove deletes a quote only. A later status is kept.
- **Query** -- read one policy by identifier
- **Compensation** -- a failed later step asks the core to undo the work already accepted
- **CQRS** -- commands and queries travel on separate buses
- **Reactive I/O** -- Spring WebFlux with virtual threads enabled

## Architecture

### Module structure

| Module | Purpose |
|--------|---------|
| `domain-insurance-policies-core` | Saga workflows, commands, handlers, and the application service |
| `domain-insurance-policies-interfaces` | Request contracts for a status change and a closing reason |
| `domain-insurance-policies-infra` | SDK client factory and downstream base-path configuration |
| `domain-insurance-policies-web` | Spring Boot application, REST API, actuator, and OpenAPI |
| `domain-insurance-policies-sdk` | Java client generated from the OpenAPI document |

### Dependency flow

```
web --> interfaces --> core --> infra --> core-insurance-policies-sdk
sdk (generated from openapi.yml)
```

### Saga map

| Saga | Core calls | Compensation |
|------|------------|--------------|
| `RegisterPolicySaga` | `createPolicy` on `PoliciesApi`, then `createPolicyStatusHistory` on `PolicyStatusHistoryApi` | Deletes the status history row, then deletes the policy |
| `UpdatePolicySaga` | Reads the current record, then writes field changes through `updatePolicy` on `PoliciesApi`. Status is left unchanged. | Writes the previous record back through `updatePolicy` |
| `UpdatePolicyStatusSaga` | Reads the current record, updates its status through `updatePolicy`, then appends status history | Deletes the new history row and writes the previous status back through the core |
| Query | `getPolicyById` on `PoliciesApi` | None. A read has nothing to undo. |

Status values accepted by the core: `QUOTE`, `BOUND`, `IN_FORCE`, `LAPSED`, `CANCELLED`, `EXPIRED`.

## Setup

### Prerequisites

- Java 25
- Apache Maven 3.9+
- `core-insurance-policies-sdk` 26.10.5
- Apache Kafka on `localhost:9092` when event publishing is left at the default

### Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_ADDRESS` | `localhost` | Bind address |
| `SERVER_PORT` | `8071` | HTTP port |
| `ENDPOINT_CORE_INSURANCE_POLICIES` | `http://localhost:8080` | Base URL of `core-insurance-policies` |

### Build and test

```bash
mvn clean verify
```

### Run

```bash
ENDPOINT_CORE_INSURANCE_POLICIES=http://localhost:8080 mvn spring-boot:run -pl domain-insurance-policies-web
```

Swagger UI (non-production): [http://localhost:8071/swagger-ui.html](http://localhost:8071/swagger-ui.html)

## API

| Method | Path | Behavior |
|--------|------|----------|
| `POST` | `/api/v1/policies` | Runs `RegisterPolicySaga` |
| `GET` | `/api/v1/policies/{policyId}` | Queries the core |
| `PUT` | `/api/v1/policies/{policyId}/status` | Runs `UpdatePolicyStatusSaga` |
| `PUT` | `/api/v1/policies/{policyId}` | Runs `UpdatePolicySaga` |
| `POST` | `/api/v1/policies/{policyId}/cancel` | Cancel policy. Runs `UpdatePolicyStatusSaga` when the core lifecycle allows `CANCELLED`. |
| `POST` | `/api/v1/policies/{policyId}/lapse` | Lapse policy. Runs `UpdatePolicyStatusSaga` when the core lifecycle allows `LAPSED`. |
| `POST` | `/api/v1/policies/{policyId}/expire` | Expire policy. Runs `UpdatePolicyStatusSaga` when the core lifecycle allows `EXPIRED`. |
| `DELETE` | `/api/v1/policies/{policyId}` | Deletes the record through `core-insurance-policies` when that core allows removal |

The status request body is `status` plus an optional `statusReason`.

Cancel, lapse, and expire run the status saga and are refused when the policies core lifecycle does not allow that move. Remove deletes a quote only. A later status is kept.
A closing command body is an optional `statusReason`.

