package com.rit.performance.config;

import com.rit.performance.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class BusinessActionLoggingConfiguration implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(BusinessActionLoggingConfiguration.class);

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                    Exception exception) {
                if (!Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod())) return;
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                Object actor = authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                        ? user.id() : "anonymous";
                Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                String ids = variables instanceof Map<?, ?> values ? values.entrySet().stream()
                        .filter(entry -> entry.getKey().toString().toLowerCase(java.util.Locale.ROOT).endsWith("id"))
                        .filter(entry -> entry.getValue().toString().matches("[0-9]{1,19}"))
                        .map(entry -> entry.getKey() + "=" + entry.getValue()).sorted()
                        .collect(Collectors.joining(",")) : "";
                log.info("Business action requestId={} userId={} method={} route={} recordIds={} outcome={} status={}",
                        MDC.get("requestId"), actor, request.getMethod(),
                        request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE), ids,
                        exception == null && response.getStatus() < 400 ? "SUCCESS" : "FAILED",
                        exception == null ? response.getStatus() : 500);
            }
        }).addPathPatterns("/api/**");
    }
}
