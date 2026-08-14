package com.btproject.loanplatform.customer_service.service;

import com.btproject.loanplatform.customer_service.domain.Customer;
import com.btproject.loanplatform.customer_service.domain.RiskCategory;
import com.btproject.loanplatform.customer_service.dto.CreateCustomerRequest;
import com.btproject.loanplatform.customer_service.dto.CustomerResponse;
import com.btproject.loanplatform.customer_service.error.CustomerAlreadyExistsException;
import com.btproject.loanplatform.customer_service.error.CustomerNotFoundException;
import com.btproject.loanplatform.customer_service.mapper.CustomerMapper;
import com.btproject.loanplatform.customer_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final String CIF = "12345678";
    private static final String NORMALIZED_EMAIL = "andrei.popescu@example.com";

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerMapper mapper;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(repository, mapper);
    }

    @Test
    void shouldReturnCustomerByCif() {
        Customer customer = customer();
        CustomerResponse response = response();
        when(repository.findByCif(CIF)).thenReturn(Optional.of(customer));
        when(mapper.toResponse(customer)).thenReturn(response);

        CustomerResponse result = service.getCustomerByCif(CIF);

        assertSame(response, result);
        verify(mapper).toResponse(customer);
    }

    @Test
    void shouldThrowWhenGettingUnknownCustomer() {
        when(repository.findByCif(CIF)).thenReturn(Optional.empty());

        CustomerNotFoundException exception = assertThrows(
                CustomerNotFoundException.class,
                () -> service.getCustomerByCif(CIF)
        );

        assertEquals("No customer exists for CIF " + CIF + ".", exception.getMessage());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldCreateCustomerWithNormalizedEmail() {
        CreateCustomerRequest request = request("ANDREI.POPESCU@EXAMPLE.COM");
        Customer customer = customer();
        customer.setEmail(request.email());
        CustomerResponse response = response();

        when(repository.existsByCif(CIF)).thenReturn(false);
        when(repository.existsByEmail(NORMALIZED_EMAIL)).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(customer);
        when(repository.save(customer)).thenReturn(customer);
        when(mapper.toResponse(customer)).thenReturn(response);

        CustomerResponse result = service.createCustomer(request);

        assertSame(response, result);
        assertEquals(NORMALIZED_EMAIL, customer.getEmail());
        verify(repository).existsByEmail(NORMALIZED_EMAIL);
        verify(repository).save(customer);
        verify(mapper).toResponse(customer);
    }

    @Test
    void shouldRejectDuplicateCifWithoutCheckingEmailOrSaving() {
        CreateCustomerRequest request = request(NORMALIZED_EMAIL);
        when(repository.existsByCif(CIF)).thenReturn(true);

        CustomerAlreadyExistsException exception = assertThrows(
                CustomerAlreadyExistsException.class,
                () -> service.createCustomer(request)
        );

        assertEquals("A customer already exists for CIF " + CIF + ".", exception.getMessage());
        verify(repository, never()).existsByEmail(anyString());
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldRejectDuplicateNormalizedEmailWithoutSaving() {
        CreateCustomerRequest request = request("ANDREI.POPESCU@EXAMPLE.COM");
        when(repository.existsByCif(CIF)).thenReturn(false);
        when(repository.existsByEmail(NORMALIZED_EMAIL)).thenReturn(true);

        CustomerAlreadyExistsException exception = assertThrows(
                CustomerAlreadyExistsException.class,
                () -> service.createCustomer(request)
        );

        assertEquals(
                "A customer already exists with the provided email address.",
                exception.getMessage()
        );
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldDeleteExistingCustomer() {
        Customer customer = customer();
        when(repository.findByCif(CIF)).thenReturn(Optional.of(customer));

        service.deleteCustomerByCif(CIF);

        verify(repository).delete(customer);
    }

    @Test
    void shouldThrowWhenDeletingUnknownCustomer() {
        when(repository.findByCif(CIF)).thenReturn(Optional.empty());

        CustomerNotFoundException exception = assertThrows(
                CustomerNotFoundException.class,
                () -> service.deleteCustomerByCif(CIF)
        );

        assertEquals("No customer exists for CIF " + CIF + ".", exception.getMessage());
        verify(repository, never()).delete(any());
    }

    private static CreateCustomerRequest request(String email) {
        return new CreateCustomerRequest(
                CIF,
                "Andrei",
                "Popescu",
                email,
                BigDecimal.valueOf(8_000),
                RiskCategory.LOW
        );
    }

    private static Customer customer() {
        Customer customer = new Customer();
        customer.setCif(CIF);
        customer.setFirstName("Andrei");
        customer.setLastName("Popescu");
        customer.setEmail(NORMALIZED_EMAIL);
        customer.setMonthlyIncome(BigDecimal.valueOf(8_000));
        customer.setRiskCategory(RiskCategory.LOW);
        return customer;
    }

    private static CustomerResponse response() {
        Instant createdAt = Instant.parse("2026-08-14T10:00:00Z");
        return new CustomerResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                CIF,
                "Andrei",
                "Popescu",
                NORMALIZED_EMAIL,
                BigDecimal.valueOf(8_000),
                RiskCategory.LOW,
                createdAt,
                createdAt
        );
    }
}
