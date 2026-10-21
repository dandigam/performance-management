package com.rit.performance.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final com.rit.performance.repository.UserRepository users;

    @Value("${app.security.authentication-required:true}")
    private boolean authenticationRequired;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("POST".equals(request.getMethod()) &&
                ("/api/auth/forgot-password".equals(request.getServletPath())
                 || "/api/auth/reset-password".equals(request.getServletPath())
                 || "/api/auth/password-otp".equals(request.getServletPath()))) return true;
        String authorization = request.getHeader("Authorization");
        boolean hasBearerToken = StringUtils.hasText(authorization)
                && authorization.startsWith("Bearer ");
        return !authenticationRequired && !hasBearerToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(7).trim();
            Claims claims = jwtService.parseAccessToken(token);
            String username = claims.getSubject();
            if (!StringUtils.hasText(username)) {
                reject(response, "Invalid access token");
                return;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails instanceof AuthenticatedUser account) {
                Number version = claims.get("sessionVersion", Number.class);
                long tokenVersion = version == null ? 0 : version.longValue();
                if (tokenVersion != account.sessionVersion()) {
                    reject(response, "Session has been revoked");
                    return;
                }
            }
            if (!userDetails.isEnabled()) {
                reject(response, "User account is inactive");
                return;
            }
            if (userDetails instanceof AuthenticatedUser account
                    && users.findById(account.id()).map(user -> "ONBOARDING_ONLY".equals(user.getPortalAccess()))
                            .orElse(false)
                    && request.getServletPath().startsWith("/api/")
                    && !isOnboardingRequestAllowed(request.getMethod(), request.getServletPath())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"code\":\"ONBOARDING_ACCESS_ONLY\","
                        + "\"message\":\"This account only has onboarding access.\"}");
                return;
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            reject(response, "Invalid or expired access token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"type\":\"WARNING\",\"code\":\"INVALID_ACCESS_TOKEN\","
                + "\"message\":\"" + message + "\"}");
    }

    // Keep this list explicit: a prefix match would expose future admin/employee-ID routes.
    static boolean isOnboardingRequestAllowed(String method, String path) {
        if ("GET".equals(method) && java.util.Set.of("/api/v1/notifications",
                "/api/v1/notifications/unread-count").contains(path)) return true;
        if ("PATCH".equals(method) && ("/api/v1/notifications/read-all".equals(path)
                || path.matches("/api/v1/notifications/[0-9]+/read"))) return true;
        return switch (method) {
            case "GET" -> java.util.Set.of("/api/auth/me", "/api/v1/onboarding/me").contains(path);
            case "POST" -> java.util.Set.of("/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                    "/api/v1/onboarding/me/submit").contains(path);
            case "PUT" -> java.util.Set.of("/api/auth/change-password",
                    "/api/v1/onboarding/me/personal", "/api/v1/onboarding/me/address",
                    "/api/v1/onboarding/me/education", "/api/v1/onboarding/me/employment-history",
                    "/api/v1/onboarding/me/bank-details", "/api/v1/onboarding/me/documents").contains(path);
            default -> false;
        };
    }
}
