package com.example.expense.statistics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DailyVO {

    private int year;
    private int month;
    private List<DailyPoint> days;

    @Data
    @Builder
    public static class DailyPoint {
        private int day;
        private BigDecimal income;
        private BigDecimal expense;
    }
}
