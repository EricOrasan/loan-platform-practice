package com.btproject.loanplatform.customer_service.service;

import com.btproject.loanplatform.customer_service.domain.Customer;
import com.btproject.loanplatform.customer_service.dto.CreateCustomerRequest;
import com.btproject.loanplatform.customer_service.dto.CustomerResponse;
import com.btproject.loanplatform.customer_service.error.CustomerAlreadyExistsException;
import com.btproject.loanplatform.customer_service.error.CustomerNotFoundException;
import com.btproject.loanplatform.customer_service.mapper.CustomerMapper;
import com.btproject.loanplatform.customer_service.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByCif(String cif) {
        Customer customer = customerRepository.findByCif(cif)
                .orElseThrow(() -> new CustomerNotFoundException(cif));
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByCif(request.cif())) {
            throw CustomerAlreadyExistsException.forCif(request.cif());
        }

        String normalizedEmail = request.email().strip().toLowerCase(Locale.ROOT);
        if (customerRepository.existsByEmail(normalizedEmail)) {
            throw CustomerAlreadyExistsException.forEmail();
        }

        Customer customer = customerMapper.toEntity(request);
        customer.setEmail(normalizedEmail);

        Customer savedCustomer = customerRepository.save(customer);
        return customerMapper.toResponse(savedCustomer);
    }

    @Transactional
    public void deleteCustomerByCif(String cif) {
        Customer customer = customerRepository.findByCif(cif)
                .orElseThrow(() -> new CustomerNotFoundException(cif));
        customerRepository.delete(customer);
    }
}
