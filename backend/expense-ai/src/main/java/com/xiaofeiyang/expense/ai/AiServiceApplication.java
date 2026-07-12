package com.xiaofeiyang.expense.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI Service — auto-categorize, analysis, report generation, anomaly detection.
 *
 * <p>No database tables. Calls statistics-service (via Feign) for monthly stats
 * and trend data. Uses Redis for caching. Calls LLM (DeepSeek) via AgentScope.</p>
 */
@SpringBootApplication(scanBasePackages = "com.xiaofeiyang.expense")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.xiaofeiyang.expense")
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
