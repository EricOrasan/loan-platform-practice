package com.btproject.loanplatform.offer_service.infrastructure.messaging.out;

import com.btproject.loanplatform.offer_service.application.port.out.LoanOfferEventPublisher;
import com.btproject.loanplatform.offer_service.domain.LoanOffer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaLoanOfferEventPublisher implements LoanOfferEventPublisher {

    private final KafkaTemplate<String, LoanOfferGeneratedEvent> kafkaTemplate;
    private final String topic;

    public KafkaLoanOfferEventPublisher(
            KafkaTemplate<String, LoanOfferGeneratedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.loan-offer-generated}")
            String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publishGenerated(LoanOffer loanOffer, String cif) {
        LoanOfferGeneratedEvent event = new LoanOfferGeneratedEvent(
                UUID.randomUUID(),
                "LOAN_OFFER_GENERATED",
                loanOffer.getApplicationId(),
                cif,
                loanOffer.getAmount(),
                loanOffer.getPeriodMonths(),
                loanOffer.getInterestRate(),
                loanOffer.getMonthlyInstallment(),
                loanOffer.getCreatedAt()
        );

        kafkaTemplate.send(
                topic,
                loanOffer.getApplicationId().toString(),
                event
        );
    }
}
