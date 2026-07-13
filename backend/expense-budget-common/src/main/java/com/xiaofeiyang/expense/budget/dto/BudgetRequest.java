package com.xiaofeiyang.expense.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetRequest {
    @NotNull
    private Long categoryId;
    @NotNull
    private Integer year;
    @NotNull
    private Integer month;
    @NotNull
    @Positive
    private BigDecimal targetAmount;
}
