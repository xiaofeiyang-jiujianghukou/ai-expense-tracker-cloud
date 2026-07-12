package com.xiaofeiyang.expense.category.dto;

import lombok.Data;

@Data
public class CategoryListRequest {
    private String type;  // INCOME / EXPENSE, null = all
}
