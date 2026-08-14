package com.btproject.loanplatform.loan_application_service.domain;

import com.btproject.loanplatform.loan_application_service.domain.exception.InvalidLoanApplicationStatusException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanApplicationTest {

    private static final String CIF = "12345678";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(30_000);

    @Test
    void shouldCreateDraftApplicationWithNormalizedPurpose() {
        LoanApplication application = new LoanApplication(CIF, AMOUNT, 48, "  Home renovation  ");

        assertEquals(CIF, application.getCif());
        assertEquals(AMOUNT, application.getRequestedAmount());
        assertEquals(48, application.getRequestedPeriodMonths());
        assertEquals("Home renovation", application.getPurpose());
        assertEquals(LoanApplicationStatus.DRAFT, application.getStatus());
        assertTrue(application.getApplicationNumber().startsWith("LA-"));
        assertEquals(application.getCreatedAt(), application.getUpdatedAt());
    }

    @Test
    void shouldUpdateDraftApplication() {
        LoanApplication application = new LoanApplication(CIF, AMOUNT, 48, "Initial purpose");

        application.update(BigDecimal.valueOf(35_000), 60, "  Updated purpose  ");

        assertEquals(BigDecimal.valueOf(35_000), application.getRequestedAmount());
        assertEquals(60, application.getRequestedPeriodMonths());
        assertEquals("Updated purpose", application.getPurpose());
        assertFalse(application.getUpdatedAt().isBefore(application.getCreatedAt()));
    }

    @Test
    void shouldAllowUpdatingSubmittedApplication() {
        LoanApplication application = restoredWithStatus(LoanApplicationStatus.SUBMITTED);

        application.update(BigDecimal.valueOf(32_000), 36, "Updated while submitted");

        assertEquals(BigDecimal.valueOf(32_000), application.getRequestedAmount());
        assertEquals(36, application.getRequestedPeriodMonths());
    }

    @Test
    void shouldRejectUpdateWhenApplicationIsNoLongerEditable() {
        LoanApplication application = restoredWithStatus(LoanApplicationStatus.APPROVED);

        InvalidLoanApplicationStatusException exception = assertThrows(
                InvalidLoanApplicationStatusException.class,
                () -> application.update(BigDecimal.valueOf(35_000), 60, "Updated purpose")
        );

        assertEquals("Cannot update loan application in status APPROVED", exception.getMessage());
    }

    @Test
    void shouldMarkApplicationAsDeleted() {
        LoanApplication application = restoredWithStatus(LoanApplicationStatus.APPROVED);

        application.markAsDeleted();

        assertEquals(LoanApplicationStatus.DELETED, application.getStatus());
        assertFalse(application.getUpdatedAt().isBefore(application.getCreatedAt()));
    }

    @Test
    void shouldRejectDeletionAfterOfferWasGenerated() {
        LoanApplication application = restoredWithStatus(LoanApplicationStatus.OFFER_GENERATED);

        InvalidLoanApplicationStatusException exception = assertThrows(
                InvalidLoanApplicationStatusException.class,
                application::markAsDeleted
        );

        assertEquals("Cannot delete loan application in status OFFER_GENERATED", exception.getMessage());
    }

    @Test
    void shouldRejectDeletingApplicationTwice() {
        LoanApplication application = restoredWithStatus(LoanApplicationStatus.DELETED);

        assertThrows(InvalidLoanApplicationStatusException.class, application::markAsDeleted);
    }

    @Test
    void shouldRejectInvalidCif() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LoanApplication("123", AMOUNT, 48, "Purpose")
        );
    }

    @Test
    void shouldRejectNonPositiveRequestedAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LoanApplication(CIF, BigDecimal.ZERO, 48, "Purpose")
        );
    }

    @Test
    void shouldRejectPeriodOutsideAllowedRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LoanApplication(CIF, AMOUNT, 121, "Purpose")
        );
    }

    @Test
    void shouldRejectBlankPurpose() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LoanApplication(CIF, AMOUNT, 48, "  ")
        );
    }

    private static LoanApplication restoredWithStatus(LoanApplicationStatus status) {
        Instant createdAt = Instant.parse("2026-08-14T10:00:00Z");
        return LoanApplication.restore(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "LA-11111111-1111-1111-1111-111111111111",
                CIF,
                AMOUNT,
                48,
                "Purpose",
                status,
                createdAt,
                createdAt
        );
    }
}
