package com.example.expense.ai.service;

import com.example.expense.ai.client.LlmClient;
import com.example.expense.common.exception.BusinessException;
import com.example.expense.common.exception.ErrorCode;
import com.example.expense.statistics.manager.StatisticsManager;
import com.example.expense.statistics.dto.MonthlyStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiReportService {

    private final StatisticsManager statisticsManager;
    private final LlmClient llmClient;
    private final AiCacheService cacheService;

    public String generateReport(int year, int month, Long userId) {
        String cached = cacheService.getReport(userId, year, month);
        if (cached != null) return cached;

        String[] prompts = buildPrompts(year, month, userId);
        try {
            String report = llmClient.chat(prompts[0], prompts[1]);
            cacheService.putReport(userId, year, month, report);
            return report;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_REPORT_FAILED);
        }
    }

    public void generateReportStream(int year, int month, Long userId, boolean forceRefresh, java.util.function.Consumer<String> onChunk) {
        if (!forceRefresh) {
            String cached = cacheService.getReport(userId, year, month);
            if (cached != null) {
                // Replay cached content in chunks for a similar streaming feel
                for (int i = 0; i < cached.length(); i += 5) {
                    int end = Math.min(i + 5, cached.length());
                    onChunk.accept(cached.substring(i, end));
                }
                return;
            }
        }

        String[] prompts = buildPrompts(year, month, userId);
        StringBuilder fullText = new StringBuilder();
        llmClient.chatStream(prompts[0], prompts[1], chunk -> {
            fullText.append(chunk);
            onChunk.accept(chunk);
        });
        cacheService.putReport(userId, year, month, fullText.toString());
    }

    private String[] buildPrompts(int year, int month, Long userId) {
        MonthlyStatsVO current = statisticsManager.getMonthlyStats(userId, year, month);
        MonthlyStatsVO previous = null;
        if (month == 1) {
            try { previous = statisticsManager.getMonthlyStats(userId, year - 1, 12); }
            catch (Exception ignored) {}
        } else {
            try { previous = statisticsManager.getMonthlyStats(userId, year, month - 1); }
            catch (Exception ignored) {}
        }

        String systemPrompt = """
                You are a professional financial analyst. Write a concise monthly financial report
                in Chinese (300-500 characters). Format your response in Markdown with the following structure:
                ## 总体评估
                (1 paragraph summary)
                ## 收支对比
                | 指标 | 上月 | 本月 | 环比 |
                |------|------|------|------|
                (fill the table with actual data)
                ## 分类观察
                (bullet points for each major category with emoji)
                ## 理财建议
                1. (numbered suggestion)
                2. (numbered suggestion)
                > (encouraging quote at the end)
                Be encouraging and constructive. Use accurate numbers from the provided data.""";

        String currentData = formatStats(current);
        String userMessage;
        if (previous != null) {
            userMessage = String.format("""
                    Current month (%d/%d):
                    %s

                    Previous month:
                    %s

                    Write a financial report comparing these two months.""",
                    year, month, currentData, formatStats(previous));
        } else {
            userMessage = String.format("""
                    Current month (%d/%d):
                    %s

                    Write a financial report for this month (no previous month data for comparison).""",
                    year, month, currentData);
        }
        return new String[]{systemPrompt, userMessage};
    }

    private String formatStats(MonthlyStatsVO stats) {
        String breakdown = stats.getCategoryBreakdown().stream()
                .map(c -> String.format("  - %s: ¥%s (%s%%)",
                        c.getCategoryName(), c.getAmount().toPlainString(), c.getPercentage()))
                .collect(Collectors.joining("\n"));

        return String.format("""
                Income: ¥%s | Expense: ¥%s | Balance: ¥%s
                Category breakdown:
                %s""",
                stats.getIncome().toPlainString(),
                stats.getExpense().toPlainString(),
                stats.getBalance().toPlainString(),
                breakdown);
    }
}
