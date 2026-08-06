package com.btproject.loanplatform.credit_assessment_service.application.port.out;

import com.btproject.loanplatform.credit_assessment_service.domain.CustomerFinancialProfile;

import java.util.Optional;

public interface CustomerInformationPort {
    Optional<CustomerFinancialProfile> findByCif(String cif);
}