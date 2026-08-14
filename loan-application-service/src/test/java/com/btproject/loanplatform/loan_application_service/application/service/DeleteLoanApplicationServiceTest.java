package com.btproject.loanplatform.loan_application_service.application.service;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteLoanApplicationServiceTest {

    private static final UUID APPLICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private LoanApplicationRepository repository;

    private DeleteLoanApplicationService service;

    @BeforeEach
    void setUp() {
        service = new DeleteLoanApplicationService(repository);
    }

    @Test
    void shouldMarkExistingApplicationAsDeletedAndSaveIt() {
        LoanApplication application = restoredWithStatus(LoanApplicationStatus.DRAFT);
        when(repository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));

        service.delete(APPLICATION_ID);

        assertEquals(LoanApplicationStatus.DELETED, application.getStatus());
        verify(repository).save(application);
    }

    @Test
    void shouldThrowWhenApplicationDoesNotExist() {
        when(repository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThrows(
                LoanApplicationNotFoundException.class,
                () -> service.delete(APPLICATION_ID)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldNotSaveWhenOfferWasAlreadyGenerated() {
        LoanApplication application = restoredWithStatus(LoanApplicationStatus.OFFER_GENERATED);
        when(repository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));

        assertThrows(
                InvalidLoanApplicationStatusException.class,
                () -> service.delete(APPLICATION_ID)
        );

        verify(repository, never()).save(any());
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
