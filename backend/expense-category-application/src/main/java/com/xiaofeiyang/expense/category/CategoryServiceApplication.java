package com.xiaofeiyang.expense.category;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Category Service — CRUD for expense/income categories.
 *
 * <p>Leaf service: no Feign clients. Called by user-service (default init),
 * bill-service (category name resolution), and statistics-service.</p>
 */
@SpringBootApplication(scanBasePackages = "com.xiaofeiyang.expense")
@MapperScan("com.xiaofeiyang.expense.category.mapper")
@EnableDiscoveryClient
public class CategoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CategoryServiceApplication.class, args);
    }
}
