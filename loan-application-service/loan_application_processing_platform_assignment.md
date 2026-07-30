# Loan Application Processing Platform - Practice Assignment

## 1. Assignment Goal

Build a small banking-related microservices platform that simulates the lifecycle of a loan application.

The platform must demonstrate:

- REST APIs
- Database persistence using JPA/Hibernate
- Service-to-service communication over HTTP
- Event-driven communication using Kafka
- Authentication and authorization with Spring Security
- Consistent error handling
- Basic resiliency for downstream calls
- Local execution using Docker Compose

The solution must be implemented using Java and Spring Boot.

Recommended stack:

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA / Hibernate
- PostgreSQL or H2 for local development
- Spring Security
- Kafka
- Docker Compose
- Liquibase

---

## 2. Business Domain

The system simulates a simplified loan application process in a banking context.

A bank customer can submit a loan application. The application is stored, assessed, an offer is generated if the application is eligible, and a notification is created.

High-level flow:

1. A customer is created in the Customer Service.
2. A loan application is created in the Loan Application Service.
3. The Loan Application Service publishes an application-created event.
4. The Credit Assessment Service consumes the event.
5. The Credit Assessment Service calls the Customer Service over HTTP to retrieve customer data.
6. The Credit Assessment Service stores the assessment result.
7. The Credit Assessment Service publishes an assessment-completed event.
8. The Offer Service consumes the assessment-completed event.
9. The Offer Service generates a loan offer when applicable.
10. The Offer Service publishes an offer-generated event.
11. The Notification Service consumes the offer-generated event and stores a notification.

---

## 3. Microservices Overview

The platform should contain the following services:

1. Customer Service
2. Loan Application Service
3. Credit Assessment Service
4. Offer Service
5. Notification Service

Optional additional service:

6. Audit Service

Each service should own its own data model and database schema.

---

## 4. Customer Service

### Responsibility

The Customer Service manages customer information and exposes customer data to other services.

Other services use this service to retrieve customer details by CIF.

### Database Table

```sql
CUSTOMER
- id
- cif
- first_name
- last_name
- email
- monthly_income
- risk_category
- created_at
- updated_at
```

### REST Endpoints

```http
POST /customers
GET /customers/{cif}
DELETE /customers/{cif}
```

### Example: Create Customer

```http
POST /customers
Content-Type: application/json
Authorization: Basic <credentials>
```

```json
{
  "cif": "12345678",
  "firstName": "Andrei",
  "lastName": "Popescu",
  "email": "andrei.popescu@example.com",
  "monthlyIncome": 8500,
  "riskCategory": "LOW"
}
```

### Example: Get Customer

```http
GET /customers/12345678
Authorization: Basic <credentials>
```

### Example: Delete Customer

```http
DELETE /customers/12345678
Authorization: Basic <credentials>
```

### Endpoint-Specific HTTP Status Codes

#### POST /customers

Expected successful response:

```text
201 Created
```

Possible error responses:

```text
400 Bad Request
- invalid request body
- missing mandatory fields
- invalid CIF format
- invalid email format
- monthly income is missing or invalid

401 Unauthorized
- missing or invalid authentication

403 Forbidden
- authenticated user does not have permission to create customers

409 Conflict
- customer with same CIF already exists

415 Unsupported Media Type
- Content-Type is not application/json

500 Internal Server Error
- unexpected server-side error
- unexpected database error

503 Service Unavailable
- database unavailable
- service temporarily unavailable
```

#### GET /customers/{cif}

Expected successful response:

```text
200 OK
```

Possible error responses:

```text
400 Bad Request
- invalid CIF format

401 Unauthorized
- missing or invalid authentication

403 Forbidden
- authenticated user does not have permission to view customer data

404 Not Found
- customer does not exist

500 Internal Server Error
- unexpected server-side error

503 Service Unavailable
- database unavailable
- service temporarily unavailable
```

#### DELETE /customers/{cif}

Expected successful response:

```text
204 No Content
```

Possible error responses:

