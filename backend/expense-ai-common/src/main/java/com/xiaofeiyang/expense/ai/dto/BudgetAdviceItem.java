package com.xiaofeiyang.expense.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAdviceItem {
    private String categoryName;
    private int amount;
}
