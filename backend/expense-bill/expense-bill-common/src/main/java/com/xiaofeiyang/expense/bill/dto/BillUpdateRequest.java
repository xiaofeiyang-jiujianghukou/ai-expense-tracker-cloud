package com.xiaofeiyang.expense.bill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BillUpdateRequest extends BillRequest {

    @NotNull(message = "ID不能为空")
    private Long id;
}
