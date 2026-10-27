package com.xin.aiagentbackend.controller;

import com.xin.aiagentbackend.api.ApiResponse;
import com.xin.aiagentbackend.conversation.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/conversations")
public class ConversationController {
    private final ConversationService service;

    public ConversationController(ConversationService service) { this.service = service; }

    @PostMapping
    public ApiResponse<ConversationDto> create(@Valid @RequestBody(required = false) CreateConversationRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @GetMapping
    public ApiResponse<PageDto<ConversationDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.list(page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationDto> get(@PathVariable String id) { return ApiResponse.ok(service.get(id)); }

    @PatchMapping("/{id}")
    public ApiResponse<ConversationDto> update(@PathVariable String id,
            @Valid @RequestBody UpdateConversationRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<PageDto<MessageDto>> messages(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        return ApiResponse.ok(service.messages(id, page, pageSize));
    }
}
