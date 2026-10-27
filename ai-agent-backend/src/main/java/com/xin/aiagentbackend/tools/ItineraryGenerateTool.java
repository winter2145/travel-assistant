package com.xin.aiagentbackend.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.xin.aiagentbackend.tools.common.ToolConstants;
import com.xin.aiagentbackend.tools.common.ToolResult;
import com.xin.aiagentbackend.tools.common.ToolSupport;
import com.xin.aiagentbackend.tools.common.TravelPace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 行程生成工具：把已搜索到的景点列表整理成按天结构化行程初稿。
 */
@Slf4j
@Component
public class ItineraryGenerateTool {

    /** 每日固定时段（4 个 slot，节奏决定每天用几个） */
    private static final String[][] TIME_SLOTS = {
            {"09:00", "11:00"},
            {"11:30", "13:00"},
            {"14:00", "16:00"},
            {"16:30", "18:00"}
    };

    private final ObjectMapper objectMapper;
    private final ToolCallback imageSearchTool;

    public ItineraryGenerateTool(ObjectMapper objectMapper,
                                 @Qualifier("pexelsMcpTools") ToolCallback[] pexelsMcpTools) {
        this.objectMapper = objectMapper;
        this.imageSearchTool = java.util.Arrays.stream(pexelsMcpTools)
                .filter(tool -> tool.getToolDefinition().name().endsWith("searchImages"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Pexels MCP 未提供 searchImages 工具"));
    }

    /**
     * 把已搜索到的景点列表整理成按天结构化行程初稿；不会凭空查询或伪造景点。
     *
     * @param city            旅游城市
     * @param startDate       出发日期 yyyy-MM-dd
     * @param days            旅游天数，1到14
     * @param attractionsJson 景点 JSON 数组，每项可含 name、address、cost；无景点传 []
     * @param pace            节奏 relaxed、normal 或 compact
     * @param budget          总预算，元
     */
    @Tool(description = "把已搜索到的景点列表整理成按天结构化行程初稿；不会凭空查询或伪造景点。")
    public ToolResult generateItinerary(
            @ToolParam(description = "旅游城市") String city,
            @ToolParam(description = "出发日期 yyyy-MM-dd") String startDate,
            @ToolParam(description = "旅游天数，1到14") int days,
            @ToolParam(description = "景点 JSON 数组，每项可含 name、address、cost；无景点传 []") String attractionsJson,
            @ToolParam(description = "节奏 relaxed、normal 或 compact") String pace,
            @ToolParam(description = "总预算，元") double budget) {
        return ToolSupport.execute("generateItinerary", () -> {
            if (city == null || city.isBlank()) {
                throw new IllegalArgumentException("city不能为空");
            }
            if (days < 1 || days > 14 || budget < 0) {
                throw new IllegalArgumentException("天数须为1到14，预算不能为负");
            }
            final LocalDate start;
            try {
                start = LocalDate.parse(startDate, ToolConstants.DATE_FORMATTER);
            } catch (Exception e) {
                throw new IllegalArgumentException("日期必须为 yyyy-MM-dd");
            }
            List<Map<String, Object>> places = parseAttractions(attractionsJson);
            TravelPace travelPace = TravelPace.from(pace);

            List<Map<String, Object>> dayList = new ArrayList<>();
            int index = 0;
            for (int d = 1; d <= days; d++) {
                List<Map<String, Object>> activities = new ArrayList<>();
                for (int slot = 0; slot < travelPace.activitiesPerDay() && index < places.size(); slot++, index++) {
                    Map<String, Object> place = places.get(index);
                    Map<String, Object> activity = new LinkedHashMap<>();
                    activity.put(ToolConstants.FIELD_START_TIME, TIME_SLOTS[slot][0]);
                    activity.put(ToolConstants.FIELD_END_TIME, TIME_SLOTS[slot][1]);
                    activity.put(ToolConstants.FIELD_NAME, place.getOrDefault("name", "待选择景点"));
                    activity.put("location", place.getOrDefault("address", place.getOrDefault("name", city)));
                    activity.put(ToolConstants.FIELD_ESTIMATED_COST, number(place.get("cost")));
                    activity.put(ToolConstants.FIELD_TRAVEL_MINUTES, 30);
                    activity.put("notes", "请结合营业时间、预约要求和路线查询结果复核");
                    addPexelsPhoto(activity, city);
                    activities.add(activity);
                }
                if (activities.isEmpty()) {
                    activities.add(Map.of(
                            ToolConstants.FIELD_START_TIME, "09:00",
                            ToolConstants.FIELD_END_TIME, "11:00",
                            ToolConstants.FIELD_NAME, "待搜索并填充景点",
                            "location", city,
                            ToolConstants.FIELD_ESTIMATED_COST, 0,
                            ToolConstants.FIELD_TRAVEL_MINUTES, 0,
                            "notes", "先调用 AttractionSearchTool 搜索真实景点"));
                }
                dayList.add(Map.of(
                        "day", d,
                        "date", start.plusDays(d - 1L).format(ToolConstants.DATE_FORMATTER),
                        ToolConstants.FIELD_ACTIVITIES, activities));
            }
            return Map.of(
                    "city", city,
                    "startDate", start.format(ToolConstants.DATE_FORMATTER),
                    "days", dayList,
                    "budget", budget,
                    "pace", travelPace.value(),
                    "estimatedActivityCost",
                            places.stream().mapToDouble(p -> number(p.get("cost"))).sum(),
                    "notes", List.of("仅汇总已提供的景点费用，不含住宿、餐饮和交通",
                            "建议继续进行预算与时间冲突校验"));
        });
    }

    private void addPexelsPhoto(Map<String, Object> activity, String city) {
        String keyword = activity.get("name") + " " + city;
        String response = toolText(imageSearchTool.call(objectMapper.createObjectNode().put("keyword", keyword).toString()))
                .replace("\\:", ":").replace("\\.", ".").replace("\\/", "/");
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("图片地址：([^\\s]+)\\s+Pexels 页面：([^\\s]+)\\s+摄影师：([^\\r\\n]+)")
                .matcher(response == null ? "" : response);
        if (!matcher.find()) {
            String detail = response == null || response.isBlank() ? "Pexels MCP 返回空响应" : response.strip();
            activity.put("photoSearchError", detail.substring(0, Math.min(240, detail.length())));
            log.warn("Pexels MCP 未返回可用图片，景点={}，响应={}", activity.get("name"), detail);
            return;
        }
        activity.put("photoUrl", matcher.group(1));
        activity.put("photoPageUrl", matcher.group(2));
        activity.put("photoPhotographer", matcher.group(3).trim());
        activity.put("photoSource", "Pexels");
    }

    private String toolText(String mcpResponse) {
        if (mcpResponse == null || mcpResponse.isBlank()) return "";
        String trimmed = mcpResponse.stripLeading();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            final JsonNode content;
            try {
                content = objectMapper.readTree(mcpResponse);
            } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
                throw new IllegalStateException("Pexels MCP 返回了无效 JSON 内容", error);
            }
            List<String> textParts = new ArrayList<>();
            collectText(content, textParts);
            if (!textParts.isEmpty()) return String.join("\n", textParts);
        }
        return mcpResponse;
    }

    private void collectText(JsonNode node, List<String> textParts) {
        if (node.isObject()) {
            JsonNode text = node.get("text");
            if (text != null && text.isTextual()) textParts.add(text.asText());
            else node.elements().forEachRemaining(child -> collectText(child, textParts));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> collectText(child, textParts));
        }
    }

    private List<Map<String, Object>> parseAttractions(String attractionsJson) {
        try {
            return objectMapper.readValue(
                    attractionsJson == null || attractionsJson.isBlank() ? "[]" : attractionsJson,
                    new TypeReference<>() {
                    });
        } catch (Exception e) {
            throw new IllegalArgumentException("attractionsJson必须是JSON数组");
        }
    }

    private double number(Object value) {
        try {
            return value == null ? 0 : Math.max(0, Double.parseDouble(value.toString()));
        } catch (Exception e) {
            return 0;
        }
    }
}
