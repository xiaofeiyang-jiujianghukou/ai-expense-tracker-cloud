package com.example.expense.bill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.expense.common.ApiResponse;
import com.example.expense.bill.dto.*;
import com.example.expense.bill.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.example.expense.common.util.SecurityUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        headers.setContentDispositionFormData("attachment", "bills.csv");
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
}
