package com.btproject.loanplatform.credit_assessment_service.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class CreditAssessment {

    private final UUID id;
    private final UUID applicationId;
    private final String cif;
    private final int score;
    private final AssessmentDecision decision;
    private final AssessmentReason reason;
    private final Instant createdAt;

    private static final Pattern CIF_PATTERN = Pattern.compile("[0-9]{8}");

    private CreditAssessment(UUID id, UUID applicationId, String cif, int score, AssessmentDecision decision, AssessmentReason reason, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId must not be null");
        this.cif = validateCif(cif);
        this.score = validateScore(score);
        this.decision = Objects.requireNonNull(decision, "decision must not be null");
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static CreditAssessment create(UUID applicationId, String cif, int score, AssessmentDecision decision, AssessmentReason reason) {
        return new CreditAssessment(UUID.randomUUID(), applicationId, cif, score, decision, reason, Instant.now());
    }

    public static CreditAssessment restore(UUID id, UUID applicationId, String cif, int score, AssessmentDecision decision, AssessmentReason reason, Instant createdAt) {
        return new CreditAssessment(id, applicationId, cif, score, decision, reason, createdAt);
    }

    public static CreditAssessment assess(UUID applicationId, String cif, CustomerFinancialProfile customer) {
        Objects.requireNonNull(customer, "customer must not be null");

        if (!cif.equals(customer.cif())) {
            throw new IllegalArgumentException("Customer CIF does not match assessment CIF");
        }

        if (customer.monthlyIncome().compareTo(BigDecimal.valueOf(7000)) >= 0 && customer.riskCategory() == RiskCategory.LOW) {
            return create(applicationId, cif, 90, AssessmentDecision.APPROVED, AssessmentReason.CUSTOMER_ELIGIBLE);
        }
        if (customer.monthlyIncome().compareTo(BigDecimal.valueOf(4000)) >= 0 && customer.riskCategory() == RiskCategory.MEDIUM) {
            return create(applicationId, cif, 65, AssessmentDecision.MANUAL_REVIEW, AssessmentReason.MANUAL_REVIEW_REQUIRED);
        }

        return create(applicationId, cif, 40, AssessmentDecision.REJECTED, AssessmentReason.CUSTOMER_NOT_ELIGIBLE);
    }

    public static CreditAssessment customerNotFound(UUID applicationId, String cif) {
        return create(applicationId, cif, 0, AssessmentDecision.REJECTED, AssessmentReason.CUSTOMER_NOT_FOUND);
    }

    public static CreditAssessment technicalFailure(UUID applicationId, String cif) {
        return create(applicationId, cif, 0, AssessmentDecision.MANUAL_REVIEW, AssessmentReason.TECHNICAL_PROCESSING_FAILED);
    }

    private static String validateCif(String cif) {
        if (cif == null || !CIF_PATTERN.matcher(cif).matches()) {
            throw new IllegalArgumentException("CIF must contain exactly 8 digits");
        }
        return cif;
    }

    private static int validateScore(int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Score must be between 0 and 100");
        }
        return score;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getCif() {
        return cif;
    }

    public int getScore() {
        return score;
    }

    public AssessmentDecision getDecision() {
        return decision;
    }

    public AssessmentReason getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}