package com.example.expense;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.expense")
public class ExpenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseApplication.class, args);
    }
}
