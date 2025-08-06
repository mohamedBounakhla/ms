package core.ms.order_book.application.dto.command;

import jakarta.validation.constraints.NotBlank;

public class CreateSnapshotCommand {
    @NotBlank(message = "Symbol code cannot be blank")
    private String symbolCode;

    public CreateSnapshotCommand() {}

    public CreateSnapshotCommand(String symbolCode) {
        this.symbolCode = symbolCode;
    }

    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
}