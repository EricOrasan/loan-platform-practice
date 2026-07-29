package com.btproject.loanplatform.customer_service.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // TODO Replace randomUUID with correlationId

    // 404 Customer Not Found
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomerNotFound(CustomerNotFoundException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "CUSTOMER_NOT_FOUND",
                "Customer was not found",
                exception.getMessage(),
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    // 400 Handles method parameter validation errors.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidation(HandlerMethodValidationException exception) {
        String details = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("; "));

        if (details.isBlank()) {
            details = "Request parameter validation failed.";
        }

        ApiErrorResponse response = new ApiErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed",
                details,
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // 400 Handles request body validation errors.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestBodyValidation(MethodArgumentNotValidException exception) {
        String details = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String message = Objects.requireNonNullElse(
                            error.getDefaultMessage(),
                            "Invalid value"
                    );

                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + message;
                    }

                    return message;
                })
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));

        if (details.isBlank()) {
            details = "Request body validation failed.";
        }

        ApiErrorResponse response = new ApiErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed",
                details,
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // 400 Request body is missing, malformed, or contains an invalid value
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequestBody(HttpMessageNotReadableException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "VALIDATION_ERROR",
                "Invalid request body",
                "Request body is missing, malformed, or contains unsupported values.",
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // 409 Customer Already exists
    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomerAlreadyExists(CustomerAlreadyExistsException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "CUSTOMER_ALREADY_EXISTS",
                "Customer already exists",
                exception.getMessage(),
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    // 409 Handles database constraint violations caused by duplicate customer data.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        LOGGER.warn("Customer creation failed because of a database constraint");

        ApiErrorResponse response = new ApiErrorResponse(
                "CUSTOMER_ALREADY_EXISTS",
                "Customer already exists",
                "A customer with the provided CIF or email already exists.",
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    // 415 Unsupported Media Type
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "UNSUPPORTED_MEDIA_TYPE",
                "Unsupported media type",
                "Content-Type must be application/json.",
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(response);
    }


    // 503 Database Unavailable
    @ExceptionHandler({
            CannotCreateTransactionException.class,
            DataAccessResourceFailureException.class,
            TransientDataAccessResourceException.class,
            QueryTimeoutException.class
    })
    public ResponseEntity<ApiErrorResponse> handleDatabaseUnavailable(Exception exception) {
        LOGGER.error("Database is temporarily unavailable", exception);

        ApiErrorResponse response = new ApiErrorResponse(
                "DATABASE_ERROR",
                "Service temporarily unavailable",
                "The database is temporarily unavailable. Please try again later.",
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

    // 405 Method Not Allowed (PUT, PATCH, etc.)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "METHOD_NOT_ALLOWED",
                "Method not allowed",
                "The requested HTTP method is not supported for this endpoint.",
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(response);
    }

    // 404 Bad Request URL
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(NoResourceFoundException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "RESOURCE_NOT_FOUND",
                "Resource was not found",
                "The requested endpoint does not exist.",
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
        LOGGER.error("Unexpected internal server error", exception);

        ApiErrorResponse response = new ApiErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "Internal server error",
                "An unexpected error occurred.",
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

}
