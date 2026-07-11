package com.example.expense.ai.service;

import com.example.expense.ai.client.LlmClient;
import com.example.expense.ai.dto.AnalysisRequest;
import com.example.expense.ai.dto.AnalysisResponse;
import com.example.expense.common.exception.BusinessException;
import com.example.expense.common.exception.ErrorCode;
import com.example.expense.statistics.manager.StatisticsManager;
import com.example.expense.statistics.dto.MonthlyStatsVO;
import com.example.expense.statistics.dto.TrendVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final StatisticsManager statisticsManager;
    private final LlmClient llmClient;
    private final AiCacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisResponse analyze(AnalysisRequest request, Long userId) {
        int year = request.getYear();
        int month = request.getMonth();

        // Check cache first (unless force refresh)
        if (!request.isForceRefresh()) {
            String cached = cacheService.getAnalysis(userId, year, month);
            if (cached != null) {
                return buildResponse(year, month, parseCachedInsights(cached));
            }
        }

        // 1. Get monthly stats
        MonthlyStatsVO stats = statisticsManager.getMonthlyStats(userId, year, month);

        // 2. Build category breakdown text
        String categoryText = stats.getCategoryBreakdown().stream()
                .map(c -> String.format("%s: ¥%s (%s%%)",
                        c.getCategoryName(), c.getAmount().toPlainString(), c.getPercentage()))
                .collect(Collectors.joining("\n"));

        // 3. Build prompts
        String systemPrompt = """
                You are a personal finance advisor. Analyze the user's monthly spending data
                and provide 3-5 concise, actionable insights. Each insight should be no more
                than 50 Chinese characters. Focus on:
                - Spending patterns and trends
                - Areas where spending seems high
                - Practical saving suggestions
                Respond with ONLY numbered insights, one per line, no other text.""";

        String userMessage = String.format("""
                Monthly finance data for %d/%d:
                - Total Income: ¥%s
                - Total Expense: ¥%s
                - Balance: ¥%s
                - Expense by category:
                %s""",
                year, month,
                stats.getIncome().toPlainString(),
                stats.getExpense().toPlainString(),
                stats.getBalance().toPlainString(),
                categoryText);

        try {
            String response = llmClient.chat(systemPrompt, userMessage);
            List<String> insights = parseInsights(response);

            // Cache the result
            cacheService.putAnalysis(userId, year, month, serializeInsights(insights));

            return buildResponse(year, month, insights);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
        }
    }

    /**
     * Streaming analysis for real-time UI feedback.
     * Force-refreshes and streams each line as a separate SSE event.
     */
    public void generateAnalysisStream(int year, int month, Long userId, java.util.function.Consumer<String> onLine) {
        MonthlyStatsVO stats = statisticsManager.getMonthlyStats(userId, year, month);
        String categoryText = stats.getCategoryBreakdown().stream()
                .map(c -> String.format("%s: ¥%s (%s%%)",
                        c.getCategoryName(), c.getAmount().toPlainString(), c.getPercentage()))
                .collect(java.util.stream.Collectors.joining("\n"));

        String systemPrompt = "You are a personal finance advisor. Analyze the spending data and provide 3-5 concise insights. Each insight on a new line, no more than 50 Chinese characters each. No numbering, no markdown.";
        String userMessage = String.format("Monthly data %d/%d: Income ¥%s, Expense ¥%s, Balance ¥%s. Categories:\n%s",
                year, month, stats.getIncome().toPlainString(), stats.getExpense().toPlainString(), stats.getBalance().toPlainString(), categoryText);

        StringBuilder full = new StringBuilder();
        llmClient.chatStream(systemPrompt, userMessage, chunk -> {
            full.append(chunk);
            onLine.accept(chunk);
        });
        // Cache the full result as serialized insights
        cacheService.putAnalysis(userId, year, month, serializeInsights(parseInsights(full.toString())));
    }

    public AnalysisResponse detectAnomaly(AnalysisRequest request, Long userId) {
        int year = request.getYear();
        int month = request.getMonth();

        MonthlyStatsVO current = statisticsManager.getMonthlyStats(userId, year, month);
        String currentText = current.getCategoryBreakdown().stream()
                .map(c -> String.format("%s: ¥%s", c.getCategoryName(), c.getAmount().toPlainString()))
                .collect(java.util.stream.Collectors.joining(", "));

        // Get previous month for comparison
        int prevYear = month == 1 ? year - 1 : year;
        int prevMonth = month == 1 ? 12 : month - 1;
        String prevText = "";
        try {
            MonthlyStatsVO previous = statisticsManager.getMonthlyStats(userId, prevYear, prevMonth);
            prevText = previous.getCategoryBreakdown().stream()
                    .map(c -> String.format("%s: ¥%s", c.getCategoryName(), c.getAmount().toPlainString()))
                    .collect(java.util.stream.Collectors.joining(", "));
        } catch (Exception ignored) { /* no previous data */ }

        String systemPrompt = """
                You are a financial anomaly detector. Compare current month spending
                with previous month (if available). Flag categories with unusual spikes
                (>50% increase) or unexpected drops. Be specific with numbers.
                If everything looks normal, say so. Respond with numbered observations.""";
        String userMessage = String.format("""
                Current month (%d/%d): %s
                Previous month: %s""",
                year, month, currentText,
                prevText.isEmpty() ? "No data" : prevText);

        try {
            String response = llmClient.chat(systemPrompt, userMessage);
            return buildResponse(year, month, parseInsights(response));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
        }
    }

    private AnalysisResponse buildResponse(int year, int month, List<String> insights) {
        return AnalysisResponse.builder()
                .year(year).month(month).insights(insights)
                .build();
    }

    private List<String> parseInsights(String llmResponse) {
        List<String> insights = new ArrayList<>();
        for (String line : llmResponse.split("\n")) {
            String trimmed = line.trim();
            trimmed = trimmed.replaceFirst("^\\d+[.、．)\\s]+", "").trim();
            if (!trimmed.isEmpty()) {
                insights.add(trimmed);
            }
        }
        if (insights.isEmpty()) {
            insights.add(llmResponse.trim());
        }
        return insights;
    }

    private String serializeInsights(List<String> insights) {
        try {
            return objectMapper.writeValueAsString(insights);
        } catch (JsonProcessingException e) {
            return String.join("|||", insights);
        }
    }

    private List<String> parseCachedInsights(String cached) {
        try {
            return objectMapper.readValue(cached, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of(cached.split("\\|\\|\\|"));
        }
    }
}
