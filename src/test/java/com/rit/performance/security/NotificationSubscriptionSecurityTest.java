package com.rit.performance.security;

import com.rit.performance.controller.NotificationSubscriptionController;
import com.rit.performance.service.NotificationSubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(NotificationSubscriptionSecurityTest.Config.class)
class NotificationSubscriptionSecurityTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean NotificationSubscriptionService service() { return mock(NotificationSubscriptionService.class); }
        @Bean NotificationSubscriptionController controller(NotificationSubscriptionService service) {
            return new NotificationSubscriptionController(service);
        }
    }

    @Autowired NotificationSubscriptionController controller;

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void nonAdminCannotReadOrModifySubscriptions() {
        assertThatThrownBy(() -> controller.list()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.get(1L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.create(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.update(1L, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.delete(1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListSubscriptions() {
        assertThatCode(() -> controller.list()).doesNotThrowAnyException();
    }
}
