package com.example.expense.budget.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BudgetVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private Integer year;
    private Integer month;
    private BigDecimal targetAmount;
    private BigDecimal actualAmount;
    private BigDecimal percentage;   // actual / target * 100
}
