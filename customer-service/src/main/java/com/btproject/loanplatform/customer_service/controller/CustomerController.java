package com.btproject.loanplatform.customer_service.controller;

import com.btproject.loanplatform.customer_service.domain.Cif;
import com.btproject.loanplatform.customer_service.dto.CreateCustomerRequest;
import com.btproject.loanplatform.customer_service.dto.CustomerResponse;
import com.btproject.loanplatform.customer_service.service.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.createCustomer(request);
    }

    @GetMapping("/{cif}")
    public CustomerResponse getCustomer(
            @PathVariable
            @Pattern(
                    regexp = Cif.FORMAT_REGEX,
                    message = Cif.VALIDATION_MESSAGE
            )
            String cif
    ) {
        return customerService.getCustomerByCif(cif);
    }

    @DeleteMapping("/{cif}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(
            @PathVariable
            @Pattern(
                    regexp = Cif.FORMAT_REGEX,
                    message = Cif.VALIDATION_MESSAGE
            )
            String cif
    ) {
        customerService.deleteCustomerByCif(cif);
    }

}
