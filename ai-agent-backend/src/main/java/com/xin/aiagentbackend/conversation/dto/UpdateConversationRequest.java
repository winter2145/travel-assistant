package com.xin.aiagentbackend.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新会话请求对象（Request）。
 * <p>作为更新会话标题接口的请求体，标题不能为空且最长 200 个字符。</p>
 *
 * @param title 新的会话标题
 */
public record UpdateConversationRequest(@NotBlank @Size(max = 200) String title) { }