```text
400 Bad Request
- invalid CIF format

401 Unauthorized
- missing or invalid authentication

403 Forbidden
- authenticated user does not have permission to delete customers

404 Not Found
- customer does not exist

409 Conflict
- customer cannot be deleted because it is referenced by active data

500 Internal Server Error
- unexpected server-side error

503 Service Unavailable
- database unavailable
- service temporarily unavailable
```

---

## 5. Loan Application Service

### Responsibility

The Loan Application Service manages loan applications.

It exposes REST endpoints for creating, reading, updating, and deleting loan applications.

When a loan application is created, the service publishes an event to a topic/queue.

### Database Table

```sql
LOAN_APPLICATION
- id
- application_number
- cif
- requested_amount
- requested_period_months
- purpose
- status
- created_at
- updated_at
```

### Suggested Status Values

```text
DRAFT
SUBMITTED
ASSESSMENT_IN_PROGRESS
APPROVED
REJECTED
OFFER_GENERATED
DELETED
```

### REST Endpoints

```http
POST /applications
GET /applications/{applicationId}
PUT /applications/{applicationId}
DELETE /applications/{applicationId}
```

### Example: Create Application

```http
POST /applications
Content-Type: application/json
Authorization: Basic <credentials>
```

```json
{
  "cif": "12345678",
  "requestedAmount": 50000,
  "requestedPeriodMonths": 60,
  "purpose": "HOME_RENOVATION"
}
```

### Example: Update Application

```http
PUT /applications/3fa85f64-5717-4562-b3fc-2c963f66afa6
Content-Type: application/json
Authorization: Basic <credentials>
```

```json
{
  "requestedAmount": 60000,
  "requestedPeriodMonths": 72,
  "purpose": "HOME_RENOVATION"
}
```

### Example: Delete Application

```http
DELETE /applications/3fa85f64-5717-4562-b3fc-2c963f66afa6
Authorization: Basic <credentials>
```

### Business Rules

- A loan application can be updated only while it is in `DRAFT` or `SUBMITTED` status.
- A loan application cannot be deleted after an offer was generated.
- Creating an application for an unknown CIF is allowed initially, but the assessment must later fail if the Customer Service returns `404 Not Found`.
- Requested amount must be greater than 0.
- Requested period must be between 6 and 120 months.

### Events Published

#### Topic/Queue

```text
loan.application.created
```

#### Event Payload

```json
{
  "eventId": "8aca1b77-f7d6-4b74-8ec6-7b2865f6c100",
  "eventType": "LOAN_APPLICATION_CREATED",
  "applicationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "cif": "12345678",
  "requestedAmount": 50000,
  "requestedPeriodMonths": 60,
  "createdAt": "2026-07-20T10:00:00Z"
}
```

### Endpoint-Specific HTTP Status Codes

#### POST /applications

Expected successful response:

```text
201 Created
```

Possible error responses:

```text
400 Bad Request
- invalid request body
- missing mandatory fields
- invalid requested amount
- invalid requested period
- invalid CIF format

401 Unauthorized
- missing or invalid authentication

403 Forbidden
- authenticated user does not have permission to create applications

409 Conflict
- duplicate application detected, if duplicate detection is implemented

415 Unsupported Media Type
- Content-Type is not application/json

500 Internal Server Error
- unexpected server-side error
- unexpected database error
- unexpected event publishing error

503 Service Unavailable
- database unavailable
- Kafka
- service temporarily unavailable
```

#### GET /applications/{applicationId}

Expected successful response:

```text
200 OK
```

Possible error responses:

```text
400 Bad Request
- invalid application ID format

401 Unauthorized
- missing or invalid authentication

403 Forbidden
- authenticated user does not have permission to view applications

404 Not Found
- application does not exist

500 Internal Server Error
- unexpected server-side error

503 Service Unavailable
- database unavailable
- service temporarily unavailable
```

#### PUT /applications/{applicationId}

Expected successful response:

```text
200 OK
```

Possible error responses:

