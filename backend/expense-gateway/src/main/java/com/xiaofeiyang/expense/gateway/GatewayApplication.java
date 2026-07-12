package com.xiaofeiyang.expense.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Spring Cloud Gateway — unified API entry point for the Expense Cloud microservices.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Route requests to backend services by path prefix</li>
 *   <li>Validate JWT and inject X-User-Id / X-User-Email headers</li>
 *   <li>Global CORS configuration</li>
 *   <li>Rate limiting via Sentinel</li>
 * </ul>
 *
 * <p>This application does NOT include Spring MVC / Tomcat — it runs on
 * Netty (reactive) as required by Spring Cloud Gateway.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
