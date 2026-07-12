package com.xiaofeiyang.expense.framework.autoconfigure;

import com.xiaofeiyang.expense.framework.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Framework-level Web/MVC infrastructure beans.
 *
 * <p>ObjectMapper is NOT declared here — Spring Boot's
 * JacksonAutoConfiguration already provides one with
 * JavaTimeModule, etc. registered. Services inject the
 * Boot-managed ObjectMapper directly.</p>
 */
@Configuration
public class FrameworkWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
