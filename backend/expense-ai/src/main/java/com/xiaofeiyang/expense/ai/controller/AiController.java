package com.xiaofeiyang.expense.ai.controller;

import com.xiaofeiyang.expense.ai.dto.AnalysisRequest;
import com.xiaofeiyang.expense.ai.dto.AnalysisResponse;
import com.xiaofeiyang.expense.ai.dto.BudgetAdviceResponse;
import com.xiaofeiyang.expense.ai.dto.CategorizeRequest;
import com.xiaofeiyang.expense.ai.dto.CategorizeResponse;
import com.xiaofeiyang.expense.ai.dto.ReportResponse;
import com.xiaofeiyang.expense.ai.service.AiAnalysisService;
import com.xiaofeiyang.expense.ai.service.AiCategoryService;
import com.xiaofeiyang.expense.ai.service.AiReportService;
import com.xiaofeiyang.expense.ai.service.BudgetAdviceService;
import com.xiaofeiyang.expense.framework.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import com.xiaofeiyang.expense.framework.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiCategoryService aiCategoryService;
    private final AiAnalysisService aiAnalysisService;
    private final AiReportService aiReportService;
    private final BudgetAdviceService budgetAdviceService;
    private final Executor ttlExecutor;

    @PostMapping("/categorize")
    public ApiResponse<CategorizeResponse> categorize(@Valid @RequestBody CategorizeRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(aiCategoryService.categorize(request, userId));
    }

    @PostMapping("/analysis")
    public ApiResponse<AnalysisResponse> analysis(@Valid @RequestBody AnalysisRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(aiAnalysisService.analyze(request, userId));
    }

    @PostMapping("/report")
    public ApiResponse<ReportResponse> report(@Valid @RequestBody AnalysisRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        String report = aiReportService.generateReport(request.getYear(), request.getMonth(), userId);
        return ApiResponse.success(ReportResponse.builder()
                .year(request.getYear())
                .month(request.getMonth())
                .report(report)
                .build());
    }

    @PostMapping(value = "/analysis/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analysisStream(@Valid @RequestBody AnalysisRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        SseEmitter emitter = new SseEmitter(120_000L);
        ttlExecutor.execute(() -> {
            try {
                aiAnalysisService.generateAnalysisStream(
                        request.getYear(), request.getMonth(), userId,
                        line -> {
                            try {
                                emitter.send(SseEmitter.event().name("line").data(line));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @PostMapping(value = "/report/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter reportStream(@Valid @RequestBody AnalysisRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        SseEmitter emitter = new SseEmitter(120_000L);
        ttlExecutor.execute(() -> {
            try {
                aiReportService.generateReportStream(
                        request.getYear(), request.getMonth(), userId, request.isForceRefresh(),
                        chunk -> {
                            try {
                                emitter.send(SseEmitter.event().name("chunk").data(chunk));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @PostMapping("/budget-advice")
    public ApiResponse<BudgetAdviceResponse> budgetAdvice(@Valid @RequestBody AnalysisRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(budgetAdviceService.generate(request, userId));
    }

    @PostMapping("/anomaly")
    public ApiResponse<AnalysisResponse> anomaly(@Valid @RequestBody AnalysisRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(aiAnalysisService.detectAnomaly(request, userId));
    }
}
