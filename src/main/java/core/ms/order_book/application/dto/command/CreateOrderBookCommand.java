package core.ms.order_book.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateOrderBookCommand {
    @NotBlank(message = "Symbol code cannot be blank")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
    private String symbolCode;

    public CreateOrderBookCommand() {}

    public CreateOrderBookCommand(String symbolCode) {
        this.symbolCode = symbolCode;
    }

    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
}