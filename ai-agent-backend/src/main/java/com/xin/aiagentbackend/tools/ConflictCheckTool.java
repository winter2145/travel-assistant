package com.xin.aiagentbackend.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.aiagentbackend.tools.common.ToolConstants;
import com.xin.aiagentbackend.tools.common.ToolResult;
import com.xin.aiagentbackend.tools.common.ToolSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 时间冲突检查工具：检查行程时间重叠、结束时间错误和活动间通勤缓冲不足。
 */
@Slf4j
@Component
public class ConflictCheckTool {

    private final ObjectMapper objectMapper;

    public ConflictCheckTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 检查行程时间重叠、结束时间错误和活动间通勤缓冲不足。
     *
     * @param itineraryJson       行程JSON字符串，days[].activities[]需含startTime、endTime
     * @param defaultTravelMinutes 默认通勤缓冲分钟数，建议30
     */
    @Tool(description = "检查行程时间重叠、结束时间错误和活动间通勤缓冲不足。")
    public ToolResult checkConflicts(
            @ToolParam(description = "行程JSON字符串，days[].activities[]需含startTime、endTime") String itineraryJson,
            @ToolParam(description = "默认通勤缓冲分钟数，建议30") int defaultTravelMinutes) {
        return ToolSupport.execute("checkConflicts", () -> {
            if (defaultTravelMinutes < 0) {
                throw new IllegalArgumentException("默认通勤缓冲分钟数不能为负数");
            }
            JsonNode root = parseItinerary(itineraryJson);

            List<Map<String, Object>> conflicts = new ArrayList<>();
            for (JsonNode day : root.path(ToolConstants.FIELD_DAYS)) {
                int dayNo = day.path("day").asInt(0);
                LocalTime previousEnd = null;
                String previousName = "";
                for (JsonNode activity : day.path(ToolConstants.FIELD_ACTIVITIES)) {
                    String name = activity.path(ToolConstants.FIELD_NAME).asText("未命名活动");
                    try {
                        LocalTime start = LocalTime.parse(activity.path(ToolConstants.FIELD_START_TIME).asText());
                        LocalTime end = LocalTime.parse(activity.path(ToolConstants.FIELD_END_TIME).asText());
                        if (!end.isAfter(start)) {
                            conflicts.add(issue(dayNo, "INVALID_DURATION", name, "结束时间必须晚于开始时间"));
                        }
                        if (previousEnd != null) {
                            if (start.isBefore(previousEnd)) {
                                conflicts.add(issue(dayNo, "TIME_OVERLAP", name, "与「" + previousName + "」时间重叠"));
                            } else {
                                int commute = activity.has(ToolConstants.FIELD_TRAVEL_MINUTES)
                                        ? activity.path(ToolConstants.FIELD_TRAVEL_MINUTES).asInt(defaultTravelMinutes)
                                        : defaultTravelMinutes;
                                long gap = Duration.between(previousEnd, start).toMinutes();
                                if (gap < commute) {
                                    conflicts.add(issue(dayNo, "INSUFFICIENT_COMMUTE", name,
                                            "间隔" + gap + "分钟，低于通勤预留" + commute + "分钟"));
                                }
                            }
                        }
                        previousEnd = end;
                        previousName = name;
                    } catch (Exception ex) {
                        conflicts.add(issue(dayNo, "INVALID_TIME", name, "时间缺失或格式错误，必须使用HH:mm"));
                    }
                }
            }
            return Map.of(
                    "hasConflict", !conflicts.isEmpty(),
                    "conflictCount", conflicts.size(),
                    "conflicts", conflicts,
                    "suggestion", conflicts.isEmpty()
                            ? "未发现明显时间冲突；仍需用地图路线核实通勤时间"
                            : "先解决重叠，再根据真实路线调整时间或删减景点");
        });
    }

    private JsonNode parseItinerary(String itineraryJson) {
        try {
            return objectMapper.readTree(itineraryJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("行程JSON解析失败：" + e.getMessage());
        }
    }

    private Map<String, Object> issue(int day, String type, String name, String message) {
        return Map.of("day", day, "type", type, "activity", name, "message", message);
    }
}
