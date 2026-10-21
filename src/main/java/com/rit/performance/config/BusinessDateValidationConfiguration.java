package com.rit.performance.config;

import java.time.Clock;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessDateValidationConfiguration {
    @Bean
    LocalValidatorFactoryBean businessDateValidation(Clock clock) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setConfigurationInitializer(configuration -> configuration.clockProvider(() -> clock));
        return validator;
    }
}
