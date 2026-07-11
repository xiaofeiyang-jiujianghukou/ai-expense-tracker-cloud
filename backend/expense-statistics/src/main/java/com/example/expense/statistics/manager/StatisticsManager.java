package com.example.expense.statistics.manager;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.AbstractColumnWidthStyleStrategy;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import com.example.expense.category.service.CategoryService;
import com.example.expense.common.exception.BusinessException;
import com.example.expense.statistics.dto.MonthlyStatsVO;
import com.example.expense.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.example.expense.bill.entity.Bill;
import com.example.expense.common.enums.BillType;
import com.example.expense.statistics.dto.DailyVO;
import com.example.expense.statistics.dto.TrendVO;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
public class StatisticsManager {

    private static final String UNKNOWN_CATEGORY = "未知";

    private final StatisticsService statisticsService;
    private final CategoryService categoryService;

    private String getCategoryName(Long categoryId) {
        try {
            return categoryService.findById(categoryId).getName();
        } catch (BusinessException e) {
            return UNKNOWN_CATEGORY;
        }
    }

    public MonthlyStatsVO getMonthlyStats(Long userId, int year, int month) {
        BigDecimal income = statisticsService.sumByType(userId, year, month, BillType.INCOME);
        BigDecimal expense = statisticsService.sumByType(userId, year, month, BillType.EXPENSE);
        BigDecimal balance = income.subtract(expense);

        Map<Long, BigDecimal> categorySum = statisticsService.sumByCategory(userId, year, month, BillType.EXPENSE);

        List<MonthlyStatsVO.CategorySummary> breakdown = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : categorySum.entrySet()) {
            String categoryName = getCategoryName(entry.getKey());
            BigDecimal pct = expense.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(expense, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            breakdown.add(MonthlyStatsVO.CategorySummary.builder()
                    .categoryId(entry.getKey())
                    .categoryName(categoryName)
                    .amount(entry.getValue())
                    .percentage(pct)
                    .build());
        }
        breakdown.sort(Comparator.comparing(MonthlyStatsVO.CategorySummary::getAmount).reversed());

        return MonthlyStatsVO.builder()
                .year(year)
                .month(month)
                .income(income)
                .expense(expense)
                .balance(balance)
                .categoryBreakdown(breakdown)
                .build();
    }

    public TrendVO getMonthlyTrend(Long userId, int months) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(months - 1).withDayOfMonth(1);

        List<Bill> bills = statisticsService.queryByDateRange(userId, start, end);

        // Group by year-month: key = "2026-07"
        Map<String, BigDecimal[]> monthlyData = new LinkedHashMap<>();
        // Initialize all months in range
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String key = cursor.getYear() + "-" + String.format("%02d", cursor.getMonthValue());
            monthlyData.put(key, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            cursor = cursor.plusMonths(1);
        }

        for (Bill bill : bills) {
            String key = bill.getBillDate().getYear() + "-"
                    + String.format("%02d", bill.getBillDate().getMonthValue());
            BigDecimal[] data = monthlyData.get(key);
            if (data != null) {
                if (BillType.INCOME.name().equals(bill.getType())) {
                    data[0] = data[0].add(bill.getAmount());
                } else {
                    data[1] = data[1].add(bill.getAmount());
                }
            }
        }

