package core.ms.order.web.mappers;

import core.ms.order.application.dto.command.*;
import core.ms.order.application.dto.query.OrderOperationResultDTO;

import core.ms.order.web.dto.request.CancelPartialOrderRequest;
import core.ms.order.web.dto.request.CreateBuyOrderRequest;
import core.ms.order.web.dto.request.CreateSellOrderRequest;
import core.ms.order.web.dto.request.UpdateOrderPriceRequest;
import core.ms.order.web.dto.response.ApiResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderWebMapper {

    // ===== REQUEST TO COMMAND MAPPING =====

    public CreateBuyOrderCommand toCommand(CreateBuyOrderRequest request) {
        return new CreateBuyOrderCommand(
                request.getPortfolioId(),
                request.getReservationId(),
                request.getSymbolCode(),
                request.getPrice(),
                request.getCurrency(),
                request.getQuantity()
        );
    }

    public CreateSellOrderCommand toCommand(CreateSellOrderRequest request) {
        return new CreateSellOrderCommand(
                request.getPortfolioId(),
                request.getReservationId(),
                request.getSymbolCode(),
                request.getPrice(),
                request.getCurrency(),
                request.getQuantity()
        );
    }

    public UpdateOrderPriceCommand toCommand(String orderId, UpdateOrderPriceRequest request) {
        return new UpdateOrderPriceCommand(
                orderId,
                request.getNewPrice(),
                request.getCurrency()
        );
    }

    public CancelOrderCommand toCommand(String orderId) {
        return new CancelOrderCommand(orderId);
    }

    public CancelPartialOrderCommand toCommand(String orderId, CancelPartialOrderRequest request) {
        return new CancelPartialOrderCommand(
                orderId,
                request.getQuantityToCancel()
        );
    }

    // ===== RESPONSE MAPPING =====

    public ApiResponse<String> toApiResponse(OrderOperationResultDTO result) {
        if (result.isSuccess()) {
            return ApiResponse.success(result.getMessage(), result.getOrderId());
        } else {
            return ApiResponse.error(result.getMessage(), result.getErrors());
        }
    }
}