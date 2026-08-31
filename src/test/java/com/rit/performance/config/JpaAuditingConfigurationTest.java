package com.rit.performance.config;

import com.rit.performance.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaAuditingConfigurationTest {

    private final AuditorAware<Long> auditorAware =
            new JpaAuditingConfiguration().auditorAware();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesDatabaseUserIdFromAuthenticatedPrincipal() {
        AuthenticatedUser principal =
                new AuthenticatedUser(42L, "user", "password", true, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        assertEquals(42L, auditorAware.getCurrentAuditor().orElseThrow());
    }

    @Test
    void returnsEmptyWithoutAuthentication() {
        assertTrue(auditorAware.getCurrentAuditor().isEmpty());
    }
}
