package com.rit.performance.controller;

import com.rit.performance.service.OnboardingService;
import com.rit.performance.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OnboardingControllerTest {
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    String body = """
        {"firstName":"Venkat","lastName":"D","email":"venkat@example.com",
        "joiningDate":"2026-01-01","roleId":29,"designationId":100,
        "employmentType":"FULL_TIME","workMode":"OFFSHORE","workLocation":"OFFICE"}
        """;
    @Configuration @EnableWebMvc @EnableMethodSecurity
    @Import({OnboardingController.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean OnboardingService service() { return mock(OnboardingService.class); }
    }
    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext()); context.register(Config.class); context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        when(context.getBean(OnboardingService.class).create(anyString(), anyString(), any()))
                .thenReturn("{\"status\":\"INVITED\"}");
    }
    @AfterEach void close() { SecurityContextHolder.clearContext(); context.close(); }

    @Test void onlyHrAndAdminMayCreateInvitations() throws Exception {
        for (String role : List.of("HR", "ADMIN", "EMPLOYEE")) {
            var auth = new UsernamePasswordAuthenticationToken("caller", "", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(auth);
            mvc.perform(post("/api/v1/hr/onboarding").principal(auth)
                    .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                    .contentType("application/json").content(body))
                    .andExpect(status().is(role.equals("EMPLOYEE") ? 403 : 201));
        }
        verify(context.getBean(OnboardingService.class), times(2)).create(anyString(), anyString(), any());
    }

    @Test void missingFieldsAndMalformedJsonReturn422() throws Exception {
        for (String input : List.of("{}", "{broken"))
            mvc.perform(post("/api/v1/hr/onboarding").contentType("application/json").content(input))
                    .andExpect(status().is(422));
        verifyNoInteractions(context.getBean(OnboardingService.class));
    }
}
