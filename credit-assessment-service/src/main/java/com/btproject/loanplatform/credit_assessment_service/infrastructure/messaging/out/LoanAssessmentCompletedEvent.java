package com.btproject.loanplatform.credit_assessment_service.infrastructure.messaging.out;

import com.btproject.loanplatform.credit_assessment_service.domain.AssessmentDecision;
import com.btproject.loanplatform.credit_assessment_service.domain.AssessmentReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanAssessmentCompletedEvent(
        UUID eventId,
        String eventType,
        UUID applicationId,
        String cif,
        BigDecimal requestedAmount,
        int requestedPeriodMonths,
        int score,
        AssessmentDecision decision,
        AssessmentReason reason,
        Instant createdAt
) {
}