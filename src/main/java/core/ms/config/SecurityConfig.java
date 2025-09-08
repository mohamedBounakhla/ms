package core.ms.config;

import core.ms.security.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS with proper configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // Authentication endpoints
                        .requestMatchers("/login", "/register", "/validate", "/refresh").permitAll()

                        // H2 Console
                        .requestMatchers("/h2-console/**").permitAll()

                        // Public API endpoints - no authentication needed
                        .requestMatchers("/api/v1/market-data/**").permitAll()
                        .requestMatchers("/api/v1/charts/**").permitAll()
                        .requestMatchers("/api/v1/internal/**").permitAll()
                        .requestMatchers("/api/v1/market/**").permitAll()

                        // WebSocket endpoints
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/ws/info").permitAll()
                        .requestMatchers("/ws/market-data/**").permitAll()
                        .requestMatchers("/app/**").permitAll()
                        .requestMatchers("/topic/**").permitAll()

                        // Bot endpoints
                        .requestMatchers("/api/v1/bots/**").permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // For H2 console
        http.headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow localhost for demo purposes
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));

        // Allow all methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Expose authorization header
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Cache preflight response
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}