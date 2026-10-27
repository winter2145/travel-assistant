package com.xin.aiagentbackend.agent.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class AgentExecutionContext {

    private static final int MAX_SAME_CALL = 2;
    private static final int MAX_NO_PROGRESS = 3;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 记录每种工具调用指纹出现的次数
    private final Map<String, Integer> callCounts = new HashMap<>();

    // 最近一次执行结果及其连续重复次数
    private String lastResultFingerprint;
    private int noProgressCount;

    // 记录任务进度，供提示词使用
    private final StringBuilder executionSummary = new StringBuilder();

    /**
     * 检查工具调用是否重复。
     *
     * 同一个工具使用相同参数，最多允许执行两次。
     * 第三次相同调用会被拒绝。
     */
    public boolean allowToolCalls(
            java.util.List<AssistantMessage.ToolCall> toolCalls) {

        if (toolCalls == null || toolCalls.isEmpty()) {
            return true;
        }

        Map<String, Integer> batchCounts = new HashMap<>();

        // 先检查整批调用，再统一登记，避免检查过程中部分计数被修改
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            String fingerprint = fingerprint(
                    toolCall.name(), toolCall.arguments());

            int previousCount = callCounts.getOrDefault(fingerprint, 0);
            int currentBatchCount = batchCounts.merge(
                    fingerprint, 1, Integer::sum);

            if (previousCount + currentBatchCount > MAX_SAME_CALL) {
                return false;
            }
        }

        for (Map.Entry<String, Integer> entry : batchCounts.entrySet()) {
            callCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        return true;
    }

    /**
     * 根据工具名称和参数生成稳定的调用指纹。
     */
    private String fingerprint(String name, String arguments) {
        try {
            JsonNode node = objectMapper.readTree(arguments);
            return name + ":" + canonicalize(node);
        } catch (Exception e) {
            // 参数不是合法 JSON 时使用原始字符串兜底
            return name + ":" + arguments;
        }
    }

    private String canonicalize(JsonNode node) {
        if (node == null || !node.isObject()) {
            return node == null ? "null" : node.toString();
        }

        ObjectNode sorted = objectMapper.createObjectNode();

        Map<String, JsonNode> fields = new TreeMap<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            fields.put(entry.getKey(), entry.getValue());
        }

        fields.forEach((key, value) ->
                sorted.set(key, canonicalNode(value)));

        return sorted.toString();
    }

    private JsonNode canonicalNode(JsonNode node) {
        if (node != null && node.isObject()) {
            try {
                return objectMapper.readTree(canonicalize(node));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        if (node != null && node.isArray()) {
            var array = objectMapper.createArrayNode();
            node.forEach(item -> array.add(canonicalNode(item)));
            return array;
        }

        return node;
    }

    /**
     * 记录工具执行结果。
     */
    public boolean recordResult(String result) {
        String fingerprint = result == null ? "" : result.trim();

        if (fingerprint.equals(lastResultFingerprint)) {
            noProgressCount++;
        } else {
            noProgressCount = 0;
            lastResultFingerprint = fingerprint;
        }

        return noProgressCount < MAX_NO_PROGRESS;
    }

    public void appendSummary(String summary) {
        if (summary != null && !summary.isBlank()) {
            executionSummary.append("- ")
                    .append(summary)
                    .append('\n');
        }
    }

    public String buildContextPrompt() {
        return """
                ## 当前任务执行状态
                已完成的工作：
                %s

                ## 执行约束
                1. 先检查已有结果，不要重复执行已完成的操作。
                2. 相同工具和相同参数不要无限重试。
                3. 如果执行结果没有变化，必须调整策略。
                4. 如果无法继续，说明原因并给出已有结果。
                5. 已获得足够信息时，直接给出最终答案。
                """.formatted(
                executionSummary.length() == 0
                        ? "暂无已完成的工作"
                        : executionSummary);
    }

    public void reset() {
        callCounts.clear();
        lastResultFingerprint = null;
        noProgressCount = 0;
        executionSummary.setLength(0);
    }
}