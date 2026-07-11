package com.example.expense.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnalysisResponse {
    private int year;
    private int month;
    private List<String> insights;
}