        List<TrendVO.TrendPoint> points = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> entry : monthlyData.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            BigDecimal inc = entry.getValue()[0];
            BigDecimal exp = entry.getValue()[1];
            points.add(TrendVO.TrendPoint.builder()
                    .year(y).month(m)
                    .income(inc).expense(exp)
                    .balance(inc.subtract(exp))
                    .build());
        }

        return TrendVO.builder().points(points).build();
    }

    /**
     * Generate monthly statistics Excel file with professional formatting.
     */
    public byte[] exportExcel(Long userId, int year, int month) {
        MonthlyStatsVO stats = getMonthlyStats(userId, year, month);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Single ExcelWriter for all sheets
        ExcelWriter excelWriter = EasyExcel.write(out).build();

        // Sheet 1: Summary
        WriteSheet summarySheet = EasyExcel.writerSheet(0, "月度汇总")
                .head(List.of(List.of("年份"), List.of("月份"), List.of("收入 (¥)"), List.of("支出 (¥)"), List.of("结余 (¥)")))
                .registerWriteHandler(new PaddedColumnWidth(16, 38, 4))
                .registerWriteHandler(buildStyleStrategy())
                .build();
        excelWriter.write(List.of(List.of(
                String.valueOf(stats.getYear()), String.valueOf(stats.getMonth()),
                fmt(stats.getIncome()), fmt(stats.getExpense()), fmt(stats.getBalance()))), summarySheet);

        // Sheet 2: Category breakdown
        List<List<String>> detailRows = new ArrayList<>();
        for (MonthlyStatsVO.CategorySummary c : stats.getCategoryBreakdown()) {
            detailRows.add(List.of(c.getCategoryName(), fmt(c.getAmount()), c.getPercentage() + "%"));
        }
        WriteSheet detailSheet = EasyExcel.writerSheet(1, "分类明细")
                .head(List.of(List.of("分类"), List.of("金额 (¥)"), List.of("占比")))
                .registerWriteHandler(new PaddedColumnWidth(14, 38, 4))
                .registerWriteHandler(buildStyleStrategy())
                .build();
        excelWriter.write(detailRows, detailSheet);

        excelWriter.finish();
        return out.toByteArray();
    }

    /** Professional header + bordered content style. */
    private HorizontalCellStyleStrategy buildStyleStrategy() {
        // -- Head: white bold on dark blue, centered --
        WriteCellStyle head = new WriteCellStyle();
        head.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        head.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        WriteFont headFont = new WriteFont();
        headFont.setBold(true);
        headFont.setFontHeightInPoints((short) 12);
        headFont.setColor(IndexedColors.WHITE.getIndex());
        headFont.setFontName("Microsoft YaHei");
        head.setWriteFont(headFont);
        head.setHorizontalAlignment(HorizontalAlignment.CENTER);
        head.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(head, BorderStyle.THIN);

        // -- Content: centered, wrapped, thin borders --
        WriteCellStyle content = new WriteCellStyle();
        content.setWrapped(true);
        content.setHorizontalAlignment(HorizontalAlignment.CENTER);
        content.setVerticalAlignment(VerticalAlignment.CENTER);
        WriteFont contentFont = new WriteFont();
        contentFont.setFontHeightInPoints((short) 11);
        contentFont.setFontName("Microsoft YaHei");
        content.setWriteFont(contentFont);
        setBorder(content, BorderStyle.THIN);

        return new HorizontalCellStyleStrategy(head, content);
    }

    private void setBorder(WriteCellStyle style, BorderStyle bs) {
        style.setBorderLeft(bs);
        style.setBorderRight(bs);
        style.setBorderTop(bs);
        style.setBorderBottom(bs);
    }

    /**
     * Column-width strategy that auto-fits with padding, a minimum, and a cap.
     * CJK characters are counted as ~2 units for better accuracy.
     */
    private static class PaddedColumnWidth extends AbstractColumnWidthStyleStrategy {
        private static final int UNIT = 280; // approximate pixel-width per char
        private final int minChars;
        private final int maxChars;
        private final int padChars;
        private final Map<Integer, Integer> colMax = new HashMap<>();

        PaddedColumnWidth(int minChars, int maxChars, int padChars) {
            this.minChars = minChars;
            this.maxChars = maxChars;
            this.padChars = padChars;
        }

        @Override
        protected void setColumnWidth(WriteSheetHolder holder, List<WriteCellData<?>> list,
                                       Cell cell, Head head, Integer idx, Boolean isHead) {
            int ci = cell.getColumnIndex();
            int len = 0;
            String s = cell.getStringCellValue();
            if (s != null) {
                for (char c : s.toCharArray()) { len += (c > 127) ? 2 : 1; }
            }
            int target = Math.max(minChars, Math.min(maxChars, len + padChars));
            Integer prev = colMax.get(ci);
            if (prev == null || target > prev) {
                colMax.put(ci, target);
                holder.getSheet().setColumnWidth(ci, target * UNIT);
            }
        }
    }

    private String fmt(BigDecimal v) {
        return v != null ? v.setScale(2).toPlainString() : "0.00";
    }

    public DailyVO getDailyDistribution(Long userId, int year, int month) {
        List<Bill> bills = statisticsService.queryByMonth(userId, year, month);

        int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
        BigDecimal[] incomeByDay = new BigDecimal[daysInMonth + 1];
        BigDecimal[] expenseByDay = new BigDecimal[daysInMonth + 1];
        for (int i = 1; i <= daysInMonth; i++) {
            incomeByDay[i] = BigDecimal.ZERO;
            expenseByDay[i] = BigDecimal.ZERO;
        }

        for (Bill bill : bills) {
            int day = bill.getBillDate().getDayOfMonth();
            if (BillType.INCOME.name().equals(bill.getType())) {
                incomeByDay[day] = incomeByDay[day].add(bill.getAmount());
            } else {
                expenseByDay[day] = expenseByDay[day].add(bill.getAmount());
            }
        }

        List<DailyVO.DailyPoint> days = new ArrayList<>();
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(DailyVO.DailyPoint.builder()
                    .day(i)
                    .income(incomeByDay[i])
                    .expense(expenseByDay[i])
                    .build());
        }

        return DailyVO.builder().year(year).month(month).days(days).build();
    }
}
