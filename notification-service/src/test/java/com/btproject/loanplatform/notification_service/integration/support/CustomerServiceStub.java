package com.btproject.loanplatform.notification_service.integration.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

public final class CustomerServiceStub implements AutoCloseable {

    private static final StubResponse DEFAULT_RESPONSE = new StubResponse(500, "");

    private final HttpServer server;
    private final ExecutorService executor;
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicReference<String> authorizationHeader = new AtomicReference<>();
    private final AtomicReference<IntFunction<StubResponse>> responsePlan = new AtomicReference<>(attempt -> DEFAULT_RESPONSE);

    public CustomerServiceStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            executor = Executors.newCachedThreadPool();
            server.setExecutor(executor);
            server.createContext("/customers", this::handleRequest);
            server.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start Customer Service test stub", exception);
        }
    }

    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public void respond(int status, String body) {
        respondWithPlan(attempt -> new StubResponse(status, body));
    }

    public void respondInSequence(StubResponse... responses) {
        if (responses.length == 0) {
            throw new IllegalArgumentException("At least one response is required");
        }

        StubResponse[] copy = responses.clone();
        respondWithPlan(attempt -> copy[Math.min(attempt - 1, copy.length - 1)]);
    }

    public int requestCount() {
        return requestCount.get();
    }

    public String authorizationHeader() {
        return authorizationHeader.get();
    }

    public void reset() {
        requestCount.set(0);
        authorizationHeader.set(null);
        responsePlan.set(attempt -> DEFAULT_RESPONSE);
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void respondWithPlan(IntFunction<StubResponse> plan) {
        requestCount.set(0);
        authorizationHeader.set(null);
        responsePlan.set(Objects.requireNonNull(plan));
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        int attempt = requestCount.incrementAndGet();
        authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
        StubResponse response = responsePlan.get().apply(attempt);
        pause(response.delay());
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(response.status(), body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static void pause(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Customer Service stub response was interrupted", exception);
        }
    }

    public record StubResponse(int status, String body, Duration delay) {
        public StubResponse(int status, String body) {
            this(status, body, Duration.ZERO);
        }

        public StubResponse {
            Objects.requireNonNull(body);
            Objects.requireNonNull(delay);

            if (delay.isNegative()) {
                throw new IllegalArgumentException("delay must not be negative");
            }
        }
    }
}
