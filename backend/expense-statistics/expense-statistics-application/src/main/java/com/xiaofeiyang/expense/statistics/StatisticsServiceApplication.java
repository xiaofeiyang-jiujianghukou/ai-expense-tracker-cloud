package com.xiaofeiyang.expense.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Statistics Service — monthly aggregation, trend analysis, Excel export.
 *
 * <p>No database tables. Queries bill-service (via Feign) for bill data,
 * category-service (via Feign) for category names, then aggregates in-memory.</p>
 */
@SpringBootApplication(scanBasePackages = "com.xiaofeiyang.expense")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.xiaofeiyang.expense")
public class StatisticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StatisticsServiceApplication.class, args);
    }
}
