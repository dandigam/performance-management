package com.rit.performance.config;

import com.rit.performance.security.AuthenticatedUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfiguration {

    @Bean
    AuditorAware<Long> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(authentication -> !(authentication instanceof AnonymousAuthenticationToken))
                .map(Authentication::getPrincipal)
                .filter(AuthenticatedUser.class::isInstance)
                .map(AuthenticatedUser.class::cast)
                .map(AuthenticatedUser::id);
    }
}
