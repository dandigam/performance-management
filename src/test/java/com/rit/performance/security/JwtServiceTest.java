package com.rit.performance.security;

import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppUserDetailsService userDetailsService = mock(AppUserDetailsService.class);
        jwtService = new JwtService(userDetailsService);
        ReflectionTestUtils.setField(jwtService, "secret",
                "unit-test-secret-with-more-than-thirty-two-bytes");
        ReflectionTestUtils.setField(jwtService, "issuer", "test-issuer");
        ReflectionTestUtils.setField(jwtService, "audience", "test-ui");
        ReflectionTestUtils.setField(jwtService, "accessExpirationMs", 60_000L);
        jwtService.validateConfiguration();

        LookupValue role = LookupValue.builder().code("FINANCE").name("Finance").build();
        when(userDetailsService.roleAuthority(role)).thenReturn("ROLE_FINANCE");
    }

    @Test
    void createsAndValidatesSignedAccessToken() {
        LookupValue role = LookupValue.builder().code("FINANCE").name("Finance").build();
        AppUserDetailsService detailsService =
                (AppUserDetailsService) ReflectionTestUtils.getField(jwtService, "userDetailsService");
        when(detailsService.roleAuthority(role)).thenReturn("ROLE_FINANCE");
        User user = new User(7L, "finance.user", "ignored", "ACTIVE", role, null);

        JwtService.AccessToken token = jwtService.createAccessToken(user);
        Claims claims = jwtService.parseAccessToken(token.value());

        assertEquals("finance.user", claims.getSubject());
        assertEquals("ROLE_FINANCE", claims.get("role", String.class));
        assertEquals("access", claims.get("tokenType", String.class));
    }

    @Test
    void rejectsModifiedToken() {
        LookupValue role = LookupValue.builder().code("FINANCE").name("Finance").build();
        AppUserDetailsService detailsService =
                (AppUserDetailsService) ReflectionTestUtils.getField(jwtService, "userDetailsService");
        when(detailsService.roleAuthority(role)).thenReturn("ROLE_FINANCE");
        User user = new User(7L, "finance.user", "ignored", "ACTIVE", role, null);
        String token = jwtService.createAccessToken(user).value();
        String[] parts = token.split("\\.");
        parts[2] = (parts[2].startsWith("a") ? "b" : "a") + parts[2].substring(1);
        String modified = String.join(".", parts);

        assertThrows(RuntimeException.class, () -> jwtService.parseAccessToken(modified));
    }
}
