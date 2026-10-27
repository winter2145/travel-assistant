package com.xin.aiagentbackend.app.controller;

import com.xin.aiagentbackend.agent.Manus;
import com.xin.aiagentbackend.api.ApiException;
import com.xin.aiagentbackend.conversation.dto.MessageDto;
import com.xin.aiagentbackend.conversation.dto.SessionChatRequest;
import com.xin.aiagentbackend.conversation.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 对话控制器（Controller）。
 * <p>对外暴露旅行智能体（Manus）的对话 REST 接口（路径前缀 /ai），
 * 支持流式（SSE）与非流式两种对话方式；携带会话标识时自动恢复历史上下文并持久化对话消息。</p>
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AIController {
    private final ObjectProvider<Manus> manusProvider;
    private final ConversationService conversations;
    private final Set<String> activeConversations = ConcurrentHashMap.newKeySet();

    public AIController(ObjectProvider<Manus> manusProvider, ConversationService conversations) {
        this.manusProvider = manusProvider;
        this.conversations = conversations;
    }

    /** 会话式流式对话：请求体携带消息与会话标识 */
    @PostMapping(value = "/manus/chat", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doSessionChat(@Valid @RequestBody SessionChatRequest request) {
        return startManusChat(request.message(), request.conversationId());
    }

    /** 兼容旧客户端的流式接口：conversationId 可省略 */
    @GetMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithManus(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {
        validateMessage(message);
        return startManusChat(message, conversationId);
    }

    /** 兼容旧客户端的非流式接口 */
    @GetMapping("/manus/chatByRun")
    public String doChatWithManusByRun(@RequestParam String message) {
        validateMessage(message);
        return manusProvider.getObject().run(message);
    }

    /**
     * 发起一次 Manus 流式对话。
     * <p>携带会话标识时：先恢复最近 20 条历史消息作为上下文，追加用户消息，
     * 流结束后若代理状态为 FINISHED 则将助手回复持久化到会话；同一会话并发生成时返回冲突异常。</p>
     */
    private SseEmitter startManusChat(String message, String conversationId) {
        validateMessage(message);
        Manus manus = manusProvider.getObject();
        if (conversationId == null || conversationId.isBlank()) {
            return manus.runStream(message);
        }

        String id = conversationId.strip();
        List<MessageDto> previous = conversations.recentMessages(id, 20);
        if (!activeConversations.add(id)) throw ApiException.conflict("该会话正在生成回复，请稍后再发送");
        try {
            conversations.appendUserMessage(id, message);
        } catch (RuntimeException error) {
            activeConversations.remove(id);
            throw error;
        }

        List<Message> history = previous.stream().map(this::toSpringMessage).toList();
        SseEmitter emitter = manus.runStream(message, history);
        AtomicBoolean completed = new AtomicBoolean();
        Runnable finish = () -> {
            if (!completed.compareAndSet(false, true)) return;
            try {
                if (manus.getState().name().equals("FINISHED")) {
                    conversations.appendAssistantMessage(id, manus.getLastAssistantText());
                }
            } catch (Exception error) {
                log.warn("Could not persist assistant response for conversation {}", id, error);
            } finally {
                activeConversations.remove(id);
            }
        };
        emitter.onCompletion(finish);
        emitter.onTimeout(() -> activeConversations.remove(id));
        emitter.onError(error -> activeConversations.remove(id));
        return emitter;
    }

    /** 将消息 DTO 转换为 Spring AI 的消息对象（按角色区分助手/用户消息） */
    private Message toSpringMessage(MessageDto message) {
        return "assistant".equals(message.role())
                ? new AssistantMessage(message.content())
                : new UserMessage(message.content());
    }

    /** 校验用户消息：非空且长度不超过 8000 字符 */
    private void validateMessage(String message) {
        if (message == null || message.isBlank()) throw ApiException.badRequest("message不能为空");
        if (message.length() > 8000) throw ApiException.badRequest("message不能超过8000个字符");
    }
}
