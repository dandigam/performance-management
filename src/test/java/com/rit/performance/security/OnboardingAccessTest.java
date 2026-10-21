package com.rit.performance.security;

import com.rit.performance.entity.User;
import com.rit.performance.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OnboardingAccessTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void restrictedAccountCannotReachBusinessOrHrApisButCanReadOwnSession() throws Exception {
        AppUserDetailsService details = mock(AppUserDetailsService.class);
        when(details.roleAuthority(any())).thenReturn("ROLE_HR");
        JwtService jwt = new JwtService(details);
        ReflectionTestUtils.setField(jwt, "secret", "a".repeat(64));
        ReflectionTestUtils.setField(jwt, "issuer", "test");
        ReflectionTestUtils.setField(jwt, "audience", "test");
        ReflectionTestUtils.setField(jwt, "accessExpirationMs", 900000L);
        User user = new User(); user.setId(1L); user.setUsername("invitee");
        user.setPortalAccess("ONBOARDING_ONLY");
        UserRepository users = mock(UserRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(details.loadUserByUsername("invitee")).thenReturn(
                new AuthenticatedUser(1L, "invitee", "hash", true, 0, List.of()));
        var filter = new JwtAuthenticationFilter(jwt, details, users);
        for (String path : List.of("/api/v1/employees", "/api/v1/hr/onboarding", "/api/auth/me", "/api/v1/onboarding/me")) {
            SecurityContextHolder.clearContext();
            var request = new MockHttpServletRequest("GET", path);
            request.setServletPath(path);
            request.addHeader("Authorization", "Bearer " + jwt.createAccessToken(user).value());
            var response = new MockHttpServletResponse();
            var chain = new MockFilterChain();
            filter.doFilter(request, response, chain);
            boolean allowed = path.equals("/api/auth/me") || path.equals("/api/v1/onboarding/me");
            assertThat(response.getStatus()).isEqualTo(allowed ? 200 : 403);
            if (!allowed) assertThat(chain.getRequest()).isNull();
            else assertThat(chain.getRequest()).isNotNull();
            assertThat(user.getPortalAccess()).isEqualTo("ONBOARDING_ONLY");
        }
    }

    @Test void allowsOnlySupportedMethodsAndSections() {
        for (String section : List.of("personal", "address", "education", "employment-history", "bank-details", "documents")) {
            String path = "/api/v1/onboarding/me/" + section;
            assertThat(JwtAuthenticationFilter.isOnboardingRequestAllowed("PUT", path)).isTrue();
            assertThat(JwtAuthenticationFilter.isOnboardingRequestAllowed("DELETE", path)).isFalse();
            assertThat(JwtAuthenticationFilter.isOnboardingRequestAllowed("POST", path)).isFalse();
        }
        assertThat(JwtAuthenticationFilter.isOnboardingRequestAllowed("POST", "/api/v1/onboarding/me/submit")).isTrue();
        assertThat(JwtAuthenticationFilter.isOnboardingRequestAllowed("GET", "/api/v1/onboarding/me/submit")).isFalse();
        for (String path : List.of("/api/v1/onboarding/me/compensation", "/api/v1/onboarding/101",
                "/api/v1/onboarding/me/../102", "/api/v1/documents/12", "/api/v1/employees/102/documents",
                "/api/v1/onboarding/me/approve", "/api/v1/onboarding/me/personal/102")) {
            for (String method : List.of("GET", "POST", "PUT", "DELETE"))
                assertThat(JwtAuthenticationFilter.isOnboardingRequestAllowed(method, path)).isFalse();
        }
        for (String path : List.of("/api/auth/login", "/api/auth/refresh", "/api/auth/logout")) {
            assertThat(JwtAuthenticationFilter.isOnboardingRequestAllowed("POST", path)).isTrue();
            assertThat(JwtAuthenticationFilter.isOnboardingRequestAllowed("DELETE", path)).isFalse();
        }
        assertThat(JwtAuthenticationFilter.isOnboardingRequestAllowed("PUT", "/api/auth/change-password")).isTrue();
    }
}
