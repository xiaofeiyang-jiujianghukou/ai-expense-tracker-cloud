package com.xiaofeiyang.expense.bill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillDTO {
    private Long id;
    private Long userId;
    private Long categoryId;
    private BigDecimal amount;
    private String type;
    private String description;
    private LocalDate billDate;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
