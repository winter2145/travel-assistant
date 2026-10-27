package com.xin.aiagentbackend.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 会话聊天请求对象（Request）。
 * <p>作为 AI 会话对话接口的请求体，携带用户输入消息与会话标识。</p>
 *
 * @param message        用户输入的消息内容，最长 8000 个字符
 * @param conversationId 会话唯一标识，不能为空
 */
public record SessionChatRequest(
        @NotBlank @Size(max = 8000) String message,
        @NotBlank String conversationId) { }
