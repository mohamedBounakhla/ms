package core.ms.security.ui;

import core.ms.security.dto.LoginRequestDTO;
import core.ms.security.dto.TokenResponseDTO;
import core.ms.security.domain.MSUserRole;
import core.ms.security.exception.UsernameAlreadyExistException;
import core.ms.security.service.AuthenticationService;
import core.ms.security.service.MSUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private MSUserService userService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            TokenResponseDTO response = authService.authenticate(loginRequest);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de l'authentification: " + e.getMessage(), e);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody LoginRequestDTO registerRequest) {
        System.out.println("Register request received: " + registerRequest.getUsername());

        try {
            userService.createUser(
                    registerRequest.getUsername(),
                    registerRequest.getPassword(),
                    MSUserRole.CUSTOMER
            );
            return ResponseEntity.status(HttpStatus.CREATED).body("Utilisateur créé avec succès");
        } catch (UsernameAlreadyExistException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Ce nom d'utilisateur existe déjà", e);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la création de l'utilisateur", e);
        }
    }



    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDTO> refreshToken(@RequestParam String token) {
        try {
            TokenResponseDTO response = authService.refreshToken(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Impossible de rafraîchir le token: " + e.getMessage(), e);
        }
    }
}
