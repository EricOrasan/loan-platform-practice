package com.btproject.loanplatform.offer_service.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class LoanOffer {

    private final UUID id;
    private final UUID applicationId;
    private final BigDecimal amount;
    private final int periodMonths;
    private final BigDecimal interestRate;
    private final BigDecimal monthlyInstallment;
    private final LoanOfferStatus status;
    private final Instant createdAt;

    private LoanOffer(UUID id, UUID applicationId, BigDecimal amount, int periodMonths, BigDecimal interestRate, BigDecimal monthlyInstallment, LoanOfferStatus status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId must not be null");
        this.amount = validateAmount(amount);
        this.periodMonths = validatePeriodMonths(periodMonths);
        this.interestRate = validateInterestRate(interestRate);
        this.monthlyInstallment = validateMonthlyInstallment(monthlyInstallment);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static LoanOffer generate(UUID applicationId, BigDecimal amount, int periodMonths, int score) {
        BigDecimal validatedAmount = validateAmount(amount);
        int validatedPeriodMonths = validatePeriodMonths(periodMonths);

        BigDecimal interestRate = determineInterestRate(score);

        BigDecimal monthlyInstallment = calculateMonthlyInstallment(validatedAmount, validatedPeriodMonths, interestRate);

        return new LoanOffer(UUID.randomUUID(), applicationId, validatedAmount, validatedPeriodMonths, interestRate, monthlyInstallment, LoanOfferStatus.GENERATED, Instant.now());
    }

    public static LoanOffer restore(UUID id, UUID applicationId, BigDecimal amount, int periodMonths, BigDecimal interestRate, BigDecimal monthlyInstallment, LoanOfferStatus status, Instant createdAt) {
        return new LoanOffer(id, applicationId, amount, periodMonths, interestRate, monthlyInstallment, status, createdAt);
    }

    private static BigDecimal determineInterestRate(int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }

        if (score >= 85) {
            return BigDecimal.valueOf(8.5);
        }

        if (score >= 70) {
            return BigDecimal.valueOf(10.5);
        }

        throw new IllegalArgumentException("score must be at least 70 to generate an offer");
    }

    private static BigDecimal calculateMonthlyInstallment(BigDecimal amount, int periodMonths, BigDecimal annualInterestRate) {
        BigDecimal monthlyInterestRate = annualInterestRate.divide(BigDecimal.valueOf(1200), MathContext.DECIMAL128);

        BigDecimal growthFactor = BigDecimal.ONE
                .add(monthlyInterestRate)
                .pow(periodMonths, MathContext.DECIMAL128);

        BigDecimal numerator = amount
                .multiply(monthlyInterestRate, MathContext.DECIMAL128)
                .multiply(growthFactor, MathContext.DECIMAL128);

        BigDecimal denominator = growthFactor.subtract(
                BigDecimal.ONE,
                MathContext.DECIMAL128
        );

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        return amount;
    }

    private static int validatePeriodMonths(int periodMonths) {
        if (periodMonths < 6 || periodMonths > 120) {
            throw new IllegalArgumentException("periodMonths must be between 6 and 120");
        }

        return periodMonths;
    }

    private static BigDecimal validateInterestRate(BigDecimal interestRate) {
        Objects.requireNonNull(interestRate, "interestRate must not be null");

        if (interestRate.signum() <= 0) {
            throw new IllegalArgumentException("interestRate must be greater than 0");
        }

        return interestRate;
    }

    private static BigDecimal validateMonthlyInstallment(BigDecimal monthlyInstallment) {
        Objects.requireNonNull(monthlyInstallment, "monthlyInstallment must not be null");

        if (monthlyInstallment.signum() <= 0) {
            throw new IllegalArgumentException("monthlyInstallment must be greater than 0");
        }

        return monthlyInstallment;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getPeriodMonths() {
        return periodMonths;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public BigDecimal getMonthlyInstallment() {
        return monthlyInstallment;
    }

    public LoanOfferStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}