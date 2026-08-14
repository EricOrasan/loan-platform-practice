package com.btproject.loanplatform.notification_service.infrastructure.customer;

import com.btproject.loanplatform.notification_service.application.exception.CustomerContactUnavailableException;
import com.btproject.loanplatform.notification_service.application.model.CustomerContact;
import com.btproject.loanplatform.notification_service.application.port.out.CustomerContactPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

@Component
public class CustomerServiceContactAdapter implements CustomerContactPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerServiceContactAdapter.class);

    private final RestClient restClient;
    private final int maxAttempts;
    private final Duration retryBackoff;

    public CustomerServiceContactAdapter(
            RestClient.Builder builder,
            @Value("${app.clients.customer-service.base-url}")
            String baseUrl,
            @Value("${app.clients.customer-service.username}")
            String username,
            @Value("${app.clients.customer-service.password}")
            String password,
            @Value("${app.clients.customer-service.retry.max-attempts}")
            int maxAttempts,
            @Value("${app.clients.customer-service.retry.backoff}")
            Duration retryBackoff
    ) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }

        if (retryBackoff.isNegative()) {
            throw new IllegalArgumentException("retryBackoff must not be negative");
        }

        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                .build();
        this.maxAttempts = maxAttempts;
        this.retryBackoff = retryBackoff;
    }

    @Override
    public Optional<CustomerContact> findByCif(String cif) {
        int attempt = 1;

        while (true) {
            try {
                CustomerServiceContactResponse response = restClient
                        .get()
                        .uri("/customers/{cif}", cif)
                        .retrieve()
                        .body(CustomerServiceContactResponse.class);

                return Optional.of(toCustomerContact(response));

            } catch (HttpClientErrorException.NotFound exception) {
                return Optional.empty();

            } catch (HttpClientErrorException exception) {
                throw new CustomerContactUnavailableException("Customer Service request failed with status " + exception.getStatusCode().value(), exception);

            } catch (HttpServerErrorException exception) {
                int status = exception.getStatusCode().value();

                if (!isRetryableStatus(status)) {
                    throw new CustomerContactUnavailableException("Customer Service request failed with status " + status, exception);
                }

                retryOrThrow(attempt, "HTTP status " + status, exception);
                attempt++;

            } catch (ResourceAccessException exception) {
                retryOrThrow(attempt, "connection or timeout failure", exception);
                attempt++;

            } catch (RestClientException exception) {
                throw new CustomerContactUnavailableException("Unexpected Customer Service response", exception);
            }
        }
    }

    private CustomerContact toCustomerContact(CustomerServiceContactResponse response) {
        if (response == null) {
            throw new CustomerContactUnavailableException("Customer Service returned an empty response", new IllegalStateException("Response body was null"));
        }

        try {
            return new CustomerContact(response.email());
        } catch (IllegalArgumentException exception) {
            throw new CustomerContactUnavailableException("Customer Service returned invalid contact data", exception);
        }
    }

    private boolean isRetryableStatus(int status) {
        return switch (status) {
            case 500, 502, 503, 504 -> true;
            default -> false;
        };
    }

    private void retryOrThrow(int attempt, String reason, Exception exception) {
        if (attempt >= maxAttempts) {
            throw new CustomerContactUnavailableException("Customer Service request failed after " + maxAttempts + " attempts: " + reason, exception);
        }

        LOGGER.warn(
                "Customer Service request failed: {}. Retrying attempt {}/{} after {} ms",
                reason,
                attempt + 1,
                maxAttempts,
                retryBackoff.toMillis()
        );

        try {
            Thread.sleep(retryBackoff.toMillis());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new CustomerContactUnavailableException("Customer Service retry was interrupted", interruptedException);
        }
    }
}
