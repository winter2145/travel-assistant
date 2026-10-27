package com.xin.aiagentbackend.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.junit.jupiter.api.Assertions.*;

class ItineraryGenerateToolTest {
    private static final String ATTRACTIONS = "[{\"name\":\"西湖\",\"address\":\"杭州西湖\"}]";

    @Test
    void storesPhotoReturnedByPexelsMcpOnGeneratedActivity() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ToolCallback pexels = callback(input -> {
            assertTrue(input.contains("西湖 杭州"));
            return "[{\"type\":\"text\",\"text\":\"图片 ID：1\\n图片地址：https://images.pexels.com/photos/123/photo.jpeg\\n"
                    + "Pexels 页面：https://www.pexels.com/photo/123/\\n摄影师：旅行摄影师\\n图片来源：Pexels\"}]";
        });

        var result = new ItineraryGenerateTool(mapper, new ToolCallback[]{pexels})
                .generateItinerary("杭州", "2026-11-01", 1, ATTRACTIONS, "normal", 2000);
        var activity = mapper.valueToTree(result.data()).path("days").get(0).path("activities").get(0);

        assertEquals("https://images.pexels.com/photos/123/photo.jpeg", activity.path("photoUrl").asText());
        assertEquals("https://www.pexels.com/photo/123/", activity.path("photoPageUrl").asText());
        assertEquals("旅行摄影师", activity.path("photoPhotographer").asText());
        assertFalse(activity.has("photoSearchError"));
    }

    @Test
    void reportsMissingPexelsImageWithoutInventingAUrl() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ToolCallback pexels = callback(input -> "[{\"type\":\"text\",\"text\":\"Pexels API Key 未配置\"}]");

        var result = new ItineraryGenerateTool(mapper, new ToolCallback[]{pexels})
                .generateItinerary("杭州", "2026-11-01", 1, ATTRACTIONS, "normal", 2000);
        var activity = mapper.valueToTree(result.data()).path("days").get(0).path("activities").get(0);

        assertFalse(activity.has("photoUrl"));
        assertEquals("Pexels API Key 未配置", activity.path("photoSearchError").asText());
    }

    private ToolCallback callback(java.util.function.Function<String, String> action) {
        return new ToolCallback() {
            @Override public String call(String input) { return action.apply(input); }
            @Override public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("searchImages").description("Pexels image search")
                        .inputSchema("{\"type\":\"object\",\"properties\":{\"keyword\":{\"type\":\"string\"}}}").build();
            }
        };
    }
}
