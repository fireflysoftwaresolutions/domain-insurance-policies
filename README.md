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

Domain layer microservice for insurance policy records within the Firefly platform. This service orchestrates create, status change, and query against `core-insurance-policies` with compensating saga steps.

Repository: [https://github.com/fireflysoftwaresolutions/domain-insurance-policies](https://github.com/fireflysoftwaresolutions/domain-insurance-policies)

---

## Overview

`domain-insurance-policies` sits between experience services and `core-insurance-policies`. Write operations run through the FireflyFramework saga engine. Queries use the CQRS query bus. The domain depends on the core SDK only. It does not import another core's internal packages. Identifiers that point at parties, contracts, products, or policies stay UUIDs owned by those cores.

It records policy status through the policies core. It does not post policy transactions, does not call a sanctions provider, and does not certify IFRS 17 or Solvency II.

### Key features

- **Create** -- register a policy through `core-insurance-policies`
- **Status change** -- read the current record and update its status through the core The create and status-change sagas also append a status history row on the same core.
- **Query** -- read one policy by identifier
- **Compensation** -- a failed later step asks the core to undo the work already accepted
- **CQRS** -- commands and queries travel on separate buses
- **Reactive I/O** -- Spring WebFlux with virtual threads enabled

## Architecture

### Module structure

| Module | Purpose |
|--------|---------|
| `domain-insurance-policies-core` | Saga workflows, commands, handlers, and the application service |
| `domain-insurance-policies-interfaces` | Request contract for a status change |
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
| `UpdatePolicyStatusSaga` | Reads the current record, updates its status through `updatePolicy`, then appends status history | Deletes the new history row and writes the previous status back through the core |
| Query | `getPolicyById` on `PoliciesApi` | None. A read has nothing to undo. |

Status values accepted by the core: `QUOTE`, `BOUND`, `IN_FORCE`, `LAPSED`, `CANCELLED`, `EXPIRED`.

## Setup

### Prerequisites

- Java 25
- Apache Maven 3.9+
- `core-insurance-policies-sdk` 26.10.3
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

The status request body is `status` plus an optional `statusReason`.
