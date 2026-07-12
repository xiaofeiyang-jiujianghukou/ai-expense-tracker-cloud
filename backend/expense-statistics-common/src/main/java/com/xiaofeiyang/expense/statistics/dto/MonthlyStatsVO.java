package com.xiaofeiyang.expense.statistics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MonthlyStatsVO {
    private int year;
    private int month;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;
    private List<CategorySummary> categoryBreakdown;

    @Data
    @Builder
    public static class CategorySummary {
        private Long categoryId;
        private String categoryName;
        private BigDecimal amount;
        private BigDecimal percentage;
    }
}
