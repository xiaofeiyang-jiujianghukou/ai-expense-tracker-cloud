package com.xiaofeiyang.expense.budget.controller;

import com.xiaofeiyang.expense.budget.dto.BudgetRequest;
import com.xiaofeiyang.expense.budget.dto.BudgetVO;
import com.xiaofeiyang.expense.budget.service.BudgetService;
import com.xiaofeiyang.expense.category.api.client.CategoryClient;
import com.xiaofeiyang.expense.category.api.dto.CategoryDTO;
import com.xiaofeiyang.expense.category.api.dto.CategoryListRequest;
import com.xiaofeiyang.expense.framework.ApiResponse;
import com.xiaofeiyang.expense.framework.util.SecurityUtil;
import com.xiaofeiyang.expense.statistics.api.client.StatisticsClient;
import com.xiaofeiyang.expense.statistics.api.dto.MonthlyStatsDTO;
import com.xiaofeiyang.expense.statistics.api.dto.MonthlyStatsRequest;
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
    private final StatisticsClient statisticsClient;
    private final CategoryClient categoryClient;

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
        MonthlyStatsDTO stats = statisticsClient.getMonthlyStats(new MonthlyStatsRequest(request.getYear(), request.getMonth()));
        Map<Long, BigDecimal> actualByCategory = stats.getCategoryBreakdown().stream()
                .collect(Collectors.toMap(
                        MonthlyStatsDTO.CategorySummary::getCategoryId,
                        MonthlyStatsDTO.CategorySummary::getAmount));
        Map<Long, String> categoryNames = categoryClient.listByUser(new CategoryListRequest("EXPENSE")).stream()
                .collect(Collectors.toMap(CategoryDTO::getId, CategoryDTO::getName));
        return ApiResponse.success(
                budgetService.listBudget(userId, request.getYear(), request.getMonth(),
                        actualByCategory, categoryNames));
    }
}
