package com.btproject.loanplatform.loan_application_service.application.service;

import com.btproject.loanplatform.loan_application_service.application.command.UpdateLoanApplicationCommand;
import com.btproject.loanplatform.loan_application_service.application.exception.LoanApplicationNotFoundException;
import com.btproject.loanplatform.loan_application_service.application.port.out.LoanApplicationRepository;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplicationStatus;
import com.btproject.loanplatform.loan_application_service.domain.exception.InvalidLoanApplicationStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateLoanApplicationServiceTest {

    private static final UUID APPLICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private LoanApplicationRepository repository;

    private UpdateLoanApplicationService service;

    @BeforeEach
    void setUp() {
        service = new UpdateLoanApplicationService(repository);
    }

    @Test
    void shouldUpdateAndSaveExistingApplication() {
        LoanApplication application = draftApplication();
        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(
                BigDecimal.valueOf(35_000),
                60,
                "Updated purpose"
        );
        when(repository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(repository.save(application)).thenReturn(application);

        LoanApplication result = service.update(APPLICATION_ID, command);

        assertSame(application, result);
        assertEquals(BigDecimal.valueOf(35_000), result.getRequestedAmount());
        assertEquals(60, result.getRequestedPeriodMonths());
        assertEquals("Updated purpose", result.getPurpose());
        verify(repository).save(application);
    }

    @Test
    void shouldThrowWhenApplicationDoesNotExist() {
        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(
                BigDecimal.valueOf(35_000),
                60,
                "Updated purpose"
        );
        when(repository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThrows(
                LoanApplicationNotFoundException.class,
                () -> service.update(APPLICATION_ID, command)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldNotSaveWhenApplicationStatusDoesNotAllowUpdate() {
        LoanApplication application = restoredWithStatus(LoanApplicationStatus.APPROVED);
        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(
                BigDecimal.valueOf(35_000),
                60,
                "Updated purpose"
        );
        when(repository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));

        assertThrows(
                InvalidLoanApplicationStatusException.class,
                () -> service.update(APPLICATION_ID, command)
        );

        verify(repository, never()).save(any());
    }

    private static LoanApplication draftApplication() {
        return restoredWithStatus(LoanApplicationStatus.DRAFT);
    }

    private static LoanApplication restoredWithStatus(LoanApplicationStatus status) {
        Instant createdAt = Instant.parse("2026-08-14T10:00:00Z");
        return LoanApplication.restore(
                APPLICATION_ID,
                "LA-11111111-1111-1111-1111-111111111111",
                "12345678",
                BigDecimal.valueOf(30_000),
                48,
                "Home renovation",
                status,
                createdAt,
                createdAt
        );
    }
}
