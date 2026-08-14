package com.btproject.loanplatform.loan_application_service.application.service;

import com.btproject.loanplatform.loan_application_service.application.command.CreateLoanApplicationCommand;
import com.btproject.loanplatform.loan_application_service.application.port.out.LoanApplicationEventPublisher;
import com.btproject.loanplatform.loan_application_service.application.port.out.LoanApplicationRepository;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateLoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository repository;

    @Mock
    private LoanApplicationEventPublisher publisher;

    private CreateLoanApplicationService service;

    @BeforeEach
    void setUp() {
        service = new CreateLoanApplicationService(repository, publisher);
    }

    @Test
    void shouldCreateSavePublishAndReturnLoanApplication() {
        CreateLoanApplicationCommand command = new CreateLoanApplicationCommand(
                "12345678",
                BigDecimal.valueOf(30_000),
                48,
                "Home renovation"
        );
        when(repository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanApplication result = service.create(command);

        ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(repository).save(captor.capture());
        LoanApplication savedApplication = captor.getValue();
        assertSame(savedApplication, result);
        assertEquals("12345678", result.getCif());
        assertEquals(BigDecimal.valueOf(30_000), result.getRequestedAmount());
        assertEquals(48, result.getRequestedPeriodMonths());
        assertEquals(LoanApplicationStatus.DRAFT, result.getStatus());
        verify(publisher).publishCreated(same(savedApplication));
    }

    @Test
    void shouldNotPublishWhenSavingFails() {
        CreateLoanApplicationCommand command = new CreateLoanApplicationCommand(
                "12345678",
                BigDecimal.valueOf(30_000),
                48,
                "Home renovation"
        );
        IllegalStateException failure = new IllegalStateException("Database unavailable");
        when(repository.save(any(LoanApplication.class))).thenThrow(failure);

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> service.create(command)
        );

        assertSame(failure, thrown);
        verify(publisher, never()).publishCreated(any());
    }
}
