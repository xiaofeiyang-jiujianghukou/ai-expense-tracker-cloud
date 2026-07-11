package com.example.expense.statistics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TrendVO {

    private List<TrendPoint> points;

    @Data
    @Builder
    public static class TrendPoint {
        private int year;
        private int month;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal balance;
    }
}
