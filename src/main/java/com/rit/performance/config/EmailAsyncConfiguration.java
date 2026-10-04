package com.rit.performance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

@Configuration
@EnableAsync
@Slf4j
public class EmailAsyncConfiguration {
    @Bean(name = "emailTaskExecutor")
    public ThreadPoolTaskExecutor emailTaskExecutor(
            @Value("${app.mail.async.core-pool-size:2}") int corePoolSize,
            @Value("${app.mail.async.max-pool-size:4}") int maxPoolSize,
            @Value("${app.mail.async.queue-capacity:200}") int queueCapacity,
            @Value("${app.mail.async.shutdown-wait-seconds:30}") int shutdownWaitSeconds) {
        Assert.isTrue(corePoolSize > 0 && maxPoolSize >= corePoolSize,
                "Email pool sizes must be positive and max must be at least core");
        Assert.isTrue(queueCapacity > 0 && shutdownWaitSeconds >= 0,
                "Email queue capacity must be positive and shutdown wait must not be negative");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("email-delivery-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(shutdownWaitSeconds);
        // Never run SMTP on the request thread, even when the queue is full.
        // Do not log the task: it may contain an OTP or a password setup link.
        executor.setRejectedExecutionHandler((task, pool) ->
                log.error("Email delivery task was not accepted: executor is full or shutting down; email will not be sent"));
        return executor;
    }
}
