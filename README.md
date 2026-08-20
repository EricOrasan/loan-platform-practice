# Loan Application Processing Platform

## 1. Application overview

The Loan Application Processing Platform simulates a simplified bank loan workflow.

A customer is registered together with basic financial information. The customer can then submit a loan application. The application is assessed automatically, an offer is generated when the customer is eligible, and a notification is created. Important events are also stored for audit and traceability.

The business flow is:

```text
Customer creation
    → Loan application creation
    → Credit assessment
    → Offer generation
    → Customer notification
    → Audit trail
```

The platform is split into six microservices. Each service has one main responsibility and owns its database.

## 2. Technology stack

- Java 21 and Spring Boot;
- Spring Web for REST APIs;
- Spring Data JPA and Hibernate for persistence;
- PostgreSQL for service databases;
- Liquibase for database migrations;
- Apache Kafka for event-driven communication;
- Spring Security with Basic Authentication;
- Bean Validation for request validation;
- Docker Compose for PostgreSQL, Kafka, and Kafka UI;
- OpenAPI and Swagger UI for API documentation;
- JUnit, Mockito, and Testcontainers for testing;
- GitHub Actions for continuous integration.

## 3. Microservices

### Customer Service

**Responsibility:** manages customer identity, contact details, monthly income, and risk category. Other services retrieve customer information by CIF.

**REST endpoints:**

| Method | Endpoint | Authorization | Success |
|---|---|---|---:|
| POST | `/customers` | ADMIN | 201 Created |
| GET | `/customers/{cif}` | USER or ADMIN | 200 OK |
| DELETE | `/customers/{cif}` | ADMIN | 204 No Content |

**Database:** `customer_db`, table `customer`.

**Important errors:** invalid request — 400; missing authentication — 401; insufficient role — 403; customer not found — 404; duplicate CIF or email — 409; invalid Content-Type — 415.

### Loan Application Service

**Responsibility:** manages loan applications and starts the asynchronous loan-processing flow.

**REST endpoints:**

| Method | Endpoint | Authorization | Success |
|---|---|---|---:|
| POST | `/applications` | USER or ADMIN | 201 Created |
| GET | `/applications/{id}` | USER or ADMIN | 200 OK |
| PUT | `/applications/{id}` | USER or ADMIN | 200 OK |
| DELETE | `/applications/{id}` | ADMIN | 204 No Content |

**Business rules:** requested amount must be greater than zero; requested period must be between 6 and 120 months; an application can be updated only in `DRAFT` or `SUBMITTED`; deletion is rejected for an application whose local status is `OFFER_GENERATED` or `DELETED`.

**Database:** `loan_application_db`, table `loan_application`.

**Kafka output:** `loan.application.created`.

DELETE is implemented as a soft delete. The database row is retained and its status becomes `DELETED`.

### Credit Assessment Service

**Responsibility:** performs the credit assessment by combining loan application data with customer financial information.

**Kafka input:** `loan.application.created`.

**HTTP communication:** calls `GET /customers/{cif}` using service credentials. Monthly income and risk category are obtained through this call because they are not included in the application event.

**Assessment logic:**

| Customer information | Score | Decision |
|---|---:|---|
| Income >= 7000 and risk LOW | 90 | APPROVED |
| Income >= 4000 and risk MEDIUM | 65 | MANUAL_REVIEW |
| Other valid customer | 40 | REJECTED |
| Customer not found | 0 | REJECTED |
| Customer Service unavailable after retries | 0 | MANUAL_REVIEW |

**Database:** `credit_assessment_db`, table `credit_assessment`.

**Kafka output:** `loan.assessment.completed`.

### Offer Service

**Responsibility:** generates and stores a loan offer for an approved assessment.

**Kafka input:** `loan.assessment.completed`.

An offer is generated only when the decision is `APPROVED`. `MANUAL_REVIEW` and `REJECTED` do not create an automatic offer.

Interest rules:

| Score | Interest rate |
|---:|---:|
| >= 85 | 8.5% |
| >= 70 | 10.5% |
| < 70 | No automatic offer |

**Database:** `offer_db`, table `loan_offer`.

**Kafka output:** `loan.offer.generated`.

### Notification Service

**Responsibility:** simulates notifying the customer that an offer was generated.

**Kafka input:** `loan.offer.generated`.

**HTTP communication:** retrieves the customer email from Customer Service.

The notification is sent through a logging adapter, marked as `SENT`, and stored. No real email or SMS provider is used.

**Database:** `notification_db`, table `notification`.

### Audit Service

**Responsibility:** stores important business events for traceability.

**Kafka inputs:**

- `loan.application.created`;
- `loan.assessment.completed`;
- `loan.offer.generated`.

**Database:** `audit_db`, table `audit_event`.

Each audit record contains the event ID, event type, application ID, serialized payload, and timestamp. Audit Service does not make business decisions and does not add synchronous coupling to the main flow.

