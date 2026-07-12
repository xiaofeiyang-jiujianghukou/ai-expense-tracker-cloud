package com.xiaofeiyang.expense.category.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CategoryUpdateRequest extends CategoryRequest {

    @NotNull(message = "ID不能为空")
    private Long id;
}
