package com.clinic.patient_profile.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        JsonSecurityErrorHandler errorHandler = new JsonSecurityErrorHandler();

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/prometheus", "/error").permitAll()
                // order matters: /me must be matched before the catch-all admin rule
                .requestMatchers("/api/patient-profiles/me").hasRole("PATIENT")
                .requestMatchers("/api/patient-profiles/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .authenticationEntryPoint(errorHandler)
                .accessDeniedHandler(errorHandler)
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakRoleConverter())));

        return http.build();
    }
}