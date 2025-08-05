package core.ms.order_book.web.mappers;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.web.dto.response.ApiResponse;
import core.ms.order_book.application.dto.query.MarketDepthDTO;
import core.ms.order_book.application.dto.query.MarketOverviewDTO;
import core.ms.order_book.application.dto.query.OrderBookOperationResultDTO;
import core.ms.order_book.application.dto.query.PriceLevelDTO;
import core.ms.order_book.domain.ports.inbound.OrderBookOperationResult;
import core.ms.order_book.domain.value_object.IPriceLevel;
import core.ms.order_book.domain.value_object.MarketDepth;
import core.ms.order_book.domain.value_object.MarketOverview;
import core.ms.order_book.infrastructure.events.dto.OrderMatchedEventDTO;
import core.ms.order_book.infrastructure.events.mappers.OrderMatchedEventMapper;
import core.ms.shared.money.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderBookWebMapper {

    @Autowired
    private OrderRepository orderRepository;

    // ===== SYMBOL CREATION =====

    public Symbol createSymbol(String symbolCode) {
        return switch (symbolCode.toUpperCase()) {
            case "BTC" -> Symbol.btcUsd();
            case "ETH" -> Symbol.ethUsd();
            case "EURUSD" -> Symbol.eurUsd();
            case "GBPUSD" -> Symbol.gbpUsd();
            default -> throw new IllegalArgumentException("Unsupported symbol: " + symbolCode);
        };
    }

    // ===== ORDER FETCHING =====

    public IOrder fetchOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    // ===== RESPONSE MAPPING =====

    public ApiResponse<String> toApiResponse(OrderBookOperationResult result) {
        if (result.isSuccess()) {
            return ApiResponse.success(result.getMessage(), result.getOrderId());
        } else {
            return ApiResponse.error(result.getMessage());
        }
    }

    // ===== DTO MAPPING =====

    public OrderBookOperationResultDTO toDTO(OrderBookOperationResult result) {
        List<OrderMatchedEventDTO> matchEventDtos = result.getMatchEvents().stream()
                .map(OrderMatchedEventMapper::toDto)
                .collect(Collectors.toList());

        return new OrderBookOperationResultDTO(
                result.isSuccess(),
                result.getMessage(),
                result.getOrderId(),
                result.getMatchCount(),
                matchEventDtos,
                result.getTimestamp()
        );
    }

    public MarketDepthDTO toDTO(MarketDepth marketDepth) {
        List<PriceLevelDTO> bidLevels = marketDepth.getBidLevels().stream()
                .map(this::toPriceLevelDTO)
                .collect(Collectors.toList());

        List<PriceLevelDTO> askLevels = marketDepth.getAskLevels().stream()
                .map(this::toPriceLevelDTO)
                .collect(Collectors.toList());

        return new MarketDepthDTO(
                marketDepth.getSymbol().getCode(),
                bidLevels,
                askLevels,
                marketDepth.getSpread() != null ? marketDepth.getSpread().getAmount() : null,
                marketDepth.getSpread() != null ? marketDepth.getSpread().getCurrency() : null,
                marketDepth.getTotalBidVolume(),
                marketDepth.getTotalAskVolume(),
                marketDepth.getTimestamp()
        );
    }

    public MarketOverviewDTO toDTO(MarketOverview overview) {
        List<String> symbolCodes = overview.getActiveSymbols().stream()
                .map(Symbol::getCode)
                .collect(Collectors.toList());

        Map<String, BigDecimal> volumeByCode = overview.getTotalVolume().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getCode(),
                        Map.Entry::getValue
                ));

        return new MarketOverviewDTO(
                symbolCodes,
                overview.getTotalOrderBooks(),
                overview.getTotalOrders(),
                volumeByCode,
                overview.getTimestamp()
        );
    }

    private PriceLevelDTO toPriceLevelDTO(IPriceLevel priceLevel) {
        return new PriceLevelDTO(
                priceLevel.getPrice().getAmount(),
                priceLevel.getPrice().getCurrency(),
                priceLevel.getTotalQuantity(),
                priceLevel.getOrderCount()
        );
    }
}