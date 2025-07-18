package core.ms.order;

import core.ms.order.domain.entities.BuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Buy Order Tests")
class BuyOrderTest {

    private BuyOrder buyOrder;
    private Money price;
    private BigDecimal quantity;

    @BeforeEach
    void setUp() {
        Symbol btcUsd = Symbol.btcUsd();
        price = Money.of("45000", Currency.USD);
        quantity = new BigDecimal("0.5");
        buyOrder = new BuyOrder("buy-order-1", btcUsd, price, quantity);
    }

    @Test
    @DisplayName("Should calculate cost basis correctly")
    void shouldCalculateCostBasisCorrectly() {
        Money expectedCostBasis = Money.of("22500.00", Currency.USD);
        assertEquals(expectedCostBasis, buyOrder.getCostBasis());
    }

    @Test
    @DisplayName("Should implement IBuyOrder interface")
    void shouldImplementIBuyOrderInterface() {
        assertTrue(buyOrder instanceof IOrder);
    }
}
