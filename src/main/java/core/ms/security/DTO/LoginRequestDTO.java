package core.ms.security.DTO;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequestDTO {
    private String username;
    private String password;

    public LoginRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

}
