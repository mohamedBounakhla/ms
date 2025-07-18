package core.ms.order.web.mappers;

import core.ms.order.application.dto.command.CreateTransactionCommand;
import core.ms.order.application.dto.query.TransactionResultDTO;
import core.ms.order.web.dto.request.CreateTransactionRequest;
import core.ms.order.web.dto.response.ApiResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionWebMapper {

    // ===== REQUEST TO COMMAND MAPPING =====

    public CreateTransactionCommand toCommand(CreateTransactionRequest request) {
        return new CreateTransactionCommand(
                request.getBuyOrderId(),
                request.getSellOrderId(),
                request.getExecutionPrice(),
                request.getCurrency(),
                request.getQuantity()
        );
    }

    // ===== RESPONSE MAPPING =====

    public ApiResponse<String> toApiResponse(TransactionResultDTO result) {
        if (result.isSuccess()) {
            return ApiResponse.success(result.getMessage(), result.getTransactionId());
        } else {
            return ApiResponse.error(result.getMessage(), result.getErrors());
        }
    }
}
