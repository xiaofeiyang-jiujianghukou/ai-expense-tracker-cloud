package com.xiaofeiyang.expense.category.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryDeleteRequest {

    @NotNull(message = "ID不能为空")
    private Long id;
}