```text
400 Bad Request
- invalid application ID format
- invalid request body
- invalid requested amount
- invalid requested period

401 Unauthorized
- missing or invalid authentication

403 Forbidden
- authenticated user does not have permission to update applications

404 Not Found
- application does not exist

409 Conflict
- application cannot be updated in current status

415 Unsupported Media Type
- Content-Type is not application/json

500 Internal Server Error
- unexpected server-side error
- unexpected database error

503 Service Unavailable
- database unavailable
- service temporarily unavailable
```

#### DELETE /applications/{applicationId}

Expected successful response:

```text
204 No Content
```

Possible error responses:

```text
400 Bad Request
- invalid application ID format

401 Unauthorized
- missing or invalid authentication

403 Forbidden
- authenticated user does not have permission to delete applications

404 Not Found
- application does not exist

409 Conflict
- application cannot be deleted in current status
- application already has generated offer

500 Internal Server Error
- unexpected server-side error
- unexpected database error

503 Service Unavailable
- database unavailable
- service temporarily unavailable
```

---

## 6. Credit Assessment Service

### Responsibility

The Credit Assessment Service consumes loan application events and performs a basic credit assessment.

This service demonstrates both event-driven communication and HTTP communication.

### Consumes

```text
loan.application.created
```

### Calls Over HTTP

```http
GET /customers/{cif}
```

### Database Table

```sql
CREDIT_ASSESSMENT
- id
- application_id
- cif
- score
- decision
- reason
- created_at
```

### Suggested Decision Values

```text
APPROVED
REJECTED
MANUAL_REVIEW
```

### Basic Assessment Logic

Example logic:

```text
If customer not found:
    decision = REJECTED
    reason = CUSTOMER_NOT_FOUND

If monthly income >= 7000 and risk category = LOW:
    score = 90
    decision = APPROVED

If monthly income >= 4000 and risk category = MEDIUM:
    score = 65
    decision = MANUAL_REVIEW

Otherwise:
    score = 40
    decision = REJECTED
```

### Produces

```text
loan.assessment.completed
```

### Event Payload

```json
{
  "eventId": "7f211a82-f5b4-4296-b3ec-8865a4b6d54b",
  "eventType": "LOAN_ASSESSMENT_COMPLETED",
  "applicationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "cif": "12345678",
  "score": 90,
  "decision": "APPROVED",
  "reason": "CUSTOMER_ELIGIBLE",
  "createdAt": "2026-07-20T10:01:00Z"
}
```

### HTTP Client Error Handling Requirements

When calling Customer Service, handle the following scenarios:

```text
200 OK
- customer data is returned and assessment continues

400 Bad Request
- request to Customer Service was malformed
- mark assessment as MANUAL_REVIEW or failed technical processing

401 Unauthorized
- service credentials are missing or invalid
- do not retry indefinitely
- mark processing as failed technical processing

403 Forbidden
- service credentials do not have permission
- do not retry indefinitely
- mark processing as failed technical processing

404 Not Found
- customer does not exist
- store rejected assessment with reason CUSTOMER_NOT_FOUND

500 Internal Server Error
- retry the HTTP call using a limited retry policy
- if retries are exhausted, mark assessment as MANUAL_REVIEW

502 Bad Gateway
- retry if appropriate
- if retries are exhausted, mark assessment as MANUAL_REVIEW

503 Service Unavailable
- Customer Service is temporarily unavailable
- retry using a limited retry policy
- if retries are exhausted, mark assessment as MANUAL_REVIEW

504 Gateway Timeout
- Customer Service did not respond in time
- retry using a limited retry policy
- if retries are exhausted, mark assessment as MANUAL_REVIEW
```

### Messaging Error Handling Requirements

The service must handle invalid or unexpected events.

Examples:

```text
Invalid event payload
- missing applicationId
- missing CIF
- invalid requested amount
- invalid requested period
```

Expected handling:

```text
- log the failure
- do not crash the consumer permanently
- send the event to a DLQ or store it as failed processing
```

---

## 7. Offer Service

### Responsibility

