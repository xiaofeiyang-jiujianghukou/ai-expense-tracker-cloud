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
public class DailyDTO {
    private int year;
    private int month;
    private List<DailyPoint> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyPoint {
        private int day;
        private BigDecimal income;
        private BigDecimal expense;
    }
}
