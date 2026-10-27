package com.xin.aiagentbackend.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xin.aiagentbackend.tools.common.ItineraryOperation;
import com.xin.aiagentbackend.tools.common.ToolConstants;
import com.xin.aiagentbackend.tools.common.ToolResult;
import com.xin.aiagentbackend.tools.common.ToolSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Map;

/**
 * 行程调整工具：确定性修改已有行程（REMOVE 删除、RENAME 改名、SHIFT 改时间）。
 */
@Slf4j
@Component
public class ItineraryAdjustTool {

    private final ObjectMapper objectMapper;

    public ItineraryAdjustTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 确定性修改已有行程：REMOVE删除活动、RENAME改名、SHIFT改时间。修改后应再次检查冲突和预算。
     *
     * @param itineraryJson 已有行程JSON字符串
     * @param operation     操作：REMOVE、RENAME、SHIFT
     * @param targetName    目标活动名称，支持包含匹配
     * @param newName       RENAME的新名称；否则可为空
     * @param newStartTime  SHIFT的新开始时间 HH:mm；否则可为空
     * @param newEndTime    SHIFT的新结束时间 HH:mm；否则可为空
     * @param targetDay     指定第几天，0表示不限制
     */
    @Tool(description = "确定性修改已有行程：REMOVE删除活动、RENAME改名、SHIFT改时间。修改后应再次检查冲突和预算。")
    public ToolResult adjustItinerary(
            @ToolParam(description = "已有行程JSON字符串") String itineraryJson,
            @ToolParam(description = "操作：REMOVE、RENAME、SHIFT") String operation,
            @ToolParam(description = "目标活动名称，支持包含匹配") String targetName,
            @ToolParam(description = "RENAME的新名称；否则可为空") String newName,
            @ToolParam(description = "SHIFT的新开始时间 HH:mm；否则可为空") String newStartTime,
            @ToolParam(description = "SHIFT的新结束时间 HH:mm；否则可为空") String newEndTime,
            @ToolParam(description = "指定第几天，0表示不限制") int targetDay) {
        return ToolSupport.execute("adjustItinerary", () -> {
            JsonNode root = parseItinerary(itineraryJson);
            if (!(root.path(ToolConstants.FIELD_DAYS) instanceof ArrayNode days)) {
                throw new IllegalArgumentException("行程必须包含days数组");
            }
            ItineraryOperation op = ItineraryOperation.from(operation);

            boolean found = false;
            for (JsonNode day : days) {
                if (targetDay > 0 && day.path("day").asInt() != targetDay) {
                    continue;
                }
                if (!(day.path(ToolConstants.FIELD_ACTIVITIES) instanceof ArrayNode activities)) {
                    continue;
                }
                for (int i = 0; i < activities.size(); i++) {
                    JsonNode activity = activities.get(i);
                    if (!activity.path(ToolConstants.FIELD_NAME).asText("")
                            .contains(targetName == null ? "" : targetName)) {
                        continue;
                    }
                    switch (op) {
                        case REMOVE -> activities.remove(i);
                        case RENAME -> {
                            if (newName == null || newName.isBlank()) {
                                throw new IllegalArgumentException("新名称不能为空");
                            }
                            ((ObjectNode) activity).put(ToolConstants.FIELD_NAME, newName);
                        }
                        case SHIFT -> {
                            LocalTime start;
                            LocalTime end;
                            try {
                                start = LocalTime.parse(newStartTime, ToolConstants.TIME_FORMATTER);
                                end = LocalTime.parse(newEndTime, ToolConstants.TIME_FORMATTER);
                            } catch (Exception ex) {
                                throw new IllegalArgumentException("时间必须为 HH:mm");
                            }
                            if (!end.isAfter(start)) {
                                throw new IllegalArgumentException("结束时间必须晚于开始时间");
                            }
                            ((ObjectNode) activity).put(ToolConstants.FIELD_START_TIME, newStartTime);
                            ((ObjectNode) activity).put(ToolConstants.FIELD_END_TIME, newEndTime);
                        }
                        default -> throw new IllegalArgumentException("operation 仅支持 REMOVE、RENAME、SHIFT");
                    }
                    found = true;
                    break;
                }
                if (found) {
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("没有找到目标活动");
            }
            return Map.of(
                    "success", true,
                    "operation", op.name(),
                    "itinerary", root,
                    "message", "调整完成；请继续调用 ConflictCheckTool 和 BudgetCheckTool 复核");
        });
    }

    private JsonNode parseItinerary(String itineraryJson) {
        try {
            return objectMapper.readTree(itineraryJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("行程JSON解析失败：" + e.getMessage());
        }
    }
}
