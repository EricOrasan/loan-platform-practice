package com.btproject.loanplatform.customer_service.service;

import com.btproject.loanplatform.customer_service.dto.CreateCustomerRequest;
import com.btproject.loanplatform.customer_service.dto.CustomerResponse;

public interface CustomerService {
    CustomerResponse getCustomerByCif(String cif);
    CustomerResponse createCustomer(CreateCustomerRequest request);
    void deleteCustomerByCif(String cif);
}
