package core.ms.order.domain.factories;

import core.ms.order.domain.entities.BuyOrder;
import core.ms.order.domain.entities.SellOrder;
import core.ms.order.domain.validators.OrderBuilderValidation;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import core.ms.utils.idgenerator.IdGen;

import java.math.BigDecimal;

/**
 * Domain factory for creating validated orders.
 * Encapsulates all business rules and validation logic within the domain.
 */
public class OrderFactory {

    private OrderFactory() {
        // Static factory - no instantiation
    }

    /**
     * Creates a validated buy order.
     * ID generation is implicit - client doesn't need to worry about it.
     *
     * @param portfolioId The portfolio ID for the order
     * @param reservationId The reservation ID for the order
     * @param symbol The trading symbol
     * @param price The order price
     * @param quantity The order quantity
     * @return A valid buy order
     * @throws OrderCreationException if validation fails
     */
    public static BuyOrder createBuyOrder(String portfolioId, String reservationId,
                                          Symbol symbol, Money price, BigDecimal quantity) {

        try {
            // Factory responsibility: Generate ID before validation
            String orderId = IdGen.generate("order");

            // All validation happens in the builder
            OrderBuilderValidation.OrderValidationResult validation =
                    OrderBuilderValidation
                            .builder()
                            .withId(orderId)                    // ← Factory-generated ID
                            .withPortfolioId(portfolioId)       // Portfolio context
                            .withReservationId(reservationId)   // Reservation context
                            .withSymbol(symbol)                 // Progressive validation
                            .withPrice(price)                   // Progressive validation
                            .withQuantity(quantity)             // Progressive validation
                            .build();                           // Final state

            // Pure entity creation using builder constructor
            return new BuyOrder(
                    validation.getId(),
                    validation.getPortfolioId(),
                    validation.getReservationId(),
                    validation.getSymbol(),
                    validation.getPrice(),
                    validation.getQuantity(),
                    validation.getStatus(),
                    validation.getCreatedAt(),
                    validation.getUpdatedAt(),
                    validation.getExecutedQuantity()
            );

        } catch (OrderBuilderValidation.ValidationOrderException e) {
            throw new OrderCreationException("Buy order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a validated sell order.
     * ID generation is implicit - client doesn't need to worry about it.
     *
     * @param portfolioId The portfolio ID for the order
     * @param reservationId The reservation ID for the order
     * @param symbol The trading symbol
     * @param price The order price
     * @param quantity The order quantity
     * @return A valid sell order
     * @throws OrderCreationException if validation fails
     */
    public static SellOrder createSellOrder(String portfolioId, String reservationId,
                                            Symbol symbol, Money price, BigDecimal quantity) {

        try {
            // Factory responsibility: Generate ID before validation
            String orderId = IdGen.generate("order");

            // All validation happens in the builder
            OrderBuilderValidation.OrderValidationResult validation =
                    OrderBuilderValidation
                            .builder()
                            .withId(orderId)                    // ← Factory-generated ID
                            .withPortfolioId(portfolioId)       // Portfolio context
                            .withReservationId(reservationId)   // Reservation context
                            .withSymbol(symbol)                 // Progressive validation
                            .withPrice(price)                   // Progressive validation
                            .withQuantity(quantity)             // Progressive validation
                            .build();                           // Final state

            // Pure entity creation using builder constructor
            return new SellOrder(
                    validation.getId(),
                    validation.getPortfolioId(),
                    validation.getReservationId(),
                    validation.getSymbol(),
                    validation.getPrice(),
                    validation.getQuantity(),
                    validation.getStatus(),
                    validation.getCreatedAt(),
                    validation.getUpdatedAt(),
                    validation.getExecutedQuantity()
            );

        } catch (OrderBuilderValidation.ValidationOrderException e) {
            throw new OrderCreationException("Sell order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a buy order with custom ID (for testing or special cases).
     */
    public static BuyOrder createBuyOrderWithId(String id, String portfolioId, String reservationId,
                                                Symbol symbol, Money price, BigDecimal quantity) {

        try {
            OrderBuilderValidation.OrderValidationResult validation =
                    OrderBuilderValidation
                            .builder()
                            .withId(id)                         // Custom ID
                            .withPortfolioId(portfolioId)       // Portfolio context
                            .withReservationId(reservationId)   // Reservation context
                            .withSymbol(symbol)
                            .withPrice(price)
                            .withQuantity(quantity)
                            .build();

            return new BuyOrder(
                    validation.getId(),
                    validation.getPortfolioId(),
                    validation.getReservationId(),
                    validation.getSymbol(),
                    validation.getPrice(),
                    validation.getQuantity(),
                    validation.getStatus(),
                    validation.getCreatedAt(),
                    validation.getUpdatedAt(),
                    validation.getExecutedQuantity()
            );

        } catch (OrderBuilderValidation.ValidationOrderException e) {
            throw new OrderCreationException("Buy order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a sell order with custom ID (for testing or special cases).
     */
    public static SellOrder createSellOrderWithId(String id, String portfolioId, String reservationId,
                                                  Symbol symbol, Money price, BigDecimal quantity) {

        try {
            OrderBuilderValidation.OrderValidationResult validation =
                    OrderBuilderValidation
                            .builder()
                            .withId(id)                         // Custom ID
                            .withPortfolioId(portfolioId)       // Portfolio context
                            .withReservationId(reservationId)   // Reservation context
                            .withSymbol(symbol)
                            .withPrice(price)
                            .withQuantity(quantity)
                            .build();

            return new SellOrder(
                    validation.getId(),
                    validation.getPortfolioId(),
                    validation.getReservationId(),
                    validation.getSymbol(),
                    validation.getPrice(),
                    validation.getQuantity(),
                    validation.getStatus(),
                    validation.getCreatedAt(),
                    validation.getUpdatedAt(),
                    validation.getExecutedQuantity()
            );

        } catch (OrderBuilderValidation.ValidationOrderException e) {
            throw new OrderCreationException("Sell order creation failed: " + e.getMessage(), e);
        }
    }

    // ===== CONVENIENCE METHODS FOR BACKWARD COMPATIBILITY =====

    /**
     * Creates a buy order without portfolio/reservation context (backward compatibility).
     * Uses default values for portfolioId and reservationId.
     */
    public static BuyOrder createBuyOrder(Symbol symbol, Money price, BigDecimal quantity) {
        return createBuyOrder(null, null, symbol, price, quantity);
    }

    /**
     * Creates a sell order without portfolio/reservation context (backward compatibility).
     * Uses default values for portfolioId and reservationId.
     */
    public static SellOrder createSellOrder(Symbol symbol, Money price, BigDecimal quantity) {
        return createSellOrder(null, null, symbol, price, quantity);
    }

    /**
     * Creates a buy order with custom ID without portfolio/reservation context (backward compatibility).
     */
    public static BuyOrder createBuyOrderWithId(String id, Symbol symbol, Money price, BigDecimal quantity) {
        return createBuyOrderWithId(id, null, null, symbol, price, quantity);
    }

    /**
     * Creates a sell order with custom ID without portfolio/reservation context (backward compatibility).
     */
    public static SellOrder createSellOrderWithId(String id, Symbol symbol, Money price, BigDecimal quantity) {
        return createSellOrderWithId(id, null, null, symbol, price, quantity);
    }

    // ===== DOMAIN EXCEPTION =====

    /**
     * Domain exception for order creation failures.
     */
    public static class OrderCreationException extends RuntimeException {

        public OrderCreationException(String message) {
            super(message);
        }

        public OrderCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}