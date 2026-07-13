package com.xiaofeiyang.expense.ai.service;

import com.xiaofeiyang.expense.ai.client.LlmClient;
import com.xiaofeiyang.expense.ai.dto.AnalysisRequest;
import com.xiaofeiyang.expense.ai.dto.BudgetAdviceItem;
import com.xiaofeiyang.expense.ai.dto.BudgetAdviceResponse;
import com.xiaofeiyang.expense.bill.api.dto.BillDTO;
import com.xiaofeiyang.expense.category.api.client.CategoryClient;
import com.xiaofeiyang.expense.category.api.dto.CategoryDTO;
import com.xiaofeiyang.expense.category.api.dto.CategoryListRequest;
import com.xiaofeiyang.expense.framework.enums.BillType;
import com.xiaofeiyang.expense.framework.exception.BusinessException;
import com.xiaofeiyang.expense.framework.exception.ErrorCode;
import com.xiaofeiyang.expense.bill.api.client.BillClient;
import com.xiaofeiyang.expense.bill.api.dto.BillQueryRangeRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetAdviceService {

    private final BillClient billClient;
    private final CategoryClient categoryClient;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public BudgetAdviceResponse generate(AnalysisRequest request, Long userId) {
        int year = request.getYear();
        int month = request.getMonth();

        // 1. Get all expense categories
        List<CategoryDTO> categories = categoryClient.listByUser(new CategoryListRequest(BillType.EXPENSE.name()));
        Map<Long, String> catNames = categories.stream()
                .collect(Collectors.toMap(CategoryDTO::getId, CategoryDTO::getName));

        // 2. Query bills for last 3 months
        LocalDate today = LocalDate.now();
        LocalDate threeMonthsAgo = today.minusMonths(3).withDayOfMonth(1);
        List<BillDTO> allBills = billClient.queryByDateRange(new BillQueryRangeRequest(threeMonthsAgo.toString(), today.toString()));

        List<BillDTO> expenseBills = allBills.stream()
                .filter(b -> BillType.EXPENSE.name().equals(b.getType()))
                .toList();

        // 3. Build per-category monthly data
        Map<Long, Map<String, BigDecimal>> catMonthlyData = new LinkedHashMap<>();
        for (BillDTO b : expenseBills) {
            String key = b.getBillDate().getYear() + "-" + b.getBillDate().getMonthValue();
            catMonthlyData.computeIfAbsent(b.getCategoryId(), k -> new LinkedHashMap<>())
                    .merge(key, b.getAmount(), BigDecimal::add);
        }

        // 4. Calculate recorded days in current month
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        LocalDate searchEnd = today.isBefore(monthEnd) ? today : monthEnd;

        Optional<LocalDate> firstDayOpt = expenseBills.stream()
                .filter(b -> b.getBillDate().getYear() == year && b.getBillDate().getMonthValue() == month)
                .map(BillDTO::getBillDate)
                .min(LocalDate::compareTo);

        int daysInMonth = monthStart.lengthOfMonth();
        long recordedDays = firstDayOpt
                .map(first -> ChronoUnit.DAYS.between(first, searchEnd) + 1)
                .orElse(0L);

        // 5. Build context for each category
        StringBuilder context = new StringBuilder();
        int validCatCount = 0;
        for (CategoryDTO cat : categories) {
            Map<String, BigDecimal> monthly = catMonthlyData.getOrDefault(cat.getId(), Map.of());
            if (monthly.isEmpty()) continue;
            validCatCount++;

            List<BigDecimal> monthValues = new ArrayList<>();
            List<String> monthLabels = new ArrayList<>();
            LocalDate cursor = threeMonthsAgo;
            while (!cursor.isAfter(searchEnd)) {
                String key = cursor.getYear() + "-" + cursor.getMonthValue();
                BigDecimal val = monthly.getOrDefault(key, BigDecimal.ZERO);
                monthValues.add(val);
                monthLabels.add(cursor.getYear() + "年" + cursor.getMonthValue() + "月");
                cursor = cursor.plusMonths(1);
            }

            List<BigDecimal> validValues = monthValues.stream()
                    .filter(v -> v.compareTo(BigDecimal.ZERO) > 0).toList();
            BigDecimal avg3Month = validValues.isEmpty() ? BigDecimal.ZERO
                    : validValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(validValues.size()), 2, RoundingMode.HALF_UP);

            String trend = "→";
            if (validValues.size() >= 2) {
                BigDecimal latest = validValues.get(validValues.size() - 1);
                BigDecimal prev = validValues.get(validValues.size() - 2);
                int cmp = latest.compareTo(prev);
                trend = cmp > 0 ? "↑增长" : cmp < 0 ? "↓下降" : "→持平";
            }

            String currentMonthKey = year + "-" + month;
            BigDecimal currentMonthSum = monthly.getOrDefault(currentMonthKey, BigDecimal.ZERO);
            BigDecimal dailyAvg = recordedDays > 0
                    ? currentMonthSum.divide(BigDecimal.valueOf(recordedDays), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal projectedFull = dailyAvg.multiply(BigDecimal.valueOf(daysInMonth)).setScale(0, RoundingMode.HALF_UP);

            context.append(String.format("""
                    %s: 近3月%s, 有效月均值¥%s, 趋势%s, 本月已记录%d天日均¥%s推算全月¥%s
                    """,
                    cat.getName(),
                    buildMonthDetail(monthLabels, monthValues),
                    avg3Month.toPlainString(),
                    trend,
                    recordedDays, dailyAvg.toPlainString(), projectedFull.toPlainString()));
        }

        if (validCatCount == 0) {
            return BudgetAdviceResponse.builder().year(year).month(month).items(List.of()).build();
        }

        // 6. Build prompt — ask for JSON
        String systemPrompt = "你是一位专业的个人理财顾问。根据数据分析每个分类的消费模式，给出月度预算建议。\n\n" +
                "规则：\n" +
                "- 优先参考近3月有效月均值，趋势上行上浮10-20%，趋势下行参考均值\n" +
                "- 仅1个月数据或波动很大时给出保守建议（日均×30×0.8）\n" +
                "- 金额取整数，避开250\n\n" +
                "【必须返回纯JSON数组，不要markdown代码块，不要其他文字】\n" +
                "格式：[{\"categoryName\":\"分类名\",\"amount\":金额}]\n" +
                "示例：[{\"categoryName\":\"餐饮\",\"amount\":1200},{\"categoryName\":\"交通\",\"amount\":150}]";

        String userMessage = String.format("""
                用户%d年%d月预算分析数据，本月已记录%d天（共%d天）：

                %s
                请返回JSON数组，每个分类一个对象。""",
                year, month, recordedDays, daysInMonth, context.toString());

        // 7. Call LLM and parse JSON
        try {
            String response = llmClient.chat(systemPrompt, userMessage);
            List<BudgetAdviceItem> items = parseJsonResponse(response, catNames);
            return BudgetAdviceResponse.builder()
                    .year(year).month(month)
                    .items(items)
                    .build();
        } catch (Exception e) {
            log.error("Budget advice failed", e);
            throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
        }
    }

    private String buildMonthDetail(List<String> labels, List<BigDecimal> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) sb.append(", ");
            BigDecimal v = values.get(i);
            if (v.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format("%s ¥%s(有效)", labels.get(i), v.toPlainString()));
            } else {
                sb.append(String.format("%s ¥0(未记录)", labels.get(i)));
            }
        }
        return sb.toString();
    }

    private List<BudgetAdviceItem> parseJsonResponse(String llmResponse, Map<Long, String> catNames) {
        // Extract JSON array from response (LLM may wrap in markdown or add extra text)
        String json = llmResponse.trim();
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }

        try {
            List<BudgetAdviceItem> items = objectMapper.readValue(json,
                    new TypeReference<List<BudgetAdviceItem>>() {});
            log.info("Parsed {} budget advice items from LLM response", items.size());
            return items;
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON response: {}", json, e);
            return List.of();
        }
    }
}
