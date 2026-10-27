package com.xin.aiagentbackend.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.aiagentbackend.tools.common.ToolResult;
import com.xin.aiagentbackend.tools.common.ToolSupport;
import com.xin.aiagentbackend.tools.common.TravelMode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public class MapRouteTool {
    private static final Pattern COORDINATES = Pattern.compile("-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?");
    private final AmapWebClient amap;
    private final ObjectMapper mapper;

    public MapRouteTool(AmapWebClient amap, ObjectMapper mapper) { this.amap = amap; this.mapper = mapper; }

    @Tool(description = "通过高德地图查询路线。起终点使用高德经纬度坐标，经度,纬度；支持 driving、walking、transit。")
    public ToolResult queryRoute(
            @ToolParam(description = "起点经纬度，经度,纬度") String origin,
            @ToolParam(description = "终点经纬度，经度,纬度") String destination,
            @ToolParam(description = "出行方式 driving、walking 或 transit") String mode,
            @ToolParam(description = "公交路线所在城市，公交查询必填") String city) {
        return ToolSupport.execute("queryRoute", () -> query(origin, destination, mode, city));
    }

    public JsonNode query(String origin, String destination, String mode, String city) {
        if (origin == null || !COORDINATES.matcher(origin).matches()
                || destination == null || !COORDINATES.matcher(destination).matches()) {
            throw new IllegalArgumentException("起终点必须是经度,纬度坐标，例如120.1551,30.2741");
        }
        TravelMode travelMode = TravelMode.from(mode);
        String path = switch (travelMode) {
            case DRIVING -> "/v3/direction/driving";
            case WALKING -> "/v3/direction/walking";
            case TRANSIT -> "/v3/direction/transit/integrated";
        };
        Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("origin", origin);
        params.put("destination", destination);
        params.put("output", "JSON");
        if (travelMode == TravelMode.DRIVING) params.put("extensions", "all");
        if (travelMode == TravelMode.TRANSIT) {
            if (city == null || city.isBlank()) throw new IllegalArgumentException("公交路线查询必须提供城市");
            params.put("city", city);
        }
        try { return mapper.readTree(amap.get(path, params)); }
        catch (java.io.IOException e) { throw new IllegalStateException("无法解析高德路线响应", e); }
    }
}
