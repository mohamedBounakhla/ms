package core.ms.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.persistence.EnumType;


@Entity
public class MSUser {
    @Id
    @Size(max = 20)
    @Column(name = "username")
    @NotBlank
    private String username;

    @Size(max = 500)
    @Column(name = "userpassword")
    @NotBlank
    private String userPassword;

    @Column(name = "userrole")
    @NotNull
    @Enumerated(EnumType.STRING)
    private MSUserRole role;


    protected MSUser() { super();}

    public MSUser(@Size(max = 20) @NotBlank String username, @NotNull MSUserRole role){
        this.username = username;
        this.role = role;
    }

    public MSUser(@Size(max = 20) @NotBlank String username, @NotNull MSUserRole role, @NotBlank String password) {
        this.username = username;
        this.role = role;
        this.userPassword = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public MSUserRole getRole() {
        return role;
    }

    public void setRole(MSUserRole role) {
        this.role = role;
    }


}
