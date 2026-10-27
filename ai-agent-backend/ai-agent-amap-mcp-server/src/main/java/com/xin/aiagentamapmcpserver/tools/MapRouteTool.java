package com.xin.aiagentamapmcpserver.tools;

import com.xin.aiagentamapmcpserver.tools.common.ToolResult;
import com.xin.aiagentamapmcpserver.tools.common.ToolSupport;
import com.xin.aiagentamapmcpserver.tools.common.TravelMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 路线规划工具：通过高德地图查询两地路线，支持驾车、步行、公交。
 */
@Slf4j
@Component
public class MapRouteTool {

    /** 高德坐标格式：经度,纬度，例如 120.1551,30.2741 */
    private static final Pattern COORD_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?");

    private final AmapWebClient amap;

    public MapRouteTool(AmapWebClient amap) {
        this.amap = amap;
    }

    /**
     * 通过高德地图查询两地路线。起终点使用经度,纬度坐标；支持 driving、walking、transit。
     *
     * @param origin      起点坐标，经度,纬度，例如120.1551,30.2741
     * @param destination 终点坐标，经度,纬度
     * @param mode        出行方式 driving、walking 或 transit
     * @param city        公交路线所在城市，公交查询必填，例如杭州
     */
    @Tool(description = "通过高德地图查询两地路线。起终点使用经度,纬度坐标；支持 driving、walking、transit。")
    public ToolResult queryRoute(
            @ToolParam(description = "起点坐标，经度,纬度，例如120.1551,30.2741") String origin,
            @ToolParam(description = "终点坐标，经度,纬度") String destination,
            @ToolParam(description = "出行方式 driving、walking 或 transit") String mode,
            @ToolParam(description = "公交路线所在城市，公交查询必填，例如杭州") String city) {
        return ToolSupport.execute("queryRoute", () -> {
            if (!validCoord(origin) || !validCoord(destination)) {
                throw new IllegalArgumentException("起终点必须为经度,纬度坐标，例如120.1551,30.2741");
            }
            TravelMode travelMode = TravelMode.from(mode);
            return switch (travelMode) {
                case DRIVING -> amap.get("/v3/direction/driving", Map.of(
                        "origin", origin, "destination", destination,
                        "extensions", "all", "output", "JSON"));
                case WALKING -> amap.get("/v3/direction/walking", Map.of(
                        "origin", origin, "destination", destination, "output", "JSON"));
                case TRANSIT -> amap.get("/v3/direction/transit/integrated", Map.of(
                        "origin", origin, "destination", destination,
                        "city", city == null ? "杭州" : city, "output", "JSON"));
            };
        });
    }

    private boolean validCoord(String s) {
        return s != null && COORD_PATTERN.matcher(s).matches();
    }
}
