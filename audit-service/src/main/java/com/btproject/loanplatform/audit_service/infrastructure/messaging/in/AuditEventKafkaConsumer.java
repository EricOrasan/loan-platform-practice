package com.btproject.loanplatform.audit_service.infrastructure.messaging.in;

import com.btproject.loanplatform.audit_service.application.command.RecordAuditEventCommand;
import com.btproject.loanplatform.audit_service.application.port.in.RecordAuditEventUseCase;
import com.btproject.loanplatform.audit_service.domain.AuditEventType;
import com.btproject.loanplatform.audit_service.infrastructure.messaging.in.exception.InvalidAuditEventException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
public class AuditEventKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final RecordAuditEventUseCase recordAuditEventUseCase;

    public AuditEventKafkaConsumer(ObjectMapper objectMapper, RecordAuditEventUseCase recordAuditEventUseCase) {
        this.objectMapper = objectMapper;
        this.recordAuditEventUseCase = recordAuditEventUseCase;
    }

    @KafkaListener(topics = {
            "${app.kafka.topics.loan-application-created}",
            "${app.kafka.topics.loan-assessment-completed}",
            "${app.kafka.topics.loan-offer-generated}"
    })
    public void consume(String payload) {
        JsonNode event = parsePayload(payload);

        UUID eventId = requiredUuid(event, "eventId");
        AuditEventType eventType = requiredEventType(event);
        UUID aggregateId = requiredUuid(event, "applicationId");

        recordAuditEventUseCase.record(new RecordAuditEventCommand(
                eventId,
                eventType,
                aggregateId,
                payload
        ));
    }

    private JsonNode parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new InvalidAuditEventException("payload must not be blank");
        }

        try {
            JsonNode event = objectMapper.readTree(payload);

            if (event == null || !event.isObject()) {
                throw new InvalidAuditEventException("payload must be a JSON object");
            }

            return event;
        } catch (JacksonException exception) {
            throw new InvalidAuditEventException("payload must contain valid JSON", exception);
        }
    }

    private AuditEventType requiredEventType(JsonNode event) {
        String value = requiredText(event, "eventType");

        try {
            return AuditEventType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidAuditEventException("eventType is not supported", exception);
        }
    }

    private UUID requiredUuid(JsonNode event, String fieldName) {
        String value = requiredText(event, fieldName);

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidAuditEventException(fieldName + " must be a valid UUID", exception);
        }
    }

    private String requiredText(JsonNode event, String fieldName) {
        JsonNode value = event.get(fieldName);

        if (value == null || !value.isString() || value.stringValue().isBlank()) {
            throw new InvalidAuditEventException(fieldName + " must not be blank");
        }

        return value.stringValue();
    }
}
