package com.xiaofeiyang.expense.statistics.api.client;

import com.xiaofeiyang.expense.statistics.api.dto.DailyDistributionRequest;
import com.xiaofeiyang.expense.statistics.api.dto.DailyDTO;
import com.xiaofeiyang.expense.statistics.api.dto.MonthlyStatsDTO;
import com.xiaofeiyang.expense.statistics.api.dto.MonthlyStatsRequest;
import com.xiaofeiyang.expense.statistics.api.dto.TrendDTO;
import com.xiaofeiyang.expense.statistics.api.dto.TrendRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "expense-statistics",
        contextId = "expense-statistics-api",
        path = "/api/statistics",
        url = "${statistics-service-api.url:}"
)
public interface StatisticsClient {

    @PostMapping("/feign/monthly")
    MonthlyStatsDTO getMonthlyStats(@RequestBody MonthlyStatsRequest request);

    @PostMapping("/feign/trend")
    TrendDTO getMonthlyTrend(@RequestBody TrendRequest request);

    @PostMapping("/feign/daily")
    DailyDTO getDailyDistribution(@RequestBody DailyDistributionRequest request);
}
