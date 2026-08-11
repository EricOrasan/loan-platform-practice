package com.btproject.loanplatform.offer_service.infrastructure.persistence;

import com.btproject.loanplatform.offer_service.application.port.out.LoanOfferRepository;
import com.btproject.loanplatform.offer_service.domain.LoanOffer;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class LoanOfferRepositoryAdapter implements LoanOfferRepository {

    private final SpringDataLoanOfferRepository repository;
    private final LoanOfferPersistenceMapper mapper;

    public LoanOfferRepositoryAdapter(SpringDataLoanOfferRepository repository, LoanOfferPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean existsByApplicationId(UUID applicationId) {
        return repository.existsByApplicationId(applicationId);
    }

    @Override
    public LoanOffer save(LoanOffer loanOffer) {
        LoanOfferJpaEntity entity = mapper.toJpaEntity(loanOffer);
        LoanOfferJpaEntity savedEntity = repository.save(entity);

        return mapper.toDomain(savedEntity);
    }
}
