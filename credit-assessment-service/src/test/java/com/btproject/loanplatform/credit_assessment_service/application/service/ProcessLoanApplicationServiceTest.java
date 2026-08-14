package com.btproject.loanplatform.credit_assessment_service.application.service;

import com.btproject.loanplatform.credit_assessment_service.application.command.ProcessLoanApplicationCommand;
import com.btproject.loanplatform.credit_assessment_service.application.exception.CustomerInformationUnavailableException;
import com.btproject.loanplatform.credit_assessment_service.application.port.out.CreditAssessmentEventPublisher;
import com.btproject.loanplatform.credit_assessment_service.application.port.out.CreditAssessmentRepository;
import com.btproject.loanplatform.credit_assessment_service.application.port.out.CustomerInformationPort;
import com.btproject.loanplatform.credit_assessment_service.domain.AssessmentDecision;
import com.btproject.loanplatform.credit_assessment_service.domain.AssessmentReason;
import com.btproject.loanplatform.credit_assessment_service.domain.CreditAssessment;
import com.btproject.loanplatform.credit_assessment_service.domain.CustomerFinancialProfile;
import com.btproject.loanplatform.credit_assessment_service.domain.RiskCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessLoanApplicationServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID APPLICATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String CIF = "12345678";
    private static final BigDecimal REQUESTED_AMOUNT = BigDecimal.valueOf(30_000);
    private static final int REQUESTED_PERIOD_MONTHS = 48;
    private static final Instant APPLICATION_CREATED_AT = Instant.parse("2026-08-14T10:00:00Z");

    @Mock
    private CreditAssessmentRepository repository;

    @Mock
    private CustomerInformationPort customerInformationPort;

    @Mock
    private CreditAssessmentEventPublisher eventPublisher;

    private ProcessLoanApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ProcessLoanApplicationService(repository, customerInformationPort, eventPublisher);
    }

    @Test
    void shouldSkipApplicationThatWasAlreadyProcessed() {
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(true);

        service.process(command());

        verify(repository, never()).save(any());
        verifyNoInteractions(customerInformationPort, eventPublisher);
    }

    @Test
    void shouldAssessSaveAndPublishWhenCustomerExists() {
        CustomerFinancialProfile customer = new CustomerFinancialProfile(
                CIF,
                BigDecimal.valueOf(7000),
                RiskCategory.LOW
        );
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(false);
        when(customerInformationPort.findByCif(CIF)).thenReturn(Optional.of(customer));
        when(repository.save(any(CreditAssessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.process(command());

        CreditAssessment savedAssessment = captureSavedAssessment();
        assertEquals(90, savedAssessment.getScore());
        assertEquals(AssessmentDecision.APPROVED, savedAssessment.getDecision());
        assertEquals(AssessmentReason.CUSTOMER_ELIGIBLE, savedAssessment.getReason());
        verify(eventPublisher).publishCompleted(
                same(savedAssessment),
                same(REQUESTED_AMOUNT),
                org.mockito.ArgumentMatchers.eq(REQUESTED_PERIOD_MONTHS)
        );
    }

    @Test
    void shouldSaveRejectedAssessmentWhenCustomerDoesNotExist() {
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(false);
        when(customerInformationPort.findByCif(CIF)).thenReturn(Optional.empty());
        when(repository.save(any(CreditAssessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.process(command());

        CreditAssessment savedAssessment = captureSavedAssessment();
        assertEquals(0, savedAssessment.getScore());
        assertEquals(AssessmentDecision.REJECTED, savedAssessment.getDecision());
        assertEquals(AssessmentReason.CUSTOMER_NOT_FOUND, savedAssessment.getReason());
        verify(eventPublisher).publishCompleted(
                same(savedAssessment),
                same(REQUESTED_AMOUNT),
                org.mockito.ArgumentMatchers.eq(REQUESTED_PERIOD_MONTHS)
        );
    }

    @Test
    void shouldSaveManualReviewAssessmentWhenCustomerInformationIsUnavailable() {
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(false);
        when(customerInformationPort.findByCif(CIF)).thenThrow(
                new CustomerInformationUnavailableException("Customer Service unavailable", new RuntimeException())
        );
        when(repository.save(any(CreditAssessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.process(command());

        CreditAssessment savedAssessment = captureSavedAssessment();
        assertEquals(0, savedAssessment.getScore());
        assertEquals(AssessmentDecision.MANUAL_REVIEW, savedAssessment.getDecision());
        assertEquals(AssessmentReason.TECHNICAL_PROCESSING_FAILED, savedAssessment.getReason());
        verify(eventPublisher).publishCompleted(
                same(savedAssessment),
                same(REQUESTED_AMOUNT),
                org.mockito.ArgumentMatchers.eq(REQUESTED_PERIOD_MONTHS)
        );
    }

    @Test
    void shouldPropagateUnexpectedCustomerLookupFailureWithoutSavingOrPublishing() {
        IllegalStateException failure = new IllegalStateException("Unexpected response");
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(false);
        when(customerInformationPort.findByCif(CIF)).thenThrow(failure);

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> service.process(command())
        );

        assertEquals(failure, thrown);
        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    private CreditAssessment captureSavedAssessment() {
        ArgumentCaptor<CreditAssessment> captor = ArgumentCaptor.forClass(CreditAssessment.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static ProcessLoanApplicationCommand command() {
        return new ProcessLoanApplicationCommand(
                EVENT_ID,
                APPLICATION_ID,
                CIF,
                REQUESTED_AMOUNT,
                REQUESTED_PERIOD_MONTHS,
                APPLICATION_CREATED_AT
        );
    }
}
