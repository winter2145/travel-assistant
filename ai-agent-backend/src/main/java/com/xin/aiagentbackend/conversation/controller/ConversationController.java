package com.xin.aiagentbackend.conversation.controller;

import com.xin.aiagentbackend.api.ApiResponse;
import com.xin.aiagentbackend.conversation.dto.ConversationDto;
import com.xin.aiagentbackend.conversation.dto.CreateConversationRequest;
import com.xin.aiagentbackend.conversation.dto.MessageDto;
import com.xin.aiagentbackend.conversation.dto.PageDto;
import com.xin.aiagentbackend.conversation.dto.UpdateConversationRequest;
import com.xin.aiagentbackend.conversation.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 会话管理控制器（Controller）。
 * <p>对外暴露会话相关的 REST 接口（路径前缀 /conversations），
 * 涵盖会话的创建、列表、详情、更新、删除以及消息分页查询。</p>
 */
@RestController
@RequestMapping("/conversations")
public class ConversationController {
    private final ConversationService service;

    public ConversationController(ConversationService service) { this.service = service; }

    /** 创建会话 */
    @PostMapping
    public ApiResponse<ConversationDto> create(@Valid @RequestBody(required = false) CreateConversationRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    /** 分页查询会话列表 */
    @GetMapping
    public ApiResponse<PageDto<ConversationDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.list(page, pageSize));
    }

    /** 查询会话详情 */
    @GetMapping("/{id}")
    public ApiResponse<ConversationDto> get(@PathVariable String id) { return ApiResponse.ok(service.get(id)); }

    /** 更新会话标题 */
    @PatchMapping("/{id}")
    public ApiResponse<ConversationDto> update(@PathVariable String id,
            @Valid @RequestBody UpdateConversationRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    /** 删除会话 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    /** 分页查询指定会话下的消息 */
    @GetMapping("/{id}/messages")
    public ApiResponse<PageDto<MessageDto>> messages(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        return ApiResponse.ok(service.messages(id, page, pageSize));
    }
}
