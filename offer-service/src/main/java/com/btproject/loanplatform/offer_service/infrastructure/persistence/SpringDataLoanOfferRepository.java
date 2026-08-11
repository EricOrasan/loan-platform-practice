package com.btproject.loanplatform.offer_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataLoanOfferRepository extends JpaRepository<LoanOfferJpaEntity, UUID> {

    boolean existsByApplicationId(UUID applicationId);
}
