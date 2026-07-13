package com.xiaofeiyang.expense.budget.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("budget")
public class Budget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long categoryId;
    private Integer year;
    private Integer month;
    private BigDecimal targetAmount;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
