package com.xin.aiagentbackend.conversation.service.impl;

import com.xin.aiagentbackend.api.ApiException;
import com.xin.aiagentbackend.conversation.dto.ConversationDto;
import com.xin.aiagentbackend.conversation.dto.CreateConversationRequest;
import com.xin.aiagentbackend.conversation.dto.MessageDto;
import com.xin.aiagentbackend.conversation.dto.PageDto;
import com.xin.aiagentbackend.conversation.dto.UpdateConversationRequest;
import com.xin.aiagentbackend.conversation.repository.ConversationRepository;
import com.xin.aiagentbackend.conversation.service.ConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 会话业务服务实现类（ServiceImpl）。
 * <p>实现 {@link ConversationService} 接口，承载会话与消息的业务规则：
 * 参数校验、标题默认值/长度限制、分页参数约束以及对外部调用的会话存在性校验。</p>
 */
@Service
public class ConversationServiceImpl implements ConversationService {
    /** 分页查询允许的最大每页条数 */
    private static final int MAX_PAGE_SIZE = 100;
    private final ConversationRepository repository;

    public ConversationServiceImpl(ConversationRepository repository) { this.repository = repository; }

    /** 创建会话：标题为空时使用默认标题，超长时报参数错误 */
    @Transactional
    public ConversationDto create(CreateConversationRequest request) {
        String title = request == null || request.title() == null || request.title().isBlank()
                ? "新旅行计划" : request.title().strip();
        if (title.length() > 200) throw ApiException.badRequest("会话标题不能超过200个字符");
        return repository.create(title);
    }

    /** 分页查询会话列表，先对页码与每页条数做合法性校验 */
    public PageDto<ConversationDto> list(int page, int pageSize) {
        int safePage = page(page);
        int safeSize = pageSize(pageSize);
        return new PageDto<>(repository.list(safeSize, (safePage - 1) * safeSize), repository.count(), safePage, safeSize);
    }

    /** 查询会话详情，会话不存在时抛出 404 异常 */
    public ConversationDto get(String id) { return require(id); }

    /** 更新会话标题：先校验会话存在，再执行更新 */
    @Transactional
    public ConversationDto update(String id, UpdateConversationRequest request) {
        require(id);
        if (!repository.updateTitle(id, request.title().strip())) throw ApiException.notFound("会话不存在");
        return require(id);
    }

    /** 删除会话，会话不存在时抛出 404 异常 */
    @Transactional
    public void delete(String id) {
        if (!repository.delete(id)) throw ApiException.notFound("会话不存在");
    }

    /** 分页查询会话下的消息，先校验会话存在与分页参数 */
    public PageDto<MessageDto> messages(String id, int page, int pageSize) {
        require(id);
        int safePage = page(page);
        int safeSize = pageSize(pageSize);
        return new PageDto<>(repository.messages(id, safeSize, (safePage - 1) * safeSize),
                repository.messageCount(id), safePage, safeSize);
    }

    /** 查询会话最近 N 条消息（供 AI 对话上下文恢复使用），条数限制在 1~40 之间 */
    public List<MessageDto> recentMessages(String id, int limit) {
        require(id);
        return repository.recentMessages(id, Math.min(Math.max(limit, 1), 40));
    }

    /** 追加一条用户消息 */
    @Transactional
    public void appendUserMessage(String id, String content) {
        require(id);
        repository.appendMessage(id, "user", content);
    }

    /** 追加一条助手消息，内容为空时跳过写入 */
    @Transactional
    public void appendAssistantMessage(String id, String content) {
        require(id);
        if (content != null && !content.isBlank()) repository.appendMessage(id, "assistant", content);
    }

    /** 校验会话 ID 并返回会话详情，为空或不存在时抛出对应异常 */
    private ConversationDto require(String id) {
        if (id == null || id.isBlank()) throw ApiException.badRequest("conversationId不能为空");
        return repository.get(id).orElseThrow(() -> ApiException.notFound("会话不存在"));
    }

    /** 校验页码：必须大于等于 1 */
    private int page(int value) {
        if (value < 1) throw ApiException.badRequest("page必须大于等于1");
        return value;
    }

    /** 校验每页条数：必须在 1 到 100 之间 */
    private int pageSize(int value) {
        if (value < 1 || value > MAX_PAGE_SIZE) throw ApiException.badRequest("pageSize必须在1到100之间");
        return value;
    }
}
