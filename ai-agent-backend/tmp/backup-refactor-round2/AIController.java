package com.xin.aiagentbackend.controller;

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

/** HTTP entry points for the existing Manus travel agent. */
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

    /** Existing stream URL retained; conversationId is optional for old clients. */
    @GetMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithManus(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {
        validateMessage(message);
        return startManusChat(message, conversationId);
    }

    @PostMapping(value = "/manus/chat", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doSessionChat(@Valid @RequestBody SessionChatRequest request) {
        return startManusChat(request.message(), request.conversationId());
    }

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

    /** Existing non-streaming route remains available for compatibility. */
    @GetMapping("/manus/chatByRun")
    public String doChatWithManusByRun(@RequestParam String message) {
        validateMessage(message);
        return manusProvider.getObject().run(message);
    }

    private Message toSpringMessage(MessageDto message) {
        return "assistant".equals(message.role())
                ? new AssistantMessage(message.content())
                : new UserMessage(message.content());
    }

    private void validateMessage(String message) {
        if (message == null || message.isBlank()) throw ApiException.badRequest("message不能为空");
        if (message.length() > 8000) throw ApiException.badRequest("message不能超过8000个字符");
    }
}
