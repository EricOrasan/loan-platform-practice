package com.btproject.loanplatform.offer_service.infrastructure.persistence;

import com.btproject.loanplatform.offer_service.domain.LoanOfferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter

@Entity
@Table(name = "loan_offer")
public class LoanOfferJpaEntity {

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false, unique = true)
    private UUID applicationId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "period_months", nullable = false)
    private int periodMonths;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "monthly_installment", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyInstallment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoanOfferStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}