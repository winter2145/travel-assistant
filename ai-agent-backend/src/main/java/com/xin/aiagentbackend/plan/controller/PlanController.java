package com.xin.aiagentbackend.plan.controller;

import com.xin.aiagentbackend.api.ApiResponse;
import com.xin.aiagentbackend.plan.dto.PdfExportRequest;
import com.xin.aiagentbackend.plan.dto.PlanDto;
import com.xin.aiagentbackend.plan.dto.PlanPageDto;
import com.xin.aiagentbackend.plan.dto.PlanRequest;
import com.xin.aiagentbackend.plan.dto.UpdatePlanRequest;
import com.xin.aiagentbackend.plan.service.PdfExportService;
import com.xin.aiagentbackend.plan.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;

/**
 * 旅行计划控制器（Controller）。
 * <p>对外暴露旅行计划相关的 REST 接口（路径前缀 /plans），
 * 涵盖计划的创建、列表、详情、按会话查询、更新、删除、复制以及 PDF 攻略导出。</p>
 */
@RestController
@RequestMapping("/plans")
public class PlanController {
    private final PlanService service;
    private final PdfExportService pdfExportService;

    public PlanController(PlanService service, PdfExportService pdfExportService) { this.service = service; this.pdfExportService = pdfExportService; }

    /** 导出旅行计划 PDF 攻略（附件下载） */
    @PostMapping(value = "/{id}/pdf-exports", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable String id, @Valid @RequestBody PdfExportRequest request) {
        PlanDto plan = service.get(id);
        byte[] pdf = pdfExportService.generate(plan, request);
        String filename = plan.city().replaceAll("[^\\p{IsHan}\\p{Alnum}._-]", "_") + "_旅行攻略.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(pdf);
    }

    /** 创建旅行计划 */
    @PostMapping
    public ApiResponse<PlanDto> create(@Valid @RequestBody PlanRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    /** 分页查询旅行计划列表，支持关键词过滤 */
    @GetMapping
    public ApiResponse<PlanPageDto> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.list(page, pageSize, keyword));
    }

    /** 查询旅行计划详情 */
    @GetMapping("/{id}")
    public ApiResponse<PlanDto> get(@PathVariable String id) { return ApiResponse.ok(service.get(id)); }

    /** 按会话标识查询最近保存的旅行计划 */
    @GetMapping("/by-conversation/{conversationId}")
    public ApiResponse<PlanDto> getByConversation(@PathVariable String conversationId) {
        return ApiResponse.ok(service.getByConversation(conversationId));
    }

    /** 更新旅行计划（乐观锁并发控制） */
    @PutMapping("/{id}")
    public ApiResponse<PlanDto> update(@PathVariable String id, @Valid @RequestBody UpdatePlanRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    /** 删除旅行计划 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    /** 复制旅行计划为副本 */
    @PostMapping("/{id}/duplicate")
    public ApiResponse<PlanDto> duplicate(@PathVariable String id) {
        return ApiResponse.ok(service.duplicate(id));
    }
}
