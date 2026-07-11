package com.example.expense.budget.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.expense.budget.dto.BudgetRequest;
import com.example.expense.budget.dto.BudgetVO;
import com.example.expense.budget.entity.Budget;
import com.example.expense.budget.mapper.BudgetMapper;
import com.example.expense.common.exception.BusinessException;
import com.example.expense.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetMapper budgetMapper;

    public void setBudget(BudgetRequest request, Long userId) {
        // Upsert: if exists for same user+cat+month, update; else insert
        Budget existing = budgetMapper.selectOne(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getCategoryId, request.getCategoryId())
                .eq(Budget::getYear, request.getYear())
                .eq(Budget::getMonth, request.getMonth()));
        if (existing != null) {
            existing.setTargetAmount(request.getTargetAmount());
            budgetMapper.updateById(existing);
        } else {
            Budget budget = new Budget();
            budget.setUserId(userId);
            budget.setCategoryId(request.getCategoryId());
            budget.setYear(request.getYear());
            budget.setMonth(request.getMonth());
            budget.setTargetAmount(request.getTargetAmount());
            budgetMapper.insert(budget);
        }
    }

    public List<BudgetVO> listBudget(Long userId, int year, int month,
                                      Map<Long, BigDecimal> actualByCategory,
                                      Map<Long, String> categoryNames) {
        List<Budget> budgets = budgetMapper.selectList(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getYear, year)
                .eq(Budget::getMonth, month));
        return budgets.stream().map(b -> {
            BigDecimal actual = actualByCategory.getOrDefault(b.getCategoryId(), BigDecimal.ZERO);
            BigDecimal pct = b.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                    ? actual.multiply(BigDecimal.valueOf(100)).divide(b.getTargetAmount(), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return BudgetVO.builder()
                    .id(b.getId()).categoryId(b.getCategoryId())
                    .categoryName(categoryNames.getOrDefault(b.getCategoryId(), "未知"))
                    .year(b.getYear()).month(b.getMonth())
                    .targetAmount(b.getTargetAmount())
                    .actualAmount(actual).percentage(pct)
                    .build();
        }).toList();
    }
}
