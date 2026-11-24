package com.clinic.ai_assessment_service.config;

import com.clinic.ai_assessment_service.security.KeycloakRoleConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Validation des jetons Keycloak, comme les autres services.
 * Mode "permit-all" (par défaut : APP_SECURITY_PERMIT_ALL=true) pour tester l'assistant sans session ;
 * désactivé (false), on repasse en validation des jetons Keycloak (votre partie).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                     @Value("${app.security.permit-all:false}") boolean permitAll) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (permitAll) {
            http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
        } else {
            http.authorizeHttpRequests(a -> a
                            .requestMatchers("/actuator/health/**").permitAll()
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(new KeycloakRoleConverter())));
        }
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.security.keycloak.jwk-set-uri}") String jwkSetUri,
                          @Value("${app.security.keycloak.issuer-uri}") String issuerUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }
}