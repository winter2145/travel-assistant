package com.xin.aiagentbackend.conversation;

import com.xin.aiagentbackend.api.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConversationService {
    private static final int MAX_PAGE_SIZE = 100;
    private final ConversationRepository repository;

    public ConversationService(ConversationRepository repository) { this.repository = repository; }

    @Transactional
    public ConversationDto create(CreateConversationRequest request) {
        String title = request == null || request.title() == null || request.title().isBlank()
                ? "新旅行计划" : request.title().strip();
        if (title.length() > 200) throw ApiException.badRequest("会话标题不能超过200个字符");
        return repository.create(title);
    }

    public PageDto<ConversationDto> list(int page, int pageSize) {
        int safePage = page(page);
        int safeSize = pageSize(pageSize);
        return new PageDto<>(repository.list(safeSize, (safePage - 1) * safeSize), repository.count(), safePage, safeSize);
    }

    public ConversationDto get(String id) { return require(id); }

    @Transactional
    public ConversationDto update(String id, UpdateConversationRequest request) {
        require(id);
        if (!repository.updateTitle(id, request.title().strip())) throw ApiException.notFound("会话不存在");
        return require(id);
    }

    @Transactional
    public void delete(String id) {
        if (!repository.delete(id)) throw ApiException.notFound("会话不存在");
    }

    public PageDto<MessageDto> messages(String id, int page, int pageSize) {
        require(id);
        int safePage = page(page);
        int safeSize = pageSize(pageSize);
        return new PageDto<>(repository.messages(id, safeSize, (safePage - 1) * safeSize),
                repository.messageCount(id), safePage, safeSize);
    }

    public List<MessageDto> recentMessages(String id, int limit) {
        require(id);
        return repository.recentMessages(id, Math.min(Math.max(limit, 1), 40));
    }

    @Transactional
    public void appendUserMessage(String id, String content) {
        require(id);
        repository.appendMessage(id, "user", content);
    }

    @Transactional
    public void appendAssistantMessage(String id, String content) {
        require(id);
        if (content != null && !content.isBlank()) repository.appendMessage(id, "assistant", content);
    }

    private ConversationDto require(String id) {
        if (id == null || id.isBlank()) throw ApiException.badRequest("conversationId不能为空");
        return repository.get(id).orElseThrow(() -> ApiException.notFound("会话不存在"));
    }

    private int page(int value) {
        if (value < 1) throw ApiException.badRequest("page必须大于等于1");
        return value;
    }

    private int pageSize(int value) {
        if (value < 1 || value > MAX_PAGE_SIZE) throw ApiException.badRequest("pageSize必须在1到100之间");
        return value;
    }
}
