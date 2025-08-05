package core.ms.order_book.infrastructure.events.mappers;

import core.ms.order_book.domain.events.OrderMatchedEvent;
import core.ms.order_book.infrastructure.events.dto.OrderMatchedEventDTO;
import core.ms.order_book.infrastructure.events.dto.OrderMatchedEventMessage;

import java.util.List;
import java.util.stream.Collectors;

public class OrderMatchedEventMapper {

    /**
     * Maps domain event to DTO (for web layer).
     */
    public static OrderMatchedEventDTO toDto(OrderMatchedEvent domainEvent) {
        return new OrderMatchedEventDTO(
                domainEvent.getBuyOrderId(),
                domainEvent.getSellOrderId(),
                domainEvent.getSymbol(),
                domainEvent.getQuantity(),
                domainEvent.getExecutionPrice().getAmount(),
                domainEvent.getExecutionPrice().getCurrency(),
                domainEvent.getTotalValue().getAmount(),
                domainEvent.getOccurredAt()
        );
    }

    /**
     * Maps domain event to infrastructure event message.
     */
    public static OrderMatchedEventMessage toEventMessage(OrderMatchedEvent domainEvent) {
        return new OrderMatchedEventMessage(
                domainEvent.getBuyOrderId(),
                domainEvent.getSellOrderId(),
                domainEvent.getSymbol().getCode(),
                domainEvent.getQuantity(),
                domainEvent.getExecutionPrice().getAmount(),
                domainEvent.getExecutionPrice().getCurrency(),
                domainEvent.getOccurredAt()
        );
    }

    /**
     * Maps list of domain events to event messages.
     */
    public static List<OrderMatchedEventMessage> toEventMessages(List<OrderMatchedEvent> domainEvents) {
        return domainEvents.stream()
                .map(OrderMatchedEventMapper::toEventMessage)
                .collect(Collectors.toList());
    }

    public static List<OrderMatchedEventDTO> toDtoList(List<OrderMatchedEvent> domainEvents) {
        return domainEvents.stream()
                .map(OrderMatchedEventMapper::toDto)
                .collect(Collectors.toList());
    }
}