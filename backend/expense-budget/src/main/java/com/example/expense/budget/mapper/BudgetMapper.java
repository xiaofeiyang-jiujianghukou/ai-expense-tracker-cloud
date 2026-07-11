package com.example.expense.budget.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.expense.budget.entity.Budget;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {
}
