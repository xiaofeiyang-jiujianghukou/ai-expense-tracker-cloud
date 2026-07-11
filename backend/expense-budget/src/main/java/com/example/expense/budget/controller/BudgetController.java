package com.example.expense.budget.controller;

import com.example.expense.budget.dto.BudgetRequest;
import com.example.expense.budget.dto.BudgetVO;
import com.example.expense.budget.service.BudgetService;
import com.example.expense.category.service.CategoryService;
import com.example.expense.common.ApiResponse;
import com.example.expense.common.enums.BillType;
import com.example.expense.common.util.SecurityUtil;
import com.example.expense.statistics.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final StatisticsService statisticsService;
    private final CategoryService categoryService;

    @Data
    public static class BudgetListRequest {
        private int year;
        private int month;
    }

    @PostMapping("/set")
    public ApiResponse<Void> setBudget(@Valid @RequestBody BudgetRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        budgetService.setBudget(request, userId);
        return ApiResponse.success();
    }

    @PostMapping("/list")
    public ApiResponse<List<BudgetVO>> listBudget(@RequestBody BudgetListRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        Map<Long, BigDecimal> actualByCategory = statisticsService.sumByCategory(
                userId, request.getYear(), request.getMonth(), BillType.EXPENSE);
        Map<Long, String> categoryNames = categoryService.listByUser(userId, BillType.EXPENSE).stream()
                .collect(Collectors.toMap(c -> c.getId(), c -> c.getName()));
        return ApiResponse.success(
                budgetService.listBudget(userId, request.getYear(), request.getMonth(),
                        actualByCategory, categoryNames));
    }
}
