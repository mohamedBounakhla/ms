package core.ms.security.service;

import core.ms.security.dto.LoginRequestDTO;
import core.ms.security.dto.TokenResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AuthenticationServiceImpl implements AuthenticationService{
    @Autowired
    private MSUserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Override
    public TokenResponseDTO authenticate(LoginRequestDTO loginRequest) {
        UserDetails userDetails = userService.loadUserByUsername(loginRequest.getUsername());

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(loginRequest.getPassword(), userDetails.getPassword())) {
            throw new BadCredentialsException("Mot de passe incorrect");
        }

        // Générer le token
        String token = tokenService.generateToken(userDetails);

        // Retourner la réponse
        return new TokenResponseDTO(token, jwtExpiration / 1000);
    }

    @Override
    public String validateToken(String token) {
        String username = tokenService.extractUsername(token);
        UserDetails userDetails = userService.loadUserByUsername(username);

        if (tokenService.validateToken(token, userDetails)) {
            return username;
        } else {
            throw new BadCredentialsException("Token invalide");
        }
    }

    @Override
    public TokenResponseDTO refreshToken(String token) {
        // Valider le token actuel
        String username = tokenService.extractUsername(token);
        UserDetails userDetails = userService.loadUserByUsername(username);

        if (!tokenService.validateToken(token, userDetails)) {
            throw new BadCredentialsException("Token invalide");
        }

        // Rafraîchir le token
        String refreshedToken = tokenService.refreshToken(token);
        return new TokenResponseDTO(refreshedToken, jwtExpiration / 1000);
    }
}
