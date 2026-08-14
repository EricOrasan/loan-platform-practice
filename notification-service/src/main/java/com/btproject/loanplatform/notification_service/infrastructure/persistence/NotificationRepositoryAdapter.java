package com.btproject.loanplatform.notification_service.infrastructure.persistence;

import com.btproject.loanplatform.notification_service.application.port.out.NotificationRepository;
import com.btproject.loanplatform.notification_service.domain.Notification;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final SpringDataNotificationRepository repository;
    private final NotificationPersistenceMapper mapper;

    public NotificationRepositoryAdapter(SpringDataNotificationRepository repository, NotificationPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean existsByApplicationId(UUID applicationId) {
        return repository.existsByApplicationId(applicationId);
    }

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = mapper.toJpaEntity(notification);
        NotificationJpaEntity savedEntity = repository.save(entity);

        return mapper.toDomain(savedEntity);
    }
}
