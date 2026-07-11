package com.example.expense.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class TtlExecutorConfig {

    /**
     * TTL-wrapped ThreadPoolTaskExecutor — propagates ThreadLocal values
     * (including SecurityContext) across all async thread boundaries.
     * Used by Spring MVC for SseEmitter, @Async, etc.
     */
    @Bean("ttlExecutor")
    public Executor ttlExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ttl-async-");
        executor.setDaemon(true);
        executor.initialize();
        return TtlExecutors.getTtlExecutor(executor);
    }
}
