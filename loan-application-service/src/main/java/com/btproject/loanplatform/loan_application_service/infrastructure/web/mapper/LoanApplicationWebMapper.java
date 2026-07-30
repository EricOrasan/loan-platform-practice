package com.btproject.loanplatform.loan_application_service.infrastructure.web.mapper;

import com.btproject.loanplatform.loan_application_service.application.command.CreateLoanApplicationCommand;
import com.btproject.loanplatform.loan_application_service.application.command.UpdateLoanApplicationCommand;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import com.btproject.loanplatform.loan_application_service.infrastructure.web.dto.CreateLoanApplicationRequest;
import com.btproject.loanplatform.loan_application_service.infrastructure.web.dto.LoanApplicationResponse;
import com.btproject.loanplatform.loan_application_service.infrastructure.web.dto.UpdateLoanApplicationRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoanApplicationWebMapper {

    CreateLoanApplicationCommand toCreateCommand(CreateLoanApplicationRequest request);
    UpdateLoanApplicationCommand toUpdateCommand(UpdateLoanApplicationRequest request);
    LoanApplicationResponse toResponse(LoanApplication loanApplication);
}
