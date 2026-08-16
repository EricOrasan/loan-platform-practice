package com.btproject.loanplatform.credit_assessment_service.integration;

import com.btproject.loanplatform.credit_assessment_service.application.exception.CustomerInformationUnavailableException;
import com.btproject.loanplatform.credit_assessment_service.domain.CustomerFinancialProfile;
import com.btproject.loanplatform.credit_assessment_service.domain.RiskCategory;
import com.btproject.loanplatform.credit_assessment_service.infrastructure.customer.CustomerServiceHttpAdapter;
import com.btproject.loanplatform.credit_assessment_service.integration.support.CustomerServiceStub;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerServiceHttpAdapterIntegrationTest {

    private static final String CIF = "12345678";
    private static final String CUSTOMER_JSON = """
            {
              "cif": "12345678",
              "monthlyIncome": 8500,
              "riskCategory": "LOW"
            }
            """;
    private static final CustomerServiceStub STUB = new CustomerServiceStub();

    private CustomerServiceHttpAdapter adapter;

    @BeforeEach
    void setUp() {
        STUB.reset();
        adapter = new CustomerServiceHttpAdapter(
                RestClient.builder(),
                STUB.baseUrl(),
                "user",
                "user123",
                3,
                Duration.ZERO
        );
    }

    @AfterAll
    static void stopStub() {
        STUB.close();
    }

    @Test
    void shouldReturnCustomerForSuccessfulResponse() {
        STUB.respond(200, CUSTOMER_JSON);

        CustomerFinancialProfile customer = adapter.findByCif(CIF).orElseThrow();

        assertEquals(CIF, customer.cif());
        assertEquals(0, customer.monthlyIncome().compareTo(BigDecimal.valueOf(8_500)));
        assertEquals(RiskCategory.LOW, customer.riskCategory());
        assertEquals(1, STUB.requestCount());
        assertEquals("Basic dXNlcjp1c2VyMTIz", STUB.authorizationHeader());
    }

    @Test
    void shouldReturnEmptyWithoutRetryForNotFound() {
        STUB.respond(404, "{}");

        Optional<CustomerFinancialProfile> customer = adapter.findByCif(CIF);

        assertTrue(customer.isEmpty());
        assertEquals(1, STUB.requestCount());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403})
    void shouldNotRetryNonRetryableClientErrors(int status) {
        STUB.respond(status, "{}");

        assertThrows(CustomerInformationUnavailableException.class, () -> adapter.findByCif(CIF));
        assertEquals(1, STUB.requestCount());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 504})
    void shouldRetryServerErrorsUpToConfiguredMaximum(int status) {
        STUB.respond(status, "{}");

        assertThrows(CustomerInformationUnavailableException.class, () -> adapter.findByCif(CIF));
        assertEquals(3, STUB.requestCount());
    }

    @Test
    void shouldSucceedWhenTemporaryFailureRecovers() {
        STUB.respondInSequence(
                new CustomerServiceStub.StubResponse(503, "{}"),
                new CustomerServiceStub.StubResponse(502, "{}"),
                new CustomerServiceStub.StubResponse(200, CUSTOMER_JSON)
        );

        CustomerFinancialProfile customer = adapter.findByCif(CIF).orElseThrow();

        assertEquals(CIF, customer.cif());
        assertEquals(3, STUB.requestCount());
    }

    @Test
    void shouldRejectInvalidSuccessfulResponse() {
        STUB.respond(200, "{\"cif\":\"invalid\",\"monthlyIncome\":8500,\"riskCategory\":\"LOW\"}");

        assertThrows(CustomerInformationUnavailableException.class, () -> adapter.findByCif(CIF));
        assertEquals(1, STUB.requestCount());
    }

    @Test
    void shouldRetryAndFailWhenCustomerServiceTimesOut() {
        STUB.respondInSequence(
                new CustomerServiceStub.StubResponse(200, CUSTOMER_JSON, Duration.ofMillis(200)),
                new CustomerServiceStub.StubResponse(200, CUSTOMER_JSON, Duration.ofMillis(200))
        );

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(100))
                        .build()
        );
        requestFactory.setReadTimeout(Duration.ofMillis(50));

        CustomerServiceHttpAdapter timeoutAdapter = new CustomerServiceHttpAdapter(
                RestClient.builder().requestFactory(requestFactory),
                STUB.baseUrl(),
                "user",
                "user123",
                2,
                Duration.ZERO
        );

        CustomerInformationUnavailableException exception = assertThrows(
                CustomerInformationUnavailableException.class,
                () -> timeoutAdapter.findByCif(CIF)
        );

        assertTrue(exception.getMessage().contains("after 2 attempts"));
    }

    @Test
    void shouldRetryAndFailWhenCustomerServiceIsUnavailable() throws Exception {
        int unavailablePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unavailablePort = socket.getLocalPort();
        }

        CustomerServiceHttpAdapter unavailableAdapter = new CustomerServiceHttpAdapter(
                RestClient.builder(),
                "http://localhost:" + unavailablePort,
                "user",
                "user123",
                2,
                Duration.ZERO
        );

        CustomerInformationUnavailableException exception = assertThrows(
                CustomerInformationUnavailableException.class,
                () -> unavailableAdapter.findByCif(CIF)
        );

        assertTrue(exception.getMessage().contains("after 2 attempts"));
    }
}
