package dev.piovra.order.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.piovra.common.ErrorClass;
import dev.piovra.common.PiovraException;
import dev.piovra.model.order.CanonicalOrder;

@Component
public class OrderEntityMapper {

    private final ObjectMapper objectMapper;

    public OrderEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OrderEntity toNewEntity(CanonicalOrder order) {
        return new OrderEntity(
                order.orderId(),
                order.tenantId().value(),
                order.channelId().value(),
                order.channelOrderId(),
                order.status().name(),
                write(order));
    }

    public void applyTo(OrderEntity entity, CanonicalOrder order) {
        entity.update(order.status().name(), write(order));
    }

    public CanonicalOrder toDomain(OrderEntity entity) {
        return read(entity.payload());
    }

    private String write(CanonicalOrder order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            throw new PiovraException(ErrorClass.INTERNAL, "ORDER_SERIALIZATION", "cannot serialize order", e);
        }
    }

    private CanonicalOrder read(String payload) {
        try {
            return objectMapper.readValue(payload, CanonicalOrder.class);
        } catch (JsonProcessingException e) {
            throw new PiovraException(ErrorClass.INTERNAL, "ORDER_DESERIALIZATION", "cannot deserialize order", e);
        }
    }
}
