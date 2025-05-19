package core.ms.security.service;

import core.ms.security.DTO.LoginRequestDTO;
import core.ms.security.DTO.TokenResponseDTO;

public interface AuthenticationService {
    /**
     * Authentifier un utilisateur et générer un token
     * @param loginRequest DTO avec les identifiants
     * @return DTO avec le token généré
     */
    TokenResponseDTO authenticate(LoginRequestDTO loginRequest);

    /**
     * Valider un token
     * @param token Token à valider
     * @return Nom d'utilisateur si valide
     */
    String validateToken(String token);

    /**
     * Rafraîchir un token
     * @param token Token à rafraîchir
     * @return DTO avec nouveau token
     */
    TokenResponseDTO refreshToken(String token);
}
