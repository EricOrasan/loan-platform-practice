package com.btproject.loanplatform.loan_application_service.infrastructure.web.error;

import com.btproject.loanplatform.loan_application_service.application.exception.EventPublishingUnavailableException;
import com.btproject.loanplatform.loan_application_service.application.exception.LoanApplicationNotFoundException;
import com.btproject.loanplatform.loan_application_service.domain.exception.InvalidLoanApplicationStatusException;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // TODO Replace randomUUID with correlationId

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404 Not Found
    @ExceptionHandler(LoanApplicationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleLoanApplicationNotFoundException(LoanApplicationNotFoundException exception){
        ApiErrorResponse response = new ApiErrorResponse(
                "APPLICATION_NOT_FOUND",
                "Loan Application was not found",
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

    // 400 Bad Request
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidApplicationId(
            MethodArgumentTypeMismatchException exception) {

        ApiErrorResponse response = new ApiErrorResponse(
                "INVALID_APPLICATION_ID",
                "Invalid application ID",
                "Application ID must be a valid UUID.",
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .badRequest()
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

    // 405 Method Not Allowed
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

    // 409 Handles database constraint violations caused by duplicate loan application data.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        LOGGER.warn("Loan Application creation failed because of a database constraint");

        ApiErrorResponse response = new ApiErrorResponse(
                "APPLICATION_ALREADY_EXISTS",
                "Loan application conflicts with existing data",
                "A database integrity constraint was violated.",
                UUID.randomUUID(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    // 409 Invalid Status
    @ExceptionHandler(InvalidLoanApplicationStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidLoanApplicationStatusException(InvalidLoanApplicationStatusException exception){
        ApiErrorResponse response = new ApiErrorResponse(
                "APPLICATION_INVALID_STATUS",
                "Loan application operation is not allowed in the current status",
                exception.getMessage(),
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

    // 503 Kafka Unavailable
    @ExceptionHandler(EventPublishingUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleEventPublishingUnavailable(EventPublishingUnavailableException exception) {
        UUID correlationId = UUID.randomUUID();

        LOGGER.error(
                "Kafka is temporarily unavailable. correlationId={}",
                correlationId,
                exception
        );

        ApiErrorResponse response = new ApiErrorResponse(
                "EVENT_PUBLISHING_UNAVAILABLE",
                "Service temporarily unavailable",
                "Kafka is temporarily unavailable. Please try again later.",
                correlationId,
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

    // 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception exception){
        UUID correlationId = UUID.randomUUID();

        LOGGER.error(
                "Unexpected error. correlationId={}",
                correlationId,
                exception
        );

        ApiErrorResponse response = new ApiErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "Unexpected server-side error",
                "An unexpected error occurred.",
                correlationId,
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

}
