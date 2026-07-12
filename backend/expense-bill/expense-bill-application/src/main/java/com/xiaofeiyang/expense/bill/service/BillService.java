package com.xiaofeiyang.expense.bill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaofeiyang.expense.category.api.client.CategoryClient;
import com.xiaofeiyang.expense.category.api.dto.CategoryDTO;
import com.xiaofeiyang.expense.category.api.dto.CategoryDetailRequest;
import com.xiaofeiyang.expense.framework.exception.BusinessException;
import com.xiaofeiyang.expense.framework.exception.ErrorCode;
import com.xiaofeiyang.expense.bill.dto.BillRequest;
import com.xiaofeiyang.expense.bill.dto.BillVO;
import com.xiaofeiyang.expense.bill.entity.Bill;
import com.xiaofeiyang.expense.bill.mapper.BillMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillMapper billMapper;
    private final CategoryClient categoryClient;

    // ── 写操作 ──

    public void create(BillRequest request, Long userId) {
        Bill bill = new Bill();
        bill.setUserId(userId);
        bill.setCategoryId(request.getCategoryId());
        bill.setAmount(request.getAmount());
        bill.setType(request.getType());
        bill.setDescription(request.getDescription());
        bill.setBillDate(request.getBillDate());
        billMapper.insert(bill);
    }

    public void update(Long id, BillRequest request, Long userId) {
        Bill bill = findById(id, userId);
        bill.setCategoryId(request.getCategoryId());
        bill.setAmount(request.getAmount());
        bill.setType(request.getType());
        bill.setDescription(request.getDescription());
        bill.setBillDate(request.getBillDate());
        billMapper.updateById(bill);
    }

    public void delete(Long id, Long userId) {
        findById(id, userId);
        billMapper.deleteById(id);
    }

    // ── 读操作（含分类名） ──

    public Page<BillVO> page(Long userId, String type, Long categoryId,
                               LocalDate startDate, LocalDate endDate,
                               int page, int size) {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<Bill>()
                .eq(Bill::getUserId, userId);
        if (type != null && !type.isBlank()) wrapper.eq(Bill::getType, type);
        if (categoryId != null) wrapper.eq(Bill::getCategoryId, categoryId);
        if (startDate != null) wrapper.ge(Bill::getBillDate, startDate);
        if (endDate != null) wrapper.le(Bill::getBillDate, endDate);
        wrapper.orderByDesc(Bill::getBillDate)
               .orderByDesc(Bill::getCreatedTime);

        Page<Bill> billPage = billMapper.selectPage(Page.of(page, size), wrapper);

        Map<Long, String> catNames = billPage.getRecords().stream()
                .map(Bill::getCategoryId)
                .distinct()
                .collect(Collectors.toMap(cid -> cid, this::getCategoryName));

        Page<BillVO> voPage = new Page<>(page, size, billPage.getTotal());
        voPage.setRecords(billPage.getRecords().stream()
                .map(b -> toVO(b, catNames.getOrDefault(b.getCategoryId(), "未知")))
                .toList());
        return voPage;
    }

    public BillVO getById(Long id, Long userId) {
        Bill bill = findById(id, userId);
        return toVO(bill, getCategoryName(bill.getCategoryId()));
    }

    /**
     * Query all bills matching filters without pagination — used for CSV export.
     */
    public List<BillVO> queryAll(Long userId, String type, Long categoryId,
                                  LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<Bill>()
                .eq(Bill::getUserId, userId);
        if (type != null && !type.isBlank()) wrapper.eq(Bill::getType, type);
        if (categoryId != null) wrapper.eq(Bill::getCategoryId, categoryId);
        if (startDate != null) wrapper.ge(Bill::getBillDate, startDate);
        if (endDate != null) wrapper.le(Bill::getBillDate, endDate);
        wrapper.orderByDesc(Bill::getBillDate)
               .orderByDesc(Bill::getCreatedTime);

        List<Bill> bills = billMapper.selectList(wrapper);

        Map<Long, String> catNames = bills.stream()
                .map(Bill::getCategoryId)
                .distinct()
                .collect(Collectors.toMap(cid -> cid, this::getCategoryName));

        return bills.stream()
                .map(b -> toVO(b, catNames.getOrDefault(b.getCategoryId(), "未知")))
                .toList();
    }

    /**
     * Generate CSV file content for bill export.
     */
    public byte[] exportCsv(Long userId, String type, Long categoryId,
                             LocalDate startDate, LocalDate endDate) {
        List<BillVO> bills = queryAll(userId, type, categoryId, startDate, endDate);
        StringBuilder sb = new StringBuilder();
        sb.append('﻿'); // BOM for Excel UTF-8 compatibility
        sb.append("日期,类型,分类,描述,金额\n");
        for (BillVO b : bills) {
            sb.append(b.getBillDate()).append(',');
            sb.append("INCOME".equals(b.getType()) ? "收入" : "支出").append(',');
            sb.append(escapeCsv(b.getCategoryName())).append(',');
            sb.append(escapeCsv(b.getDescription() != null ? b.getDescription() : "")).append(',');
            sb.append(b.getAmount().toPlainString()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ── 内部 ──

    public Bill findById(Long id, Long userId) {
        Bill bill = billMapper.selectById(id);
        if (bill == null) throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        if (!bill.getUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        return bill;
    }

    private String getCategoryName(Long categoryId) {
        return categoryClient.findById(new CategoryDetailRequest(categoryId)).getName();
    }

    private BillVO toVO(Bill bill, String categoryName) {
        return BillVO.builder()
                .id(bill.getId()).categoryId(bill.getCategoryId()).categoryName(categoryName)
                .amount(bill.getAmount()).type(bill.getType()).description(bill.getDescription())
                .billDate(bill.getBillDate())
                .createdTime(bill.getCreatedTime()).updatedTime(bill.getUpdatedTime())
                .build();
    }

    // ---- Internal Feign methods (raw entities, no category name resolution) ----

    public List<Bill> queryByDateRange(Long userId, LocalDate start, LocalDate end) {
        return billMapper.selectList(new LambdaQueryWrapper<Bill>()
                .eq(Bill::getUserId, userId)
                .between(Bill::getBillDate, start, end));
    }

    public List<Bill> queryByMonth(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return queryByDateRange(userId, start, end);
    }
}
