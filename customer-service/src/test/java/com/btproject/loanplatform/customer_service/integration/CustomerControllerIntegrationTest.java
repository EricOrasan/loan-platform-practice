package com.btproject.loanplatform.customer_service.integration;

import com.btproject.loanplatform.customer_service.TestcontainersConfiguration;
import com.btproject.loanplatform.customer_service.domain.Customer;
import com.btproject.loanplatform.customer_service.domain.RiskCategory;
import com.btproject.loanplatform.customer_service.dto.CreateCustomerRequest;
import com.btproject.loanplatform.customer_service.dto.CustomerResponse;
import com.btproject.loanplatform.customer_service.error.ApiErrorResponse;
import com.btproject.loanplatform.customer_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerControllerIntegrationTest {

    private static final String CIF = "12345678";
    private static final String EMAIL = "andrei.popescu@example.com";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CustomerRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void shouldExposeContractFirstOpenApiDocumentWithoutAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/openapi/customer-service.yaml",
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("title: Customer Service API"));
        assertTrue(response.getBody().contains("/customers:"));

        ResponseEntity<String> swaggerUi = restTemplate.getForEntity(
                "/swagger-ui/index.html",
                String.class
        );
        assertEquals(HttpStatus.OK, swaggerUi.getStatusCode());
        assertNotNull(swaggerUi.getBody());
        assertTrue(swaggerUi.getBody().contains("Swagger UI"));
    }

    @Test
    void shouldCreateAndPersistCustomerAsAdmin() {
        CreateCustomerRequest request = validRequest("ANDREI.POPESCU@EXAMPLE.COM");

        ResponseEntity<CustomerResponse> response = adminClient().postForEntity(
                "/customers",
                request,
                CustomerResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().id());
        assertEquals(CIF, response.getBody().cif());
        assertEquals(EMAIL, response.getBody().email());

        Customer persisted = repository.findByCif(CIF).orElseThrow();
        assertEquals(EMAIL, persisted.getEmail());
        assertEquals(0, persisted.getMonthlyIncome().compareTo(BigDecimal.valueOf(8_500)));
        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
    }

    @Test
    void shouldReturnValidationErrorForInvalidRequest() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "12",
                "",
                "Popescu",
                "not-an-email",
                BigDecimal.ZERO,
                null
        );

        ResponseEntity<ApiErrorResponse> response = adminClient().postForEntity(
                "/customers",
                request,
                ApiErrorResponse.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertErrorResponse(response.getBody(), "VALIDATION_ERROR");
        assertFalse(repository.existsByCif(CIF));
    }

    @Test
    void shouldReturnConflictForDuplicateCif() {
        persistCustomer();

        CreateCustomerRequest duplicate = new CreateCustomerRequest(
                CIF,
                "Maria",
                "Ionescu",
                "maria.ionescu@example.com",
                BigDecimal.valueOf(7_000),
                RiskCategory.MEDIUM
        );

        ResponseEntity<ApiErrorResponse> response = adminClient().postForEntity(
                "/customers",
                duplicate,
                ApiErrorResponse.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertErrorResponse(response.getBody(), "CUSTOMER_ALREADY_EXISTS");
        assertEquals(1, repository.count());
    }

    @Test
    void shouldReturnExistingCustomerForAuthenticatedUser() {
        Customer customer = persistCustomer();

        ResponseEntity<CustomerResponse> response = userClient().getForEntity(
                "/customers/{cif}",
                CustomerResponse.class,
                CIF
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(customer.getId(), response.getBody().id());
        assertEquals(CIF, response.getBody().cif());
    }

    @Test
    void shouldReturnNotFoundForUnknownCustomer() {
        ResponseEntity<ApiErrorResponse> response = userClient().getForEntity(
                "/customers/{cif}",
                ApiErrorResponse.class,
                CIF
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertErrorResponse(response.getBody(), "CUSTOMER_NOT_FOUND");
    }

    @Test
    void shouldDeleteCustomerAsAdmin() {
        persistCustomer();

        ResponseEntity<Void> response = adminClient().exchange(
                "/customers/{cif}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class,
                CIF
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertFalse(repository.existsByCif(CIF));
    }

    @Test
    void shouldReturnUnauthorizedWithoutCredentials() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "/customers/{cif}",
                ApiErrorResponse.class,
                CIF
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertErrorResponse(response.getBody(), "UNAUTHORIZED");
        assertTrue(response.getHeaders().containsHeader(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void shouldReturnForbiddenWhenUserCreatesCustomer() {
        ResponseEntity<ApiErrorResponse> response = userClient().postForEntity(
                "/customers",
                validRequest(EMAIL),
                ApiErrorResponse.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertErrorResponse(response.getBody(), "FORBIDDEN");
        assertEquals(0, repository.count());
    }

    @Test
    void shouldReturnUnsupportedMediaTypeForNonJsonRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> request = new HttpEntity<>("not-json", headers);

        ResponseEntity<ApiErrorResponse> response = adminClient().exchange(
                "/customers",
                HttpMethod.POST,
                request,
                ApiErrorResponse.class
        );

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertErrorResponse(response.getBody(), "UNSUPPORTED_MEDIA_TYPE");
    }

    private TestRestTemplate adminClient() {
        return restTemplate.withBasicAuth("admin", "admin123");
    }

    private TestRestTemplate userClient() {
        return restTemplate.withBasicAuth("user", "user123");
    }

    private Customer persistCustomer() {
        Customer customer = new Customer();
        customer.setCif(CIF);
        customer.setFirstName("Andrei");
        customer.setLastName("Popescu");
        customer.setEmail(EMAIL);
        customer.setMonthlyIncome(BigDecimal.valueOf(8_500));
        customer.setRiskCategory(RiskCategory.LOW);
        return repository.saveAndFlush(customer);
    }

    private static CreateCustomerRequest validRequest(String email) {
        return new CreateCustomerRequest(
                CIF,
                "Andrei",
                "Popescu",
                email,
                BigDecimal.valueOf(8_500),
                RiskCategory.LOW
        );
    }

    private static void assertErrorResponse(ApiErrorResponse response, String expectedCode) {
        assertNotNull(response);
        assertEquals(expectedCode, response.code());
        assertNotNull(response.message());
        assertNotNull(response.details());
        assertNotNull(response.correlationId());
        assertNotNull(response.timestamp());
    }
}
