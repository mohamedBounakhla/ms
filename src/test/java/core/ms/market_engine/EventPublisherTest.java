package core.ms.market_engine;

import core.ms.market_engine.event.OrderAcceptedEvent;
import core.ms.market_engine.event.OrderExecutedEvent;
import core.ms.market_engine.event.TransactionCreatedEvent;
import core.ms.order.domain.entities.*;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EventPublisherTest {

    private EventPublisher eventPublisher;
    private ByteArrayOutputStream outputCapture;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        eventPublisher = new EventPublisher();

        // Capture System.out for testing console output
        outputCapture = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputCapture));
    }

    @BeforeEach
    void tearDown() {
        // Restore original System.out
        if (originalOut != null) {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("Should publish order accepted event")
    void shouldPublishOrderAcceptedEvent() {
        // Given
        Symbol symbol = Symbol.eurUsd();
        Money price = Money.of("1.2000", Currency.USD);
        IBuyOrder order = new BuyOrder("order-123", symbol, price, new BigDecimal("100"));
        OrderAcceptedEvent event = new OrderAcceptedEvent(order, "engine-1");

        // When
        eventPublisher.publishOrderAccepted(event);

        // Then
        String output = outputCapture.toString();
        assertTrue(output.contains("ORDER_ACCEPTED"));
        assertTrue(output.contains("Order order-123 accepted"));
        assertTrue(output.contains("symbol EURUSD"));
    }

    @Test
    @DisplayName("Should publish transaction created event")
    void shouldPublishTransactionCreatedEvent() {
        // Given
        Symbol symbol = Symbol.eurUsd();
        Money price = Money.of("1.2000", Currency.USD);
        IBuyOrder buyOrder = new BuyOrder("buy-1", symbol, price, new BigDecimal("50"));
        ISellOrder sellOrder = new SellOrder("sell-1", symbol, price, new BigDecimal("50"));

        ITransaction transaction = new Transaction("tx-123", symbol, buyOrder, sellOrder, price, new BigDecimal("50"));
        TransactionCreatedEvent event = new TransactionCreatedEvent(transaction, "engine-1");

        // When
        eventPublisher.publishTransactionCreated(event);

        // Then
        String output = outputCapture.toString();
        assertTrue(output.contains("TRANSACTION_CREATED"));
        assertTrue(output.contains("Transaction tx-123 created"));
        assertTrue(output.contains("50 EURUSD"));
        assertTrue(output.contains("1.2000"));
    }

    @Test
    @DisplayName("Should publish order executed event")
    void shouldPublishOrderExecutedEvent() {
        // Given
        Money executionPrice = Money.of("1.2000", Currency.USD);
        OrderExecutedEvent event = new OrderExecutedEvent(
                "order-456",
                new BigDecimal("30"),
                new BigDecimal("20"),
                executionPrice,
                "engine-1"
        );

        // When
        eventPublisher.publishOrderExecuted(event);

        // Then
        String output = outputCapture.toString();
        assertTrue(output.contains("ORDER_EXECUTED"));
        assertTrue(output.contains("Order order-456 executed"));
        assertTrue(output.contains("30"));
        assertTrue(output.contains("1.2000"));
        assertTrue(output.contains("remaining: 20"));
    }

    @Test
    @DisplayName("Should throw exception when publishing null order accepted event")
    void shouldThrowExceptionWhenPublishingNullOrderAcceptedEvent() {
        // When & Then
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> eventPublisher.publishOrderAccepted(null)
        );
        assertEquals("Event cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when publishing null transaction created event")
    void shouldThrowExceptionWhenPublishingNullTransactionCreatedEvent() {
        // When & Then
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> eventPublisher.publishTransactionCreated(null)
        );
        assertEquals("Event cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when publishing null order executed event")
    void shouldThrowExceptionWhenPublishingNullOrderExecutedEvent() {
        // When & Then
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> eventPublisher.publishOrderExecuted(null)
        );
        assertEquals("Event cannot be null", exception.getMessage());
    }

    /*@Test
    @DisplayName("Should log events with timestamp")
    void shouldLogEventsWithTimestamp() {
        // Given
        Symbol symbol = Symbol.eurUsd();
        Money price = Money.of("1.2000", Currency.USD);
        IBuyOrder order = new BuyOrder("order-time", symbol, price, new BigDecimal("100"));
        OrderAcceptedEvent event = new OrderAcceptedEvent(order, "engine-1");

        // When
        eventPublisher.publishOrderAccepted(event);

        // Then
        String output = outputCapture.toString();
        assertTrue(output.matches(".*\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?\\].*"));
    }*/

    @Test
    @DisplayName("Should handle multiple event publications")
    void shouldHandleMultipleEventPublications() {
        // Given
        Symbol symbol = Symbol.eurUsd();
        Money price = Money.of("1.2000", Currency.USD);

        IBuyOrder order1 = new BuyOrder("order-1", symbol, price, new BigDecimal("100"));
        IBuyOrder order2 = new BuyOrder("order-2", symbol, price, new BigDecimal("200"));

        OrderAcceptedEvent event1 = new OrderAcceptedEvent(order1, "engine-1");
        OrderAcceptedEvent event2 = new OrderAcceptedEvent(order2, "engine-1");

        // When
        eventPublisher.publishOrderAccepted(event1);
        eventPublisher.publishOrderAccepted(event2);

        // Then
        String output = outputCapture.toString();
        assertTrue(output.contains("order-1"));
        assertTrue(output.contains("order-2"));

        // Count occurrences of ORDER_ACCEPTED
        long acceptedCount = output.lines()
                .filter(line -> line.contains("ORDER_ACCEPTED"))
                .count();
        assertEquals(2, acceptedCount);
    }

    @Test
    @DisplayName("Should handle order executed event with zero remaining quantity")
    void shouldHandleOrderExecutedEventWithZeroRemainingQuantity() {
        // Given
        Money executionPrice = Money.of("1.2000", Currency.USD);
        OrderExecutedEvent event = new OrderExecutedEvent(
                "fully-filled-order",
                new BigDecimal("100"),
                BigDecimal.ZERO,
                executionPrice,
                "engine-1"
        );

        // When
        eventPublisher.publishOrderExecuted(event);

        // Then
        String output = outputCapture.toString();
        assertTrue(output.contains("fully-filled-order"));
        assertTrue(output.contains("remaining: 0"));
    }

    @Test
    @DisplayName("Should handle transaction with different currencies")
    void shouldHandleTransactionWithDifferentCurrencies() {
        // Given
        Symbol btcSymbol = Symbol.btcUsd();
        Money btcPrice = Money.of("50000.00", Currency.USD);
        IBuyOrder buyOrder = new BuyOrder("btc-buy", btcSymbol, btcPrice, new BigDecimal("0.5"));
        ISellOrder sellOrder = new SellOrder("btc-sell", btcSymbol, btcPrice, new BigDecimal("0.5"));

        ITransaction transaction = new Transaction("btc-tx", btcSymbol, buyOrder, sellOrder, btcPrice, new BigDecimal("0.5"));
        TransactionCreatedEvent event = new TransactionCreatedEvent(transaction, "engine-1");

        // When
        eventPublisher.publishTransactionCreated(event);

        // Then
        String output = outputCapture.toString();
        assertTrue(output.contains("0.5 BTC"));
        assertTrue(output.contains("50000.00"));
    }
}