package core.ms.portfolio.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatePortfolioRequest {

    @NotBlank(message = "Portfolio ID cannot be blank")
    @Size(min = 1, max = 50, message = "Portfolio ID must be between 1 and 50 characters")
    private String portfolioId;

    @NotBlank(message = "Owner ID cannot be blank")
    @Size(min = 1, max = 50, message = "Owner ID must be between 1 and 50 characters")
    private String ownerId;

    // Constructors
    public CreatePortfolioRequest() {}

    public CreatePortfolioRequest(String portfolioId, String ownerId) {
        this.portfolioId = portfolioId;
        this.ownerId = ownerId;
    }

    // Getters and Setters
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
}