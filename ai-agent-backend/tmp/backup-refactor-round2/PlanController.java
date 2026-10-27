package com.xin.aiagentbackend.controller;

import com.xin.aiagentbackend.api.ApiResponse;
import com.xin.aiagentbackend.plan.*;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/plans")
public class PlanController {
    private final PlanService service;
    private final PdfExportService pdfExportService;

    public PlanController(PlanService service, PdfExportService pdfExportService) { this.service = service; this.pdfExportService = pdfExportService; }

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

    @PostMapping
    public ApiResponse<PlanDto> create(@Valid @RequestBody PlanRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @GetMapping
    public ApiResponse<PlanPageDto> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.list(page, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlanDto> get(@PathVariable String id) { return ApiResponse.ok(service.get(id)); }

    @GetMapping("/by-conversation/{conversationId}")
    public ApiResponse<PlanDto> getByConversation(@PathVariable String conversationId) {
        return ApiResponse.ok(service.getByConversation(conversationId));
    }

    @PutMapping("/{id}")
    public ApiResponse<PlanDto> update(@PathVariable String id, @Valid @RequestBody UpdatePlanRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/duplicate")
    public ApiResponse<PlanDto> duplicate(@PathVariable String id) {
        return ApiResponse.ok(service.duplicate(id));
    }
}
