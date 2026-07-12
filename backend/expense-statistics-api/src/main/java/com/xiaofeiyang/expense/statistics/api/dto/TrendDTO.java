package com.xiaofeiyang.expense.statistics.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDTO {
    private List<TrendPoint> points;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private int year;
        private int month;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal balance;
    }
}