## 4. Architecture

### Customer Service — layered architecture

Customer Service uses a conventional layered structure:

```text
controller
    ↓
service
    ↓
repository
    ↓
PostgreSQL
```

- the controller handles HTTP requests and responses;
- the service coordinates application logic;
- the repository provides database access;
- DTOs define the REST contract;
- the mapper converts between DTOs and the JPA entity.

This structure is suitable for a small CRUD-oriented service.

### Other services — hexagonal architecture

Loan Application, Credit Assessment, Offer, Notification, and Audit follow a ports-and-adapters structure:

```text
domain
├── domain models
├── enums and value objects
└── business rules

application
├── command
├── port/in       — use cases exposed by the service
├── port/out      — external capabilities required by the service
└── service       — use-case implementations

infrastructure
├── web           — REST controllers, where required
├── messaging     — Kafka consumers and producers
├── persistence   — JPA entities, repositories, and mappers
├── customer      — HTTP adapters, where required
└── config
```

A **port** is an interface defined by the application. An **adapter** connects that interface to an external technology.

Examples:

```text
LoanApplicationRepository port
    ← LoanApplicationPersistenceAdapter
    ← Spring Data JPA and PostgreSQL

LoanApplicationEventPublisher port
    ← KafkaLoanApplicationEventPublisher
    ← KafkaTemplate and Kafka
```

The central business logic does not depend directly on REST controllers, JPA entities, Kafka, or HTTP clients. Dependencies point toward the application and domain layers.

## 5. Data and messaging interfaces

### Database per service

Each microservice owns its data and database schema. Services do not read another service's tables directly.

| Service | Database | Main table |
|---|---|---|
| Customer | `customer_db` | `customer` |
| Loan Application | `loan_application_db` | `loan_application` |
| Credit Assessment | `credit_assessment_db` | `credit_assessment` |
| Offer | `offer_db` | `loan_offer` |
| Notification | `notification_db` | `notification` |
| Audit | `audit_db` | `audit_event` |

Database ownership reduces coupling and allows each service to evolve its schema independently. The local environment uses one PostgreSQL container with six separate logical databases.

### Kafka topics

| Topic | Publisher | Consumers |
|---|---|---|
| `loan.application.created` | Loan Application | Credit Assessment, Audit |
| `loan.assessment.completed` | Credit Assessment | Offer, Audit |
| `loan.offer.generated` | Offer | Notification, Audit |

Consumers validate event payloads and avoid duplicate business processing using application IDs or event IDs.

Invalid events and records that cannot be processed after bounded retries are sent to dead-letter topics:

```text
loan.application.created.dlq
loan.assessment.completed.dlq
loan.offer.generated.dlq
audit.events.dlq
```

A DLQ prevents one invalid message from permanently blocking the consumer and preserves it for investigation or replay.

## 6. Security and error handling

The REST APIs use stateless Basic Authentication.

```text
user / user123   → USER
admin / admin123 → ADMIN
```

