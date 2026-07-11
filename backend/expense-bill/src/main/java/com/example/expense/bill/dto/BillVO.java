package com.example.expense.bill.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BillVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private String type;
    private String description;
    private LocalDate billDate;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
