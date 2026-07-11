package com.example.expense.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorizeResponse {
    private Long categoryId;
    private String categoryName;
    private BigDecimal confidence;
    private String reason;
}
