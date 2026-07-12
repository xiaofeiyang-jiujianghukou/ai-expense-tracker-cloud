package com.xiaofeiyang.expense.bill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Bill Service — bill CRUD, CSV export, budget management.
 *
 * <p>Calls category-service (via Feign) for category name resolution.
 * Budget module code is merged into this service.</p>
 */
@SpringBootApplication(scanBasePackages = "com.xiaofeiyang.expense")
@MapperScan({"com.xiaofeiyang.expense.bill.mapper", "com.xiaofeiyang.expense.budget.mapper"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.xiaofeiyang.expense")
public class BillServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillServiceApplication.class, args);
    }
}
