package core.ms.security.service;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.Map;

/**
 * Interface définissant les opérations de base pour la gestion des tokens d'authentification.
 * Cette interface est indépendante de l'implémentation spécifique (JWT, OAuth, etc.)
 */
public interface TokenService {

    /**
     * Génère un token pour un utilisateur
     * @param userDetails Les détails de l'utilisateur
     * @return Le token généré
     */
    String generateToken(UserDetails userDetails);

    /**
     * Génère un token avec des claims personnalisées
     * @param claims Les claims à inclure dans le token
     * @param subject Le sujet du token (généralement l'identifiant de l'utilisateur)
     * @return Le token généré
     */
    public String generateToken(Map<String, Object> claims, String subject);

    /**
     * Extrait le nom d'utilisateur d'un token
     * @param token Le token à analyser
     * @return Le nom d'utilisateur extrait
     */
    String extractUsername(String token);

    /**
     * Extrait la date d'expiration d'un token
     * @param token Le token à analyser
     * @return La date d'expiration
     */
    Date extractExpiration(String token);

    /**
     * Vérifie si un token est valide pour un utilisateur donné
     * @param token Le token à valider
     * @param userDetails Les détails de l'utilisateur à vérifier
     * @return true si le token est valide, false sinon
     */
    boolean validateToken(String token, UserDetails userDetails);

    /**
     * Vérifie si un token est expiré
     * @param token Le token à vérifier
     * @return true si le token est expiré, false sinon
     */
    boolean isTokenExpired(String token);

    /**
     * Rafraîchit un token existant
     * @param token Le token à rafraîchir
     * @return Un nouveau token avec une nouvelle date d'expiration
     */
    String refreshToken(String token);
}