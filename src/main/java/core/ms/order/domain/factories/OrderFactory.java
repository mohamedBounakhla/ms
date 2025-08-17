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
     * portfolioId and reservationId are MANDATORY.
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
                            .withId(orderId)
                            .withPortfolioId(portfolioId)
                            .withReservationId(reservationId)
                            .withSymbol(symbol)
                            .withPrice(price)
                            .withQuantity(quantity)
                            .build();

            // Pure entity creation using single constructor
            return new BuyOrder(
                    validation.getId(),
                    validation.getPortfolioId(),
                    validation.getReservationId(),
                    validation.getSymbol(),
                    validation.getPrice(),
                    validation.getQuantity()
            );

        } catch (OrderBuilderValidation.ValidationOrderException e) {
            throw new OrderCreationException("Buy order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a validated sell order.
     * portfolioId and reservationId are MANDATORY.
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
                            .withId(orderId)
                            .withPortfolioId(portfolioId)
                            .withReservationId(reservationId)
                            .withSymbol(symbol)
                            .withPrice(price)
                            .withQuantity(quantity)
                            .build();

            // Pure entity creation using single constructor
            return new SellOrder(
                    validation.getId(),
                    validation.getPortfolioId(),
                    validation.getReservationId(),
                    validation.getSymbol(),
                    validation.getPrice(),
                    validation.getQuantity()
            );

        } catch (OrderBuilderValidation.ValidationOrderException e) {
            throw new OrderCreationException("Sell order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a buy order with custom ID (for reconstruction from persistence).
     * portfolioId and reservationId are MANDATORY.
     */
    public static BuyOrder createBuyOrderWithId(String id, String portfolioId, String reservationId,
                                                Symbol symbol, Money price, BigDecimal quantity) {

        try {
            OrderBuilderValidation.OrderValidationResult validation =
                    OrderBuilderValidation
                            .builder()
                            .withId(id)
                            .withPortfolioId(portfolioId)
                            .withReservationId(reservationId)
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
                    validation.getQuantity()
            );

        } catch (OrderBuilderValidation.ValidationOrderException e) {
            throw new OrderCreationException("Buy order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a sell order with custom ID (for reconstruction from persistence).
     * portfolioId and reservationId are MANDATORY.
     */
    public static SellOrder createSellOrderWithId(String id, String portfolioId, String reservationId,
                                                  Symbol symbol, Money price, BigDecimal quantity) {

        try {
            OrderBuilderValidation.OrderValidationResult validation =
                    OrderBuilderValidation
                            .builder()
                            .withId(id)
                            .withPortfolioId(portfolioId)
                            .withReservationId(reservationId)
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
                    validation.getQuantity()
            );

        } catch (OrderBuilderValidation.ValidationOrderException e) {
            throw new OrderCreationException("Sell order creation failed: " + e.getMessage(), e);
        }
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