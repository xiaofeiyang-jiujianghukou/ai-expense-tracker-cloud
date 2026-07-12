package com.xiaofeiyang.expense.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportResponse {
    private int year;
    private int month;
    private String report;
}
