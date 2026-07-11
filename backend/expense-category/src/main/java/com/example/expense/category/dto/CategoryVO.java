package com.example.expense.category.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CategoryVO {
    private Long id;
    private String name;
    private String type;
    private LocalDateTime createdTime;
}
