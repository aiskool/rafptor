package com.rafptor.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables {@code @Async} support and exposes a dedicated thread pool for
 * long-running background tasks such as SSH agent deployment.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "deploymentExecutor")
    public Executor deploymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("deployment-");
        executor.initialize();
        return executor;
    }
}
