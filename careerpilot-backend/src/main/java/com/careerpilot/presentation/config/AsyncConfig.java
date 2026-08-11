package com.careerpilot.presentation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async thread-pool configuration.
 * Used by both F2 (resume parsing) and F6 (resume tailoring).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "resumeTaskExecutor")
    public Executor resumeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("resume-async-");
        executor.initialize();
        return executor;
    }
}
