package core.ms.security.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenResponseDTO {
    private String token;
    private long expiresIn;

    public TokenResponseDTO(String token, long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }

    private String tokenType="Bearer";

}
