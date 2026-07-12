package com.xiaofeiyang.expense.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * User Service — registration, login, JWT token generation.
 *
 * <p>Depends on category-service (via Feign) for default category initialization
 * on new user registration.</p>
 */
@SpringBootApplication(scanBasePackages = "com.xiaofeiyang.expense")
@MapperScan("com.xiaofeiyang.expense.user.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.xiaofeiyang.expense")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
