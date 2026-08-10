package com.rit.performance.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousApplicationRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/vendors"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication is required"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonFinanceUserCannotAccessVendors() throws Exception {
        mockMvc.perform(get("/api/v1/vendors"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access is denied"));
    }

    @Test
    void localUiOriginCanCallAuthenticationApi() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "http://localhost:8081"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
