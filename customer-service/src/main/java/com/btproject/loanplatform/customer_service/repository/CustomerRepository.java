package com.btproject.loanplatform.customer_service.repository;

import com.btproject.loanplatform.customer_service.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByCif(String cif);
    boolean existsByCif(String cif);
    boolean existsByEmail(String email);
}
