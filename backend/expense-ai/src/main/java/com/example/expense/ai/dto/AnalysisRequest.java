package com.example.expense.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnalysisRequest {
    @NotNull
    private Integer year;

    @NotNull
    @Min(1) @Max(12)
    private Integer month;

    private boolean forceRefresh;
}
