package com.rit.performance.controller;

import com.rit.performance.exception.*;
import com.rit.performance.security.*;
import com.rit.performance.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PasswordResetControllerTest {
    private final PasswordResetService service = mock(PasswordResetService.class);
    private final JwtService jwt = mock(JwtService.class);
    private final PasswordResetRateLimiter limiter = new PasswordResetRateLimiter(Clock.systemUTC());
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PasswordResetController(service, limiter))
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new JwtAuthenticationFilter(jwt, mock(AppUserDetailsService.class))).build();

    @Test void publicForgotSucceedsEvenWithStaleBearer() throws Exception {
        for (String email : new String[]{"known@example.com"}) {
            mvc.perform(post("/api/auth/forgot-password").servletPath("/api/auth/forgot-password")
                    .header("Authorization", "Bearer expired").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value(PasswordResetController.ACK));
        }
        verifyNoInteractions(jwt);
    }

    @Test void unknownEmailReturnsClear400Message() throws Exception {
        doThrow(new InvalidOperationException(PasswordResetService.EMAIL_NOT_FOUND))
                .when(service).forgot(eq("unknown@example.com"), anyString());
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No active account was found for this email address."));
    }

    @Test void throttledForgotReturns429InsteadOfSuccess() throws Exception {
        doThrow(new PasswordResetRateLimitException()).when(service).forgot(anyString(), anyString());
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@example.com\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "3600"))
                .andExpect(jsonPath("$.message").value("Too many password reset requests. Please try again later."));
    }
    @Test void resetReturnsSuccessWithoutAuthentication() throws Exception {
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"raw\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Password reset successfully."));
        verify(service).reset("raw", "new-password");
    }

    @Test void invalidResetAndEmailReturn400Message() throws Exception {
        doThrow(new InvalidOperationException(PasswordResetService.INVALID_LINK)).when(service).reset(any(), any());
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"bad\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(PasswordResetService.INVALID_LINK));
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @Test void limitsResetAttempts() throws Exception {
        for (int i = 0; i < 30; i++) mvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON).content("{}"));
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "900"));
        verify(service, times(30)).reset(null, null);
    }
}
