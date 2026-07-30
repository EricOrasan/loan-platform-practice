package com.btproject.loanplatform.loan_application_service.infrastructure.web.controller;

import com.btproject.loanplatform.loan_application_service.application.command.CreateLoanApplicationCommand;
import com.btproject.loanplatform.loan_application_service.application.command.UpdateLoanApplicationCommand;
import com.btproject.loanplatform.loan_application_service.application.port.in.CreateLoanApplicationUseCase;
import com.btproject.loanplatform.loan_application_service.application.port.in.DeleteLoanApplicationUseCase;
import com.btproject.loanplatform.loan_application_service.application.port.in.GetLoanApplicationUseCase;
import com.btproject.loanplatform.loan_application_service.application.port.in.UpdateLoanApplicationUseCase;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import com.btproject.loanplatform.loan_application_service.infrastructure.web.dto.CreateLoanApplicationRequest;
import com.btproject.loanplatform.loan_application_service.infrastructure.web.dto.LoanApplicationResponse;
import com.btproject.loanplatform.loan_application_service.infrastructure.web.dto.UpdateLoanApplicationRequest;
import com.btproject.loanplatform.loan_application_service.infrastructure.web.mapper.LoanApplicationWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/applications")
public class LoanApplicationController {

    private final CreateLoanApplicationUseCase createLoanApplicationUseCase;
    private final UpdateLoanApplicationUseCase updateLoanApplicationUseCase;
    private final GetLoanApplicationUseCase getLoanApplicationUseCase;
    private final DeleteLoanApplicationUseCase deleteLoanApplicationUseCase;

    private final LoanApplicationWebMapper loanApplicationWebMapper;

    public LoanApplicationController(CreateLoanApplicationUseCase createLoanApplicationUseCase,
                                     UpdateLoanApplicationUseCase updateLoanApplicationUseCase,
                                     GetLoanApplicationUseCase getLoanApplicationUseCase,
                                     DeleteLoanApplicationUseCase deleteLoanApplicationUseCase,
                                     LoanApplicationWebMapper loanApplicationWebMapper) {
        this.createLoanApplicationUseCase = createLoanApplicationUseCase;
        this.updateLoanApplicationUseCase = updateLoanApplicationUseCase;
        this.getLoanApplicationUseCase = getLoanApplicationUseCase;
        this.deleteLoanApplicationUseCase = deleteLoanApplicationUseCase;
        this.loanApplicationWebMapper = loanApplicationWebMapper;
    }

    @GetMapping("/{id}")
    public LoanApplicationResponse getLoanApplication(@PathVariable UUID id) {
        LoanApplication loanApplication = getLoanApplicationUseCase.get(id);
        return loanApplicationWebMapper.toResponse(loanApplication);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LoanApplicationResponse createLoanApplication(@Valid @RequestBody CreateLoanApplicationRequest request) {
        CreateLoanApplicationCommand command = loanApplicationWebMapper.toCreateCommand(request);
        LoanApplication loanApplication = createLoanApplicationUseCase.create(command);
        return loanApplicationWebMapper.toResponse(loanApplication);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoanApplicationResponse updateLoanApplication(@PathVariable UUID id, @Valid @RequestBody UpdateLoanApplicationRequest request) {
        UpdateLoanApplicationCommand command = loanApplicationWebMapper.toUpdateCommand(request);
        LoanApplication loanApplication = updateLoanApplicationUseCase.update(id, command);
        return loanApplicationWebMapper.toResponse(loanApplication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLoanApplication(@PathVariable UUID id) {
        deleteLoanApplicationUseCase.delete(id);
    }
}
