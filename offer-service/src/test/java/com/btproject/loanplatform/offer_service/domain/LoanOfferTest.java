package com.btproject.loanplatform.offer_service.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoanOfferTest {

    private static final UUID APPLICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldGenerateHighScoreOfferWithPreferredInterestRate() {
        LoanOffer offer = LoanOffer.generate(
                APPLICATION_ID,
                BigDecimal.valueOf(42_000),
                60,
                90
        );

        assertEquals(BigDecimal.valueOf(8.5), offer.getInterestRate());
        assertEquals(new BigDecimal("861.69"), offer.getMonthlyInstallment());
        assertEquals(LoanOfferStatus.GENERATED, offer.getStatus());
    }

    @Test
    void shouldGenerateOfferAtMinimumAcceptedScore() {
        LoanOffer offer = LoanOffer.generate(
                APPLICATION_ID,
                BigDecimal.valueOf(30_000),
                48,
                70
        );

        assertEquals(BigDecimal.valueOf(10.5), offer.getInterestRate());
        assertEquals(new BigDecimal("768.10"), offer.getMonthlyInstallment());
        assertEquals(LoanOfferStatus.GENERATED, offer.getStatus());
    }

    @Test
    void shouldRejectScoreBelowOfferThreshold() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> LoanOffer.generate(APPLICATION_ID, BigDecimal.valueOf(30_000), 48, 69)
        );

        assertEquals("score must be at least 70 to generate an offer", exception.getMessage());
    }

    @Test
    void shouldRejectScoreOutsideAllowedRange() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> LoanOffer.generate(APPLICATION_ID, BigDecimal.valueOf(30_000), 48, 101)
        );

        assertEquals("score must be between 0 and 100", exception.getMessage());
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> LoanOffer.generate(APPLICATION_ID, BigDecimal.ZERO, 48, 90)
        );

        assertEquals("amount must be greater than 0", exception.getMessage());
    }

    @Test
    void shouldRejectPeriodOutsideAllowedRange() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> LoanOffer.generate(APPLICATION_ID, BigDecimal.valueOf(30_000), 121, 90)
        );

        assertEquals("periodMonths must be between 6 and 120", exception.getMessage());
    }
}
