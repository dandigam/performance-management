package com.rit.performance.controller;

import com.rit.performance.dto.request.ChangePasswordRequest;
import com.rit.performance.exception.*;
import com.rit.performance.security.*;
import com.rit.performance.service.ChangePasswordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChangePasswordControllerTest {
    private final ChangePasswordService service = mock(ChangePasswordService.class);
    private final JwtService jwt = mock(JwtService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ChangePasswordController(service))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new JwtAuthenticationFilter(jwt, mock(AppUserDetailsService.class))).build();
    private static final String BODY = "{\"currentPassword\":\"current-password\",\"newPassword\":\"new-password\"}";

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    private void login() {
        var user = User.withUsername("alice").password("unused").roles("USER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void returns204AndUsesPrincipal() throws Exception {
        login();
        mvc.perform(put("/api/auth/change-password").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).change("alice", new ChangePasswordRequest("current-password", "new-password"));
    }

    @Test
    void validationUsesMessageField() throws Exception {
        login();
        doThrow(new InvalidOperationException("Current password is incorrect.")).when(service).change(eq("alice"), any());
        mvc.perform(put("/api/auth/change-password").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Current password is incorrect."));
    }

    @Test
    void malformedBodyUsesMessageField() throws Exception {
        login();
        mvc.perform(put("/api/auth/change-password").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Invalid request body."));
    }

    @Test
    void missingSessionReturns401() throws Exception {
        doThrow(new AuthenticationException("Authentication is required.")).when(service).change(isNull(), any());
        mvc.perform(put("/api/auth/change-password").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidAndExpiredTokensReturn401BeforeController() throws Exception {
        for (String token : new String[]{"invalid", "expired"}) {
            when(jwt.parseAccessToken(token)).thenThrow(new IllegalArgumentException("Invalid or expired access token"));
            mvc.perform(put("/api/auth/change-password").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service);
    }
}
