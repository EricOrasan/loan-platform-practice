package com.btproject.loanplatform.audit_service.application.port.in;

import com.btproject.loanplatform.audit_service.application.command.RecordAuditEventCommand;

public interface RecordAuditEventUseCase {

    void record(RecordAuditEventCommand command);
}
