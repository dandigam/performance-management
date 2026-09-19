package com.rit.performance.security;

import com.rit.performance.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResetSessionRevocationTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void rejectsOldSignedTokenAndAcceptsNewVersion() throws Exception {
        AppUserDetailsService details = mock(AppUserDetailsService.class);
        when(details.roleAuthority(any())).thenReturn("ROLE_USER");
        JwtService jwt = new JwtService(details);
        ReflectionTestUtils.setField(jwt, "secret", "a".repeat(64));
        ReflectionTestUtils.setField(jwt, "issuer", "test");
        ReflectionTestUtils.setField(jwt, "audience", "test");
        ReflectionTestUtils.setField(jwt, "accessExpirationMs", 900000L);
        User user = new User(); user.setId(1L); user.setUsername("alice");
        String before = jwt.createAccessToken(user).value();
        user.setSessionVersion(1L);
        when(details.loadUserByUsername("alice")).thenReturn(new AuthenticatedUser(1L, "alice", "hash", true, 1L, List.of()));
        var filter = new JwtAuthenticationFilter(jwt, details);
        var request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer " + before);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer " + jwt.createAccessToken(user).value());
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(200, response.getStatus());
        assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