The Offer Service generates a loan offer based on the credit assessment result.

### Consumes

```text
loan.assessment.completed
```

### Database Table

```sql
LOAN_OFFER
- id
- application_id
- amount
- period_months
- interest_rate
- monthly_installment
- status
- created_at
```

### Suggested Status Values

```text
GENERATED
NOT_GENERATED
```

### Business Logic

```text
If decision = APPROVED:
    generate offer

If decision = MANUAL_REVIEW:
    do not generate offer automatically

If decision = REJECTED:
    do not generate offer
```

Example interest logic:

```text
score >= 85 -> interest rate 8.5%
score >= 70 -> interest rate 10.5%
score < 70  -> no automatic offer
```

### Produces

```text
loan.offer.generated
```

### Event Payload

```json
{
  "eventId": "27f454a3-cf59-48db-bf32-4a5fbdf442e3",
  "eventType": "LOAN_OFFER_GENERATED",
  "applicationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "amount": 50000,
  "periodMonths": 60,
  "interestRate": 8.5,
  "monthlyInstallment": 1025.35,
  "createdAt": "2026-07-20T10:02:00Z"
}
```

### Messaging Error Handling Requirements

The service must handle:

```text
Invalid assessment event
Duplicate assessment event
Database unavailable
Unexpected processing failure
```

Expected handling:

```text
- validate event payload
- avoid generating duplicate offers for the same application
- log failed processing
- send invalid events to DLQ or store them as failed processing
```

---

## 8. Notification Service

### Responsibility

The Notification Service simulates sending notifications to the customer.

The service does not need to send real email or SMS. It can store notifications in the database and log them.

### Consumes

```text
loan.offer.generated
```

### Database Table

```sql
NOTIFICATION
- id
- application_id
- channel
- recipient
- message
- status
- created_at
```

### Suggested Channel Values

```text
EMAIL
SMS
```

### Suggested Status Values

```text
CREATED
SENT
FAILED
```

### Example Notification Message

```text
Your loan offer was generated successfully for application 3fa85f64-5717-4562-b3fc-2c963f66afa6.
```

### Messaging Error Handling Requirements

The service must handle:

```text
Invalid offer event
Duplicate offer event
Database unavailable
Unexpected processing failure
```

Expected handling:

```text
- validate event payload
- avoid creating duplicate notifications for the same offer event
- log failed processing
- send invalid events to DLQ or store them as failed processing
```

---

## 9. Optional: Audit Service

### Responsibility

The Audit Service stores important events for traceability.

### Consumes

```text
loan.application.created
loan.assessment.completed
loan.offer.generated
```

### Database Table

```sql
AUDIT_EVENT
- id
- event_id
- event_type
- aggregate_id
- payload
- created_at
```

The purpose of this service is to demonstrate audit trails in banking systems.

---

## 10. Spring Security Requirements

Security must be implemented for all REST services.

Keep security simple for the first version.

### Option A: Basic Authentication

Define two users:

```text
user / user123 -> role USER
admin / admin123 -> role ADMIN
```

Suggested authorization rules:

```text
Customer Service:
POST /customers         -> ADMIN
GET /customers/{cif}    -> USER or ADMIN
DELETE /customers/{cif} -> ADMIN

Loan Application Service:
POST /applications          -> USER or ADMIN
GET /applications/{id}      -> USER or ADMIN
PUT /applications/{id}      -> USER or ADMIN
DELETE /applications/{id}   -> ADMIN
```

### Option B: JWT Authentication

JWT can be implemented as an optional improvement after Basic Authentication is working.

---

## 11. Common Error Response Format

All REST APIs must use a consistent error response format.

```json
{
  "code": "CUSTOMER_NOT_FOUND",
  "message": "Customer was not found.",
  "details": "No customer exists for CIF 12345678.",
  "correlationId": "2f9a0b2e-9f51-4f62-98e5-b87e5ef51c23",
  "timestamp": "2026-07-20T10:15:00Z"
}
```

### Required HTTP Status Codes to Understand and Use

