package com.btproject.loanplatform.loan_application_service.application.service;

import com.btproject.loanplatform.loan_application_service.application.exception.LoanApplicationNotFoundException;
import com.btproject.loanplatform.loan_application_service.application.port.out.LoanApplicationRepository;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository repository;

    private GetLoanApplicationService service;

    @BeforeEach
    void setUp() {
        service = new GetLoanApplicationService(repository);
    }

    @Test
    void shouldReturnExistingApplication() {
        LoanApplication application = application();
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));

        LoanApplication result = service.get(application.getId());

        assertSame(application, result);
    }

    @Test
    void shouldThrowWhenApplicationDoesNotExist() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(LoanApplicationNotFoundException.class, () -> service.get(id));
    }

    @Test
    void shouldTreatDeletedApplicationAsNotFound() {
        LoanApplication application = application();
        application.markAsDeleted();
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));

        assertThrows(
                LoanApplicationNotFoundException.class,
                () -> service.get(application.getId())
        );
    }

    private static LoanApplication application() {
        return new LoanApplication(
                "12345678",
                BigDecimal.valueOf(30_000),
                48,
                "Home renovation"
        );
    }
}
