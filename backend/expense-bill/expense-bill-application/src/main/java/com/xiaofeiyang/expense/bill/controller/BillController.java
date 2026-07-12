package com.xiaofeiyang.expense.bill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaofeiyang.expense.bill.api.dto.BillDTO;
import com.xiaofeiyang.expense.bill.api.dto.BillQueryMonthRequest;
import com.xiaofeiyang.expense.bill.api.dto.BillQueryRangeRequest;
import com.xiaofeiyang.expense.framework.ApiResponse;
import com.xiaofeiyang.expense.bill.dto.*;
import com.xiaofeiyang.expense.bill.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.xiaofeiyang.expense.framework.util.SecurityUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody BillRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        billService.create(request, userId);
        return ApiResponse.success();
    }

    @PostMapping("/list")
    public ApiResponse<Page<BillVO>> list(@RequestBody BillListRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(billService.page(userId, request.getType(),
                request.getCategoryId(), request.getStartDate(), request.getEndDate(),
                request.getPage(), request.getSize()));
    }

    @PostMapping("/export-csv")
    public ResponseEntity<byte[]> exportCsv(@RequestBody BillListRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        byte[] csvBytes = billService.exportCsv(userId, request.getType(),
                request.getCategoryId(), request.getStartDate(), request.getEndDate());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        return ResponseEntity.ok().headers(headers).body(csvBytes);
    }

    @GetMapping("/{id}")
    public ApiResponse<BillVO> getById(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(billService.getById(id, userId));
    }

    @PostMapping("/update")
    public ApiResponse<Void> update(@Valid @RequestBody BillUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        billService.update(request.getId(), request, userId);
        return ApiResponse.success();
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@Valid @RequestBody BillDeleteRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        billService.delete(request.getId(), userId);
        return ApiResponse.success();
    }

    // ---- Internal Feign endpoints (service-to-service) ----

    @PostMapping("/query-range")
    public List<BillDTO> queryByDateRange(@RequestBody BillQueryRangeRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return billService.queryByDateRange(userId, LocalDate.parse(request.getStart()), LocalDate.parse(request.getEnd()))
                .stream().map(this::toDTO).toList();
    }

    @PostMapping("/query-month")
    public List<BillDTO> queryByMonth(@RequestBody BillQueryMonthRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return billService.queryByMonth(userId, request.getYear(), request.getMonth())
                .stream().map(this::toDTO).toList();
    }

    private BillDTO toDTO(com.xiaofeiyang.expense.bill.entity.Bill b) {
        com.xiaofeiyang.expense.bill.api.dto.BillDTO dto = new com.xiaofeiyang.expense.bill.api.dto.BillDTO();
        dto.setId(b.getId());
        dto.setUserId(b.getUserId());
        dto.setCategoryId(b.getCategoryId());
        dto.setAmount(b.getAmount());
        dto.setType(b.getType());
        dto.setDescription(b.getDescription());
        dto.setBillDate(b.getBillDate());
        return dto;
    }
}
