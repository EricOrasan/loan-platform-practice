package com.btproject.loanplatform.credit_assessment_service.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreditAssessmentTest {

    private static final UUID APPLICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String CIF = "12345678";

    @Test
    void shouldApproveCustomerWithLowRiskAndIncomeAtApprovalThreshold() {
        CustomerFinancialProfile customer = customer(BigDecimal.valueOf(7000), RiskCategory.LOW);

        CreditAssessment assessment = CreditAssessment.assess(APPLICATION_ID, CIF, customer);

        assertEquals(90, assessment.getScore());
        assertEquals(AssessmentDecision.APPROVED, assessment.getDecision());
        assertEquals(AssessmentReason.CUSTOMER_ELIGIBLE, assessment.getReason());
    }

    @Test
    void shouldRequireManualReviewForMediumRiskCustomerAtManualReviewThreshold() {
        CustomerFinancialProfile customer = customer(BigDecimal.valueOf(4000), RiskCategory.MEDIUM);

        CreditAssessment assessment = CreditAssessment.assess(APPLICATION_ID, CIF, customer);

        assertEquals(65, assessment.getScore());
        assertEquals(AssessmentDecision.MANUAL_REVIEW, assessment.getDecision());
        assertEquals(AssessmentReason.MANUAL_REVIEW_REQUIRED, assessment.getReason());
    }

    @Test
    void shouldRejectCustomerWhoDoesNotMeetEligibilityRules() {
        CustomerFinancialProfile customer = customer(BigDecimal.valueOf(3999), RiskCategory.MEDIUM);

        CreditAssessment assessment = CreditAssessment.assess(APPLICATION_ID, CIF, customer);

        assertEquals(40, assessment.getScore());
        assertEquals(AssessmentDecision.REJECTED, assessment.getDecision());
        assertEquals(AssessmentReason.CUSTOMER_NOT_ELIGIBLE, assessment.getReason());
    }

    @Test
    void shouldRejectHighRiskCustomerEvenWhenIncomeIsHigh() {
        CustomerFinancialProfile customer = customer(BigDecimal.valueOf(10_000), RiskCategory.HIGH);

        CreditAssessment assessment = CreditAssessment.assess(APPLICATION_ID, CIF, customer);

        assertEquals(40, assessment.getScore());
        assertEquals(AssessmentDecision.REJECTED, assessment.getDecision());
        assertEquals(AssessmentReason.CUSTOMER_NOT_ELIGIBLE, assessment.getReason());
    }

    @Test
    void shouldRejectLowRiskCustomerWhenIncomeIsBelowApprovalThreshold() {
        CustomerFinancialProfile customer = customer(BigDecimal.valueOf(6999), RiskCategory.LOW);

        CreditAssessment assessment = CreditAssessment.assess(APPLICATION_ID, CIF, customer);

        assertEquals(40, assessment.getScore());
        assertEquals(AssessmentDecision.REJECTED, assessment.getDecision());
        assertEquals(AssessmentReason.CUSTOMER_NOT_ELIGIBLE, assessment.getReason());
    }

    @Test
    void shouldRejectWhenCustomerDoesNotExist() {
        CreditAssessment assessment = CreditAssessment.customerNotFound(APPLICATION_ID, CIF);

        assertEquals(0, assessment.getScore());
        assertEquals(AssessmentDecision.REJECTED, assessment.getDecision());
        assertEquals(AssessmentReason.CUSTOMER_NOT_FOUND, assessment.getReason());
    }

    @Test
    void shouldRequireManualReviewWhenCustomerInformationIsUnavailable() {
        CreditAssessment assessment = CreditAssessment.technicalFailure(APPLICATION_ID, CIF);

        assertEquals(0, assessment.getScore());
        assertEquals(AssessmentDecision.MANUAL_REVIEW, assessment.getDecision());
        assertEquals(AssessmentReason.TECHNICAL_PROCESSING_FAILED, assessment.getReason());
    }

    @Test
    void shouldRejectAssessmentWhenCustomerCifDoesNotMatchApplicationCif() {
        CustomerFinancialProfile customer = new CustomerFinancialProfile(
                "87654321",
                BigDecimal.valueOf(7000),
                RiskCategory.LOW
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> CreditAssessment.assess(APPLICATION_ID, CIF, customer)
        );

        assertEquals("Customer CIF does not match assessment CIF", exception.getMessage());
    }

    private static CustomerFinancialProfile customer(BigDecimal monthlyIncome, RiskCategory riskCategory) {
        return new CustomerFinancialProfile(CIF, monthlyIncome, riskCategory);
    }
}
