package com.xiaofeiyang.expense.bill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BillDeleteRequest {

    @NotNull(message = "ID不能为空")
    private Long id;
}
