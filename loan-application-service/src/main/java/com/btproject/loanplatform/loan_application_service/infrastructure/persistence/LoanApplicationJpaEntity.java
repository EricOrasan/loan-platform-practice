package com.btproject.loanplatform.loan_application_service.infrastructure.persistence;

import com.btproject.loanplatform.loan_application_service.domain.LoanApplicationStatus;
import jakarta.persistence.*;
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
@Table(name = "loan_application")
public class LoanApplicationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "application_number", nullable = false, unique = true, length = 39)
    private String applicationNumber;

    @Column(nullable = false, length = 8)
    private String cif;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "requested_period_months", nullable = false)
    private int requestedPeriodMonths;

    @Column(nullable = false)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoanApplicationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
