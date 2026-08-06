package com.btproject.loanplatform.credit_assessment_service.infrastructure.persistence;

import com.btproject.loanplatform.credit_assessment_service.domain.AssessmentDecision;
import com.btproject.loanplatform.credit_assessment_service.domain.AssessmentReason;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter

@Entity
@Table(name = "credit_assessment")
public class CreditAssessmentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false, unique = true)
    private UUID applicationId;

    @Column(nullable = false, length = 8)
    private String cif;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentReason reason;

    @Column(name = "created_at",  nullable = false, updatable = false)
    private Instant createdAt;
}
