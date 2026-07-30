package com.btproject.loanplatform.loan_application_service.domain;

import com.btproject.loanplatform.loan_application_service.domain.exception.InvalidLoanApplicationStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class LoanApplication {

    private final UUID id;
    private final String applicationNumber;
    private final String cif;
    private BigDecimal requestedAmount;
    private int requestedPeriodMonths;
    private String purpose;
    private LoanApplicationStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private static final Pattern CIF_PATTERN = Pattern.compile("^[0-9]{8}$");

    public LoanApplication(String cif, BigDecimal requestedAmount, int requestedPeriodMonths, String purpose) {
        this.id = UUID.randomUUID();
        this.applicationNumber = generateApplicationNumber();
        this.cif = verifyCif(cif);
        this.requestedAmount = verifyRequestedAmount(requestedAmount);
        this.requestedPeriodMonths = verifyRequestedPeriodMonths(requestedPeriodMonths);
        this.purpose = verifyPurpose(purpose);
        this.status = LoanApplicationStatus.DRAFT;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    private LoanApplication(UUID id, String applicationNumber, String cif, BigDecimal requestedAmount, int requestedPeriodMonths, String purpose, LoanApplicationStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.applicationNumber = Objects.requireNonNull(applicationNumber, "applicationNumber must not be null");
        this.cif = verifyCif(cif);
        this.requestedAmount = verifyRequestedAmount(requestedAmount);
        this.requestedPeriodMonths = verifyRequestedPeriodMonths(requestedPeriodMonths);
        this.purpose = verifyPurpose(purpose);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    public static LoanApplication restore(UUID id, String applicationNumber, String cif, BigDecimal requestedAmount, int requestedPeriodMonths, String purpose, LoanApplicationStatus status, Instant createdAt, Instant updatedAt) {
        return new LoanApplication(id, applicationNumber, cif, requestedAmount, requestedPeriodMonths, purpose, status, createdAt, updatedAt);
    }

    public void update(BigDecimal requestedAmount, int requestedPeriodMonths, String purpose) {

        if (this.status != LoanApplicationStatus.DRAFT && this.status != LoanApplicationStatus.SUBMITTED) {
            throw new InvalidLoanApplicationStatusException(this.status, "update");
        }

        this.requestedAmount = verifyRequestedAmount(requestedAmount);
        this.requestedPeriodMonths = verifyRequestedPeriodMonths(requestedPeriodMonths);
        this.purpose = verifyPurpose(purpose);
        this.updatedAt = Instant.now();
    }

    public void markAsDeleted() {

        if (this.status == LoanApplicationStatus.OFFER_GENERATED || this.status == LoanApplicationStatus.DELETED) {
            throw new InvalidLoanApplicationStatusException(this.status, "delete");
        }

        this.status = LoanApplicationStatus.DELETED;
        this.updatedAt = Instant.now();
    }

    private static String generateApplicationNumber() {
        return "LA-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
    }

    private static String verifyCif(String cif) {
        if (cif == null || !CIF_PATTERN.matcher(cif).matches()) {
            throw new IllegalArgumentException("CIF must contain exactly 8 digits");
        }
        return cif;
    }

    private static BigDecimal verifyRequestedAmount(BigDecimal requestedAmount) {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return requestedAmount;
    }

    private static int verifyRequestedPeriodMonths(int requestedPeriodMonths) {
        if  (requestedPeriodMonths < 6 || requestedPeriodMonths > 120) {
            throw new IllegalArgumentException("Requested period must be between 6 and 120 months");
        }
        return requestedPeriodMonths;
    }

    private static String verifyPurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Purpose must not be blank");
        }

        String normalizedPurpose = purpose.trim();

        if (normalizedPurpose.length() > 255) {
            throw new IllegalArgumentException("Purpose must not exceed 255 characters");
        }
        return normalizedPurpose;
    }

    public UUID getId() {
        return id;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public String getCif() {
        return cif;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public int getRequestedPeriodMonths() {
        return requestedPeriodMonths;
    }

    public String getPurpose() {
        return purpose;
    }

    public LoanApplicationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
