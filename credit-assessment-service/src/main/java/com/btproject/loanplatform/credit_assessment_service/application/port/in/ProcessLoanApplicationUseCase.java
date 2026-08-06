package com.btproject.loanplatform.credit_assessment_service.application.port.in;

import com.btproject.loanplatform.credit_assessment_service.application.command.ProcessLoanApplicationCommand;

public interface ProcessLoanApplicationUseCase {
    void process(ProcessLoanApplicationCommand command);
}
