package com.btproject.loanplatform.offer_service.application.service;

import com.btproject.loanplatform.offer_service.application.command.GenerateLoanOfferCommand;
import com.btproject.loanplatform.offer_service.application.port.out.LoanOfferEventPublisher;
import com.btproject.loanplatform.offer_service.application.port.out.LoanOfferRepository;
import com.btproject.loanplatform.offer_service.domain.AssessmentDecision;
import com.btproject.loanplatform.offer_service.domain.LoanOffer;
import com.btproject.loanplatform.offer_service.domain.LoanOfferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
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
class GenerateLoanOfferServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID APPLICATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String CIF = "12345678";
    private static final BigDecimal REQUESTED_AMOUNT = BigDecimal.valueOf(42_000);
    private static final int REQUESTED_PERIOD_MONTHS = 60;
    private static final Instant ASSESSMENT_CREATED_AT = Instant.parse("2026-08-14T10:00:00Z");

    @Mock
    private LoanOfferRepository repository;

    @Mock
    private LoanOfferEventPublisher eventPublisher;

    private GenerateLoanOfferService service;

    @BeforeEach
    void setUp() {
        service = new GenerateLoanOfferService(repository, eventPublisher);
    }

    @Test
    void shouldSkipRejectedAssessment() {
        service.generate(command(AssessmentDecision.REJECTED, 40));

        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void shouldSkipAssessmentThatRequiresManualReview() {
        service.generate(command(AssessmentDecision.MANUAL_REVIEW, 65));

        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void shouldSkipApplicationThatAlreadyHasAnOffer() {
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(true);

        service.generate(command(AssessmentDecision.APPROVED, 90));

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldGenerateSaveAndPublishApprovedOffer() {
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(false);
        when(repository.save(any(LoanOffer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.generate(command(AssessmentDecision.APPROVED, 90));

        ArgumentCaptor<LoanOffer> captor = ArgumentCaptor.forClass(LoanOffer.class);
        verify(repository).save(captor.capture());

        LoanOffer savedOffer = captor.getValue();
        assertEquals(APPLICATION_ID, savedOffer.getApplicationId());
        assertEquals(REQUESTED_AMOUNT, savedOffer.getAmount());
        assertEquals(REQUESTED_PERIOD_MONTHS, savedOffer.getPeriodMonths());
        assertEquals(BigDecimal.valueOf(8.5), savedOffer.getInterestRate());
        assertEquals(LoanOfferStatus.GENERATED, savedOffer.getStatus());
        verify(eventPublisher).publishGenerated(same(savedOffer), org.mockito.ArgumentMatchers.eq(CIF));
    }

    @Test
    void shouldNotSaveOrPublishWhenApprovedScoreCannotGenerateOffer() {
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(command(AssessmentDecision.APPROVED, 69))
        );

        assertEquals("score must be at least 70 to generate an offer", exception.getMessage());
        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    private static GenerateLoanOfferCommand command(AssessmentDecision decision, int score) {
        return new GenerateLoanOfferCommand(
                EVENT_ID,
                APPLICATION_ID,
                CIF,
                REQUESTED_AMOUNT,
                REQUESTED_PERIOD_MONTHS,
                score,
                decision,
                ASSESSMENT_CREATED_AT
        );
    }
}
