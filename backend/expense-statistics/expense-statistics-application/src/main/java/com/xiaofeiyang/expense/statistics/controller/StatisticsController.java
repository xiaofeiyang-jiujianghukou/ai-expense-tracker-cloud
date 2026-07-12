package com.xiaofeiyang.expense.statistics.controller;

import com.xiaofeiyang.expense.framework.ApiResponse;
import com.xiaofeiyang.expense.statistics.api.dto.DailyDTO;
import com.xiaofeiyang.expense.statistics.api.dto.DailyDistributionRequest;
import com.xiaofeiyang.expense.statistics.api.dto.MonthlyStatsDTO;
import com.xiaofeiyang.expense.statistics.api.dto.MonthlyStatsRequest;
import com.xiaofeiyang.expense.statistics.api.dto.TrendDTO;
import com.xiaofeiyang.expense.statistics.api.dto.TrendRequest;
import com.xiaofeiyang.expense.statistics.dto.DailyVO;
import com.xiaofeiyang.expense.statistics.dto.MonthlyStatsVO;
import com.xiaofeiyang.expense.statistics.dto.TrendVO;
import com.xiaofeiyang.expense.statistics.manager.StatisticsManager;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.xiaofeiyang.expense.framework.util.SecurityUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsManager statisticsManager;

    @Data
    public static class MonthlyRequest {
        private int year;
        private int month;
    }

    @Data
    public static class TrendRequest {
        private int months = 6;
    }

    @PostMapping("/monthly")
    public ApiResponse<MonthlyStatsVO> monthly(@RequestBody MonthlyRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(
                statisticsManager.getMonthlyStats(userId, request.getYear(), request.getMonth()));
    }

    @PostMapping("/trend")
    public ApiResponse<TrendVO> trend(@RequestBody TrendRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(
                statisticsManager.getMonthlyTrend(userId, request.getMonths()));
    }

    @PostMapping("/daily")
    public ApiResponse<DailyVO> daily(@RequestBody MonthlyRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(
                statisticsManager.getDailyDistribution(userId, request.getYear(), request.getMonth()));
    }

    @PostMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(@RequestBody MonthlyRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        byte[] excelBytes = statisticsManager.exportExcel(userId, request.getYear(), request.getMonth());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    // ---- Internal Feign endpoints (service-to-service) ----

    @PostMapping("/feign/monthly")
    public MonthlyStatsDTO feignMonthly(@RequestBody MonthlyStatsRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        MonthlyStatsVO vo = statisticsManager.getMonthlyStats(userId, request.getYear(), request.getMonth());
        return toMonthlyStatsDTO(vo);
    }

    @PostMapping("/feign/trend")
    public TrendDTO feignTrend(@RequestBody TrendRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        TrendVO vo = statisticsManager.getMonthlyTrend(userId, request.getMonths());
        return toTrendDTO(vo);
    }

    @PostMapping("/feign/daily")
    public DailyDTO feignDaily(@RequestBody DailyDistributionRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        DailyVO vo = statisticsManager.getDailyDistribution(userId, request.getYear(), request.getMonth());
        return toDailyDTO(vo);
    }

    private MonthlyStatsDTO toMonthlyStatsDTO(MonthlyStatsVO vo) {
        return MonthlyStatsDTO.builder()
                .year(vo.getYear()).month(vo.getMonth())
                .income(vo.getIncome()).expense(vo.getExpense()).balance(vo.getBalance())
                .categoryBreakdown(vo.getCategoryBreakdown().stream()
                        .map(c -> MonthlyStatsDTO.CategorySummary.builder()
                                .categoryId(c.getCategoryId()).categoryName(c.getCategoryName())
                                .amount(c.getAmount()).percentage(c.getPercentage()).build())
                        .toList())
                .build();
    }

    private TrendDTO toTrendDTO(TrendVO vo) {
        return TrendDTO.builder()
                .points(vo.getPoints().stream()
                        .map(p -> TrendDTO.TrendPoint.builder()
                                .year(p.getYear()).month(p.getMonth())
                                .income(p.getIncome()).expense(p.getExpense())
                                .balance(p.getBalance()).build())
                        .toList())
                .build();
    }

    private DailyDTO toDailyDTO(DailyVO vo) {
        return DailyDTO.builder()
                .year(vo.getYear()).month(vo.getMonth())
                .days(vo.getDays().stream()
                        .map(d -> DailyDTO.DailyPoint.builder()
                                .day(d.getDay()).income(d.getIncome()).expense(d.getExpense()).build())
                        .toList())
                .build();
    }
}
