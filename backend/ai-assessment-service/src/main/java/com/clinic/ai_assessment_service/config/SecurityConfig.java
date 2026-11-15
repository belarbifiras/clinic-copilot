package com.clinic.ai_assessment_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sécurité TEMPORAIRE.
 * - app.security.permit-all=true  : aucune authentification (développement local uniquement).
 * - app.security.permit-all=false : tout est refusé sauf /actuator/health, en attendant le branchement JWT
 *   (à faire quand on connaîtra la configuration JWT de la gateway et de identity-profile-service).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    @Value("${app.security.permit-all:true}") boolean permitAll) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (permitAll) {
            log.warn("SECURITY: app.security.permit-all=true -> endpoints ouverts SANS authentification (dev uniquement)");
            http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
        } else {
            http.authorizeHttpRequests(a -> a
                    .requestMatchers("/actuator/health/**").permitAll()
                    .anyRequest().authenticated());
        }
        return http.build();
    }
}
