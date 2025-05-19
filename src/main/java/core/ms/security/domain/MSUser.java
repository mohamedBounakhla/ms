package core.ms.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
public class MSUser {
    @Setter
    @Getter
    @Id
    @Size(max = 20)
    @Column(name = "username")
    @NotBlank
    private String username;

    @Setter
    @Getter
    @Size(max = 500)
    @Column(name = "userpassword")
    @NotBlank
    private String userPassword;

    @Column(name = "userrole", length = 20)
    @NotNull
    private String userRoleStr;

    protected MSUser() { super(); }

    public MSUser(@Size(max = 20) @NotBlank String username, @NotNull MSUserRole role) {
        this.username = username;
        setRole(role);
    }

    public MSUser(@Size(max = 20) @NotBlank String username, @NotNull MSUserRole role, @NotBlank String password) {
        this.username = username;
        setRole(role);
        this.userPassword = password;
    }

    public MSUserRole getRole() {
        return MSUserRole.valueOf(userRoleStr);
    }

    public void setRole(MSUserRole role) {
        this.userRoleStr = role.name();
    }
}