package core.ms.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TokenServiceImpl implements TokenService {
    @Value("${jwt.secret:defaultSecretKeyThatShouldBeVeryLongAndSecureForProduction}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;


    @Override
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
       //Extraire les rôles des autorités
        String roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        claims.put("roles", roles);
        return generateToken(claims, userDetails.getUsername());
    }

    @Override
    public String generateToken(Map<String,Object> claims, String subject){
        Date now = new Date();
        Date expiration = new Date(now.getTime() + this.jwtExpiration);

        //Utilisation de Keys pour générer pour une clé sécurisée
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    private Claims extractAllClaims(String token){
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    @Override
    public boolean isTokenExpired(String token) {
        final Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    @Override
    public String refreshToken(String token) {
        if(isTokenExpired(token)) {
            throw new IllegalArgumentException("Le token est expiré");
        }

        Claims claims = extractAllClaims(token);
        String subject = claims.getSubject();

        claims.remove(Claims.EXPIRATION);
        claims.remove(Claims.ISSUED_AT);

        return generateToken(claims,subject);
    }

}