```text
200 OK
- successful read/update operation

201 Created
- resource created successfully

204 No Content
- resource deleted successfully

400 Bad Request
- invalid request syntax or validation error

401 Unauthorized
- caller is not authenticated

403 Forbidden
- caller is authenticated but not authorized

404 Not Found
- requested resource does not exist

405 Method Not Allowed
- HTTP method is not supported by the endpoint

409 Conflict
- request conflicts with current resource state

415 Unsupported Media Type
- unsupported Content-Type

422 Unprocessable Entity, optional
- syntactically correct request, but business validation failed

429 Too Many Requests, optional bonus
- rate limit exceeded

500 Internal Server Error
- unexpected server-side failure

502 Bad Gateway
- invalid response from downstream service or gateway/proxy issue

503 Service Unavailable
- service temporarily unavailable, dependency unavailable, or maintenance mode

504 Gateway Timeout
- downstream service did not respond in time
```

### Suggested Application Error Codes

```text
VALIDATION_ERROR
UNAUTHORIZED
FORBIDDEN
UNSUPPORTED_MEDIA_TYPE
METHOD_NOT_ALLOWED

CUSTOMER_NOT_FOUND
CUSTOMER_ALREADY_EXISTS
CUSTOMER_DELETE_CONFLICT

APPLICATION_NOT_FOUND
APPLICATION_ALREADY_EXISTS
APPLICATION_INVALID_STATUS
APPLICATION_DELETE_CONFLICT

OFFER_NOT_FOUND
OFFER_NOT_GENERATED

DOWNSTREAM_SERVICE_UNAVAILABLE
DOWNSTREAM_SERVICE_TIMEOUT
DOWNSTREAM_BAD_RESPONSE

EVENT_PROCESSING_FAILED
INVALID_EVENT_PAYLOAD
DUPLICATE_EVENT

DATABASE_ERROR
MESSAGE_BROKER_UNAVAILABLE
INTERNAL_SERVER_ERROR
```

---

## 12. Suggested Technical Requirements

Each service should contain:

```text
Controller layer
Service layer
Repository layer
Entity classes
DTO classes
Mapper classes
Exception classes
Global exception handler
Database migration scripts
Unit tests
Integration tests where useful
```

Recommended libraries:

```text
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-validation
spring-boot-starter-security
spring-boot-starter-actuator
spring-kafka or spring-boot-starter-amqp
postgresql
liquibase-core
mapstruct
lombok
testcontainers
```

---

## 13. Architecture Requirements

At least the Loan Application Service should follow a clean architecture style, preferably hexagonal architecture / ports and adapters.

Suggested structure:

```text
application
- use cases
- services
- ports

domain
- entities
- value objects
- domain rules

infrastructure
- REST controllers
- JPA repositories
- messaging producers/consumers
- HTTP clients
```

Expected separation:

```text
Domain logic should not depend directly on controllers, JPA entities, Kafka, or HTTP clients.
External communication should be implemented through adapters.
```

---

## 14. Docker Compose

The project should provide a local Docker Compose setup with:

```text
PostgreSQL
Kafka
Kafka UI
```

Recommended setup:

```text
One database per service
```

Example databases:

```text
customer_db
loan_application_db
credit_assessment_db
offer_db
notification_db
```

---

## 15. Suggested Local Flow to Demo

1. Start Docker Compose.
2. Start all Spring Boot services.
3. Create a customer.
4. Create a loan application.
5. Check that the application-created event was published.
6. Check that the Credit Assessment Service consumed the event.
7. Check that the Credit Assessment Service called the Customer Service.
8. Check that the assessment was stored.
9. Check that the offer was generated.
10. Check that the notification was created.
11. Test error scenarios:
    - missing customer
    - invalid application amount
    - deleting an application after offer generation
    - calling secured endpoints without credentials
    - calling secured endpoints with insufficient role
    - Customer Service unavailable during assessment
    - message broker unavailable during event publishing

---

## 16. Suggested Implementation Milestones

### Milestone 1 - Project Setup

