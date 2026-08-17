package com.btproject.loanplatform.customer_service.config;

import com.btproject.loanplatform.customer_service.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setHeader(
                HttpHeaders.WWW_AUTHENTICATE,
                "Basic realm=\"customer-service\""
        );

        writeErrorResponse(
                response,
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication is required",
                "Missing or invalid authentication credentials."
        );
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        writeErrorResponse(
                response,
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access is forbidden",
                "You do not have permission to access this resource."
        );
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String code, String message, String details) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                code,
                message,
                details,
                UUID.randomUUID(),
                Instant.now()
        );

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}