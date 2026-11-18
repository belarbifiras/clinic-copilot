package com.cliniccopilot.records.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Convertit le token JWT Keycloak en authentification Spring Security
 * avec un AuthUser (sub = UUID, role = PATIENT|DOCTOR|ADMIN) comme Principal.
 */
public class KeycloakRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    @SuppressWarnings("unchecked")
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        Collection<String> roles = realmAccess == null
                ? List.of()
                : (Collection<String>) realmAccess.getOrDefault("roles", List.of());

        String matchedRole = roles.stream()
                .filter(r -> r.equals("ADMIN") || r.equals("PATIENT") || r.equals("DOCTOR"))
                .findFirst()
                .orElse("");

        List<GrantedAuthority> authorities = matchedRole.isEmpty()
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + matchedRole));

        UUID userId = null;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ignored) {
        }

        AuthUser authUser = new AuthUser(userId, matchedRole);

        return new UsernamePasswordAuthenticationToken(authUser, jwt, authorities);
    }
}