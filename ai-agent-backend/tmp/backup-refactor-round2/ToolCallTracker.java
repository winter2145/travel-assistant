package com.xin.aiagentbackend.app;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录每个会话（chatId）实际被 LLM 调用的工具名称，
 * 用于验证 doChatWithTools 中 10 个工具类是否都被正确调用。
 */
@Component
public class ToolCallTracker {

    private final ConcurrentHashMap<String, Set<String>> invokedByChatId = new ConcurrentHashMap<>();

    /**
     * 记录某会话调用过某工具
     */
    public void record(String chatId, String toolName) {
        invokedByChatId.computeIfAbsent(chatId, k -> ConcurrentHashMap.newKeySet()).add(toolName);
    }

    /**
     * 查询某会话实际调用过的工具名称集合
     */
    public Set<String> getInvokedTools(String chatId) {
        return invokedByChatId.getOrDefault(chatId, Collections.emptySet());
    }

    /**
     * 查询某会话是否调用过指定工具
     */
    public boolean hasInvoked(String chatId, String toolName) {
        return getInvokedTools(chatId).contains(toolName);
    }
}
