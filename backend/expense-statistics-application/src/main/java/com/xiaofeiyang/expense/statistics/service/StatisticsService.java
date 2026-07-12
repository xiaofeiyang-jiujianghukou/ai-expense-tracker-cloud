package com.xiaofeiyang.expense.statistics.service;

import com.xiaofeiyang.expense.bill.api.client.BillClient;
import com.xiaofeiyang.expense.bill.api.dto.BillQueryMonthRequest;
import com.xiaofeiyang.expense.bill.api.dto.BillQueryRangeRequest;
import com.xiaofeiyang.expense.bill.api.dto.BillDTO;
import com.xiaofeiyang.expense.framework.enums.BillType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final BillClient billClient;

    public BigDecimal sumByType(Long userId, int year, int month, BillType type) {
        List<BillDTO> bills = fetchMonth(userId, year, month);
        return bills.stream()
                .filter(b -> type.name().equals(b.getType()))
                .map(BillDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<Long, BigDecimal> sumByCategory(Long userId, int year, int month, BillType type) {
        List<BillDTO> bills = fetchMonth(userId, year, month);
        return bills.stream()
                .filter(b -> type.name().equals(b.getType()))
                .collect(Collectors.groupingBy(
                        BillDTO::getCategoryId,
                        Collectors.reducing(BigDecimal.ZERO, BillDTO::getAmount, BigDecimal::add)));
    }

    /** Fetch bills in a date range — calls bill-service via Feign. */
    public List<BillDTO> queryByDateRange(Long userId, LocalDate start, LocalDate end) {
        return billClient.queryByDateRange(new BillQueryRangeRequest(start.toString(), end.toString()));
    }

    /** Fetch bills for a single month. */
    public List<BillDTO> queryByMonth(Long userId, int year, int month) {
        return billClient.queryByMonth(new BillQueryMonthRequest(year, month));
    }

    /** Internal: fetch and filter locally (single Feign call, filter in-memory). */
    private List<BillDTO> fetchMonth(Long userId, int year, int month) {
        return billClient.queryByMonth(new BillQueryMonthRequest(year, month));
    }
}
