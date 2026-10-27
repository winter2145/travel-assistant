package com.xin.aiagentamapmcpserver.tools;

import com.xin.aiagentamapmcpserver.tools.common.ToolResult;
import com.xin.aiagentamapmcpserver.tools.common.ToolSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 美食搜索工具：通过高德地图 POI 搜索指定城市或区域的餐馆、地方菜和美食。
 */
@Slf4j
@Component
public class FoodCuisineSearchTool {

    private final AmapWebClient amap;

    public FoodCuisineSearchTool(AmapWebClient amap) {
        this.amap = amap;
    }

    /**
     * 通过高德地图 POI 搜索指定城市或区域的餐馆、地方菜和美食。
     *
     * @param city     城市名称，例如杭州
     * @param keyword  美食关键词，例如杭帮菜、片儿川、素食、火锅
     * @param area     可选区域，例如西湖景区附近；没有时传空字符串
     * @param pageSize 每页条数，1到20
     */
    @Tool(description = "通过高德地图 POI 搜索指定城市或区域的餐馆、地方菜和美食。")
    public ToolResult searchFood(
            @ToolParam(description = "城市名称，例如杭州") String city,
            @ToolParam(description = "美食关键词，例如杭帮菜、片儿川、素食、火锅") String keyword,
            @ToolParam(description = "可选区域，例如西湖景区附近；没有时传空字符串") String area,
            @ToolParam(description = "每页条数，1到20") int pageSize) {
        return ToolSupport.execute("searchFood", () -> {
            int size = Math.max(1, Math.min(20, pageSize));
            String query = ((area == null || area.isBlank()) ? "" : area + " ")
                    + (keyword == null ? "餐馆" : keyword);
            return amap.get("/v3/place/text", Map.of(
                    "keywords", query,
                    "city", city == null ? "" : city,
                    "citylimit", "true",
                    "offset", size,
                    "page", 1,
                    "types", "餐饮服务",
                    "extensions", "all",
                    "output", "JSON"));
        });
    }
}