- Create Git repository.
- Create parent folder with separate service folders.
- Add Docker Compose with PostgreSQL and Kafka.
- Add basic Spring Boot applications.

### Milestone 2 - Customer Service

- Implement customer entity, repository, service, controller.
- Add create customer endpoint.
- Add get customer endpoint.
- Add delete customer endpoint.
- Add validation and error handling.
- Add Spring Security.

### Milestone 3 - Loan Application Service

- Implement application entity, repository, service, controller.
- Add create application endpoint.
- Add get application endpoint.
- Add update application endpoint.
- Add delete application endpoint.
- Add validation and status rules.
- Add Spring Security.

### Milestone 4 - Messaging

- Publish `loan.application.created` event.
- Consume event in Credit Assessment Service.
- Add invalid event handling.

### Milestone 5 - HTTP Communication

- Credit Assessment Service calls Customer Service.
- Handle 404, 500, 502, 503, 504, timeout, and unavailable service errors.
- Add retry logic with a clear maximum number of attempts.

### Milestone 6 - Assessment and Offer

- Store credit assessment.
- Publish assessment completed event.
- Generate offer asynchronously.
- Store offer.

### Milestone 7 - Notification Service

- Consume offer generated event.
- Store notification.
- Log notification message.

---

## 17. Expected Knowledge to Demonstrate

The implementation should demonstrate understanding of:

- Why each service has its own database.
- Difference between synchronous HTTP communication and asynchronous event communication.
- When to return 200, 201, 204, 400, 401, 403, 404, 409, 415, 500, 502, 503, and 504.
- How Spring Data JPA maps entities to database tables.
- How validation works with request DTOs.
- How global exception handling works.
- How Spring Security protects endpoints.
- How messages are published and consumed.
- What happens when a downstream service is unavailable.
- What retry means and why infinite retries are dangerous.
- What a DLQ is and why it is useful.
- How to avoid duplicate processing for events.

---

## 18. Optional Improvements

If the required scope is completed, the following improvements can be added:

- JWT instead of Basic Authentication.
- Correlation ID propagation across HTTP requests and events.
- Outbox pattern for reliable event publishing.
- Idempotent consumers.
- Pagination for list endpoints.
- Soft delete instead of hard delete.
- Separate profiles for local and test environments.
- Contract-first OpenAPI YAML.
- Testcontainers-based integration tests.
- GitHub Actions or Jenkins pipeline.
- Basic rate limiting returning `429 Too Many Requests`.

---

## 19. Recommended Repository Structure

```text
loan-platform-practice/
  docker-compose.yml
  README.md

  customer-service/
    src/main/java/...
    src/main/resources/...

  loan-application-service/
    src/main/java/...
    src/main/resources/...

  credit-assessment-service/
    src/main/java/...
    src/main/resources/...

  offer-service/
    src/main/java/...
    src/main/resources/...

  notification-service/
    src/main/java/...
    src/main/resources/...
```

---

## 20. Minimal Acceptance Criteria

The assignment is considered complete when:

- A customer can be created, retrieved, and deleted.
- A loan application can be created, updated, retrieved, and deleted.
- REST endpoints are secured with Spring Security.
- Unauthorized requests return `401 Unauthorized`.
- Authenticated requests without sufficient permissions return `403 Forbidden`.
- Invalid requests return `400 Bad Request` with a consistent error body.
- Missing resources return `404 Not Found`.
- Invalid state transitions return `409 Conflict`.
- Temporary dependency failures can be represented using `503 Service Unavailable`.
- A created loan application triggers an event.
- The Credit Assessment Service consumes the event.
- The Credit Assessment Service calls the Customer Service over HTTP.
- Customer Service `404 Not Found` is handled as a rejected assessment.
- Customer Service `503 Service Unavailable` is handled using limited retries.
- Assessment result is stored in a database.
- Offer generation happens asynchronously.
- Notification creation happens asynchronously.
- At least one invalid event scenario is handled.
- The README explains how to run and test the system locally.
```
