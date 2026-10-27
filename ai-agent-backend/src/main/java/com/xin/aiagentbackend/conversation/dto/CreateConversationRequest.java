package com.xin.aiagentbackend.conversation.dto;

import jakarta.validation.constraints.Size;

/**
 * 创建会话请求对象（Request）。
 * <p>作为创建会话接口的请求体，标题最长 200 个字符。</p>
 *
 * @param title 会话标题，可选，为空时由服务端生成默认标题
 */
public record CreateConversationRequest(@Size(max = 200) String title) { }
