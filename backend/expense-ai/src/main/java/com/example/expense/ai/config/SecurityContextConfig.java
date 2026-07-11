package com.example.expense.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.annotation.PostConstruct;

@Configuration
public class SecurityContextConfig {

    static {
        // Use inheritable mode so TTL can propagate SecurityContext to async threads
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}