All REST errors use the same response structure:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": "Requested Amount must be greater than 0",
  "correlationId": "...",
  "timestamp": "..."
}
```

Important status codes include:

- 200 — successful read or update;
- 201 — resource created;
- 204 — successful operation without a response body;
- 400 — invalid request or validation error;
- 401 — caller is not authenticated;
- 403 — caller is authenticated but not authorized;
- 404 — resource does not exist;
- 409 — duplicate data or invalid resource state;
- 415 — unsupported Content-Type;
- 500 — unexpected internal failure;
- 503 — service or dependency unavailable.

## 7. Starting the application

### Prerequisites

- Java 21;
- Docker Desktop;
- IntelliJ IDEA;
- Postman;
- ports 5432, 9092, 8090, and 8081–8086 available.

### Environment configuration

Copy `.env.example` to `.env` and set a local PostgreSQL password:

```powershell
Copy-Item .env.example .env
```

The same `POSTGRES_PASSWORD` value must be configured in each IntelliJ service run configuration.

### Infrastructure

```powershell
docker compose up -d
docker compose ps
```

Docker Compose starts PostgreSQL, Kafka, Kafka UI, and a Kafka topic-initialization job. Kafka UI is available at [http://localhost:8090](http://localhost:8090).

### Spring Boot services

Start the six services from IntelliJ:

| Service | Port |
|---|---:|
| Customer | 8081 |
| Loan Application | 8082 |
| Credit Assessment | 8083 |
| Offer | 8084 |
| Notification | 8085 |
| Audit | 8086 |

## 8. Demo — happy path

Postman contains the prepared happy-path and error requests.

### Create a customer

```http
POST http://localhost:8081/customers
Authorization: Basic admin/admin123
```

```json
{
  "cif": "19082026",
  "firstName": "Andrei",
  "lastName": "Demo",
  "email": "andrei.demo.19082026@example.com",
  "monthlyIncome": 8500,
  "riskCategory": "LOW"
}
```

Expected result: `201 Created`.

### Create a loan application

```http
POST http://localhost:8082/applications
Authorization: Basic user/user123
```

```json
{
  "cif": "19082026",
  "requestedAmount": 50000,
  "requestedPeriodMonths": 60,
  "purpose": "HOME_RENOVATION"
}
```

Expected result: `201 Created`. The returned `applicationId` is used to follow the complete flow.

### Inspect Kafka

Open Kafka UI and inspect the following topics in order:

1. `loan.application.created`;
2. `loan.assessment.completed`;
3. `loan.offer.generated`.

All records should use the same application ID. The expected assessment is:

```text
score = 90
decision = APPROVED
reason = CUSTOMER_ELIGIBLE
```

Consumer Groups can be used to show that Credit Assessment, Offer, Notification, and Audit consumed their records and reached zero lag.

### Inspect PostgreSQL

Using the same application ID, inspect:

| Database | Expected result |
|---|---|
| `customer_db` | demo customer |
| `loan_application_db` | application for 50000 over 60 months |
| `credit_assessment_db` | score 90 and APPROVED |
| `offer_db` | generated offer with 8.5% interest |
| `notification_db` | EMAIL notification with SENT status |
| `audit_db` | the three lifecycle events |

The Notification Service console also logs the simulated email notification.

## 9. Demo — error scenarios

### Invalid application amount

Submit an application with `requestedAmount: -500`.

Expected result: `400 Bad Request` with `VALIDATION_ERROR`. No application or Kafka event is created.

### Missing authentication

Call a protected customer or application endpoint without Basic Authentication.

Expected result: `401 Unauthorized` with `UNAUTHORIZED`.

### Insufficient role

Call `POST /customers` using `user / user123`.

Expected result: `403 Forbidden` with `FORBIDDEN`.

### Missing customer (404)

Create a valid loan application using a valid but unregistered CIF.

Expected flow:

```text
Loan Application             → 201 Created
Customer Service call        → 404 Not Found
Credit Assessment            → REJECTED / CUSTOMER_NOT_FOUND
Assessment event             → published
Offer and notification       → not created
```

### Deleting an application after offer generation

Call `DELETE /applications/{id}` using ADMIN credentials after the application has reached `OFFER_GENERATED`.

Expected result: `409 Conflict` with `APPLICATION_INVALID_STATUS`. The domain rule prevents deletion of an application that already has a generated offer.

The current event flow does not synchronize the generated-offer status back into Loan Application Service. This scenario should therefore be demonstrated live only when the local application status is already `OFFER_GENERATED`; otherwise, the domain rule can be shown through its automated test.

### Customer Service unavailable during assessment

Stop Customer Service and create a new application for a customer that is known to exist.

Credit Assessment performs at most three attempts with a 500 ms backoff. After the retries are exhausted, it stores and publishes:

```text
score = 0
decision = MANUAL_REVIEW
reason = TECHNICAL_PROCESSING_FAILED
```

No offer or notification is generated. This is different from a 404: an unavailable service creates technical uncertainty, while 404 confirms that the customer does not exist.

### Kafka unavailable during event publishing

Stop Kafka and submit a valid loan application.

Expected result: `503 Service Unavailable` with `EVENT_PUBLISHING_UNAVAILABLE`. The application transaction is rolled back, so an application is not left in the database without its event.

Kafka can then be restarted and a new application submitted to demonstrate recovery.

## 10. Minimum acceptance coverage

- REST APIs and database persistence;
- service-to-service communication over HTTP;
- event-driven communication through Kafka;
- authentication and authorization;
- consistent validation and REST errors;
- bounded retry for downstream failures;
- invalid-event handling and DLQs;
- duplicate-processing protection;
- Docker Compose and Liquibase;
- unit and integration tests.

## 11. Additional improvements

### Audit Service

An additional microservice stores the complete business-event trail for traceability.

### OpenAPI and Swagger

Customer and Loan Application provide versioned OpenAPI contracts. OpenAPI describes endpoints, authentication, payloads, responses, and errors. Swagger UI renders the contracts, and the same YAML files can be imported into Postman.

```text
http://localhost:8081/swagger-ui.html
http://localhost:8082/swagger-ui.html
```

### GitHub Actions

The CI workflow runs on pushes and pull requests to `main`, as well as through manual dispatch. It creates one test job for each microservice and runs all six test suites using Java 21.

The workflow status is available in the repository's **Actions** tab. All six service jobs should be green.

### Testcontainers

Integration tests start temporary PostgreSQL and Kafka containers. This validates real database migrations, JPA mappings, event serialization, Kafka consumers and producers, retries, and DLQ behavior instead of testing only against mocks.

### Soft delete

Loan Application retains deleted financial records by changing their status to `DELETED` instead of physically removing the database row.
