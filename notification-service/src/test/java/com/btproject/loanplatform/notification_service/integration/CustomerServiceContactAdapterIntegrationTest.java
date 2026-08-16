package com.btproject.loanplatform.notification_service.integration;

import com.btproject.loanplatform.notification_service.application.exception.CustomerContactUnavailableException;
import com.btproject.loanplatform.notification_service.application.model.CustomerContact;
import com.btproject.loanplatform.notification_service.infrastructure.customer.CustomerServiceContactAdapter;
import com.btproject.loanplatform.notification_service.integration.support.CustomerServiceStub;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerServiceContactAdapterIntegrationTest {

    private static final String CIF = "12345678";
    private static final String CONTACT_JSON = """
            {
              "cif": "12345678",
              "email": "andrei.popescu@example.com"
            }
            """;
    private static final CustomerServiceStub STUB = new CustomerServiceStub();

    private CustomerServiceContactAdapter adapter;

    @BeforeEach
    void setUp() {
        STUB.reset();
        adapter = new CustomerServiceContactAdapter(
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
    void shouldReturnContactForSuccessfulResponse() {
        STUB.respond(200, CONTACT_JSON);

        CustomerContact contact = adapter.findByCif(CIF).orElseThrow();

        assertEquals("andrei.popescu@example.com", contact.email());
        assertEquals(1, STUB.requestCount());
        assertEquals("Basic dXNlcjp1c2VyMTIz", STUB.authorizationHeader());
    }

    @Test
    void shouldReturnEmptyWithoutRetryForNotFound() {
        STUB.respond(404, "{}");

        Optional<CustomerContact> contact = adapter.findByCif(CIF);

        assertTrue(contact.isEmpty());
        assertEquals(1, STUB.requestCount());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403})
    void shouldNotRetryClientErrors(int status) {
        STUB.respond(status, "{}");

        assertThrows(CustomerContactUnavailableException.class, () -> adapter.findByCif(CIF));
        assertEquals(1, STUB.requestCount());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 504})
    void shouldRetryServerErrorsUpToConfiguredMaximum(int status) {
        STUB.respond(status, "{}");

        assertThrows(CustomerContactUnavailableException.class, () -> adapter.findByCif(CIF));
        assertEquals(3, STUB.requestCount());
    }

    @Test
    void shouldSucceedWhenTemporaryFailureRecovers() {
        STUB.respondInSequence(
                new CustomerServiceStub.StubResponse(503, "{}"),
                new CustomerServiceStub.StubResponse(502, "{}"),
                new CustomerServiceStub.StubResponse(200, CONTACT_JSON)
        );

        CustomerContact contact = adapter.findByCif(CIF).orElseThrow();

        assertEquals("andrei.popescu@example.com", contact.email());
        assertEquals(3, STUB.requestCount());
    }

    @Test
    void shouldRejectInvalidSuccessfulResponse() {
        STUB.respond(200, "{\"cif\":\"12345678\",\"email\":\"\"}");

        assertThrows(CustomerContactUnavailableException.class, () -> adapter.findByCif(CIF));
        assertEquals(1, STUB.requestCount());
    }

    @Test
    void shouldRetryAndFailWhenCustomerServiceTimesOut() {
        STUB.respondInSequence(
                new CustomerServiceStub.StubResponse(200, CONTACT_JSON, Duration.ofMillis(200)),
                new CustomerServiceStub.StubResponse(200, CONTACT_JSON, Duration.ofMillis(200))
        );

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(100)).build()
        );
        requestFactory.setReadTimeout(Duration.ofMillis(50));
        CustomerServiceContactAdapter timeoutAdapter = new CustomerServiceContactAdapter(
                RestClient.builder().requestFactory(requestFactory),
                STUB.baseUrl(),
                "user",
                "user123",
                2,
                Duration.ZERO
        );

        CustomerContactUnavailableException exception = assertThrows(
                CustomerContactUnavailableException.class,
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

        CustomerServiceContactAdapter unavailableAdapter = new CustomerServiceContactAdapter(
                RestClient.builder(),
                "http://localhost:" + unavailablePort,
                "user",
                "user123",
                2,
                Duration.ZERO
        );

        CustomerContactUnavailableException exception = assertThrows(
                CustomerContactUnavailableException.class,
                () -> unavailableAdapter.findByCif(CIF)
        );

        assertTrue(exception.getMessage().contains("after 2 attempts"));
    }
}
