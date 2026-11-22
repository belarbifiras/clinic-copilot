package com.clinic.ai_assessment_service.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Lit les rôles du realm Keycloak (realm_access.roles) et les transforme en ROLE_ADMIN, ROLE_DOCTOR, ROLE_PATIENT. */
public class KeycloakRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    @SuppressWarnings("unchecked")
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        Collection<String> roles = realmAccess == null
                ? List.of()
                : (Collection<String>) realmAccess.getOrDefault("roles", List.of());

        List<GrantedAuthority> authorities = roles.stream()
                .filter(r -> r.equals("ADMIN") || r.equals("PATIENT") || r.equals("DOCTOR"))
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}