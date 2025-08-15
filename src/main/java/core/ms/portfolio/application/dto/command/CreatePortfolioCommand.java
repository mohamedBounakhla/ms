package core.ms.portfolio.application.dto.command;

public class CreatePortfolioCommand {
    private String portfolioId;
    private String ownerId;

    public CreatePortfolioCommand() {}

    public CreatePortfolioCommand(String portfolioId, String ownerId) {
        this.portfolioId = portfolioId;
        this.ownerId = ownerId;
    }

    // Getters and Setters
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
}