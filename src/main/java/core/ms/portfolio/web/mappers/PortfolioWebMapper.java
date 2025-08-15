package core.ms.portfolio.web.mappers;

import core.ms.portfolio.application.dto.command.CreatePortfolioCommand;
import core.ms.portfolio.application.dto.query.PortfolioDTO;
import core.ms.portfolio.web.dto.request.CreatePortfolioRequest;
import core.ms.portfolio.web.dto.response.PortfolioResponse;
import core.ms.portfolio.web.dto.response.PositionResponse;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PortfolioWebMapper {

    // Request to Command mappings
    public CreatePortfolioCommand toCommand(CreatePortfolioRequest request) {
        return new CreatePortfolioCommand(
                request.getPortfolioId(),
                request.getOwnerId()
        );
    }

    // DTO to Response mappings
    public PortfolioResponse toResponse(PortfolioDTO dto) {
        PortfolioResponse response = new PortfolioResponse();
        response.setPortfolioId(dto.getPortfolioId());
        response.setOwnerId(dto.getOwnerId());

        // Convert cash balances
        Map<String, BigDecimal> cashBalances = new HashMap<>();
        for (Map.Entry<Currency, Money> entry : dto.getCashBalances().entrySet()) {
            cashBalances.put(entry.getKey().name(), entry.getValue().getAmount());
        }
        response.setCashBalances(cashBalances);

        // Convert positions
        List<PositionResponse> positions = new ArrayList<>();
        for (Map.Entry<Symbol, BigDecimal> entry : dto.getPositions().entrySet()) {
            PositionResponse positionResponse = new PositionResponse();
            positionResponse.setSymbolCode(entry.getKey().getCode());
            positionResponse.setSymbolName(entry.getKey().getName());
            positionResponse.setQuantity(entry.getValue());
            // Other fields would need market data
            positions.add(positionResponse);
        }
        response.setPositions(positions);

        // Set total value
        if (dto.getTotalValue() != null) {
            response.setTotalValue(dto.getTotalValue().getAmount());
            response.setValueCurrency(dto.getTotalValue().getCurrency().name());
        }

        response.setLastUpdated(dto.getLastUpdated());

        return response;
    }
}