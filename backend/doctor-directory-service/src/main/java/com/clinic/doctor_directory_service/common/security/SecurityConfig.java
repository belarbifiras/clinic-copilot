package com.clinic.doctor_directory_service.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Ce service ne gère aucune authentification : il valide seulement les JWT émis par Keycloak
 * (signature vérifiée avec la clé publique du realm, rôles lus dans realm_access.roles).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/directory/**", "/api/specialties/**", "/api/languages/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/doctors/**").hasRole("DOCTOR")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .authenticationEntryPoint(new ApiAuthenticationEntryPoint())
                .accessDeniedHandler(new ApiAccessDeniedHandler())
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakRoleConverter())));
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