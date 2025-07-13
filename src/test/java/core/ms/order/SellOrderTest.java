package core.ms.order;

import core.ms.order.domain.IOrder;
import core.ms.order.domain.ISellOrder;
import core.ms.order.domain.SellOrder;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Sell Order Tests")
class SellOrderTest {

    private SellOrder sellOrder;
    private Money price;
    private BigDecimal quantity;

    @BeforeEach
    void setUp() {
        Symbol btcUsd = Symbol.btcUsd();
        price = Money.of("45000", Currency.USD);
        quantity = new BigDecimal("0.5");
        sellOrder = new SellOrder("sell-order-1", btcUsd, price, quantity);
    }

    @Test
    @DisplayName("Should calculate proceeds correctly")
    void shouldCalculateProceedsCorrectly() {
        Money expectedProceeds = Money.of("22500.00", Currency.USD);
        assertEquals(expectedProceeds, sellOrder.getProceeds());
    }


}