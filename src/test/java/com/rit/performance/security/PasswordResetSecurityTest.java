package com.rit.performance.security;

import com.rit.performance.controller.PasswordResetController;
import com.rit.performance.service.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.time.Clock;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PasswordResetSecurityTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, PasswordResetController.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
    static class Config {
        @Bean PasswordResetService service() { return mock(PasswordResetService.class); }
        @Bean PasswordResetRateLimiter limiter() { return new PasswordResetRateLimiter(Clock.systemUTC()); }
        @Bean JwtService jwt() { return mock(JwtService.class); }
        @Bean AppUserDetailsService details() { return mock(AppUserDetailsService.class); }
        @Bean JwtAuthenticationFilter filter(JwtService jwt, AppUserDetailsService details) { return new JwtAuthenticationFilter(jwt, details); }
    }

    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext()); context.register(Config.class); context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void close() { context.close(); }

    @Test void publicEndpointsPassActualSecurityChainWithoutToken() throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value(PasswordResetController.ACK));
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"raw\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("SUCCESS"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void expiredBearerDoesNotBlockPublicReset() throws Exception {
        mvc.perform(post("/api/auth/forgot-password").servletPath("/api/auth/forgot-password")
                .header("Authorization", "Bearer expired").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@example.com\"}"))
                .andExpect(status().isOk());
        verify(context.getBean(JwtService.class), never()).parseAccessToken(anyString());
    }
}
