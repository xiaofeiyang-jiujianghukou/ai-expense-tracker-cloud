package com.xiaofeiyang.expense.budget;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.xiaofeiyang.expense")
@MapperScan("com.xiaofeiyang.expense.budget.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.xiaofeiyang.expense")
public class BudgetServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BudgetServiceApplication.class, args);
    }
}
