package com.example.expense.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.expense.bill.entity.Bill;
import com.example.expense.bill.mapper.BillMapper;
import com.example.expense.common.enums.BillType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final BillMapper billMapper;

    public BigDecimal sumByType(Long userId, int year, int month, BillType type) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<Bill> bills = billMapper.selectList(
                new LambdaQueryWrapper<Bill>()
                        .eq(Bill::getUserId, userId)
                        .eq(Bill::getType, type.name())
                        .between(Bill::getBillDate, start, end));

        return bills.stream()
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<Long, BigDecimal> sumByCategory(Long userId, int year, int month, BillType type) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<Bill> bills = billMapper.selectList(
                new LambdaQueryWrapper<Bill>()
                        .eq(Bill::getUserId, userId)
                        .eq(Bill::getType, type.name())
                        .between(Bill::getBillDate, start, end));

        return bills.stream()
                .collect(Collectors.groupingBy(
                        Bill::getCategoryId,
                        Collectors.reducing(BigDecimal.ZERO, Bill::getAmount, BigDecimal::add)));
    }

    /**
     * Query all bills in a date range and group by year/month for trend data.
     */
    public List<Bill> queryByDateRange(Long userId, LocalDate start, LocalDate end) {
        return billMapper.selectList(
                new LambdaQueryWrapper<Bill>()
                        .eq(Bill::getUserId, userId)
                        .between(Bill::getBillDate, start, end));
    }

    /**
     * Query all bills for a single month for daily breakdown.
     */
    public List<Bill> queryByMonth(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return billMapper.selectList(
                new LambdaQueryWrapper<Bill>()
                        .eq(Bill::getUserId, userId)
                        .between(Bill::getBillDate, start, end));
    }
}
