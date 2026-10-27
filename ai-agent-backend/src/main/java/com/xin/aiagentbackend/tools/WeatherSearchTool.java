//package com.xin.aiagentbackend.tools;
//
//import com.xin.aiagentbackend.tools.common.ToolResult;
//import com.xin.aiagentbackend.tools.common.ToolSupport;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.tool.annotation.Tool;
//import org.springframework.ai.tool.annotation.ToolParam;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//
///**
// * 天气查询工具：通过高德天气接口查询中国城市天气。
// */
//@Slf4j
//@Component
//public class WeatherSearchTool {
//
//    /** 实况天气 */
//    private static final String EXTENSIONS_BASE = "base";
//    /** 天气预报（含未来 3 天） */
//    private static final String EXTENSIONS_ALL = "all";
//
//    private final AmapWebClient amap;
//
//    public WeatherSearchTool(AmapWebClient amap) {
//        this.amap = amap;
//    }
//
//    /**
//     * 通过高德天气接口查询中国城市天气。
//     *
//     * @param cityOrAdcode 城市名或行政区划编码，例如杭州或330100
//     * @param extensions   base查询实况，all查询天气预报
//     */
//    @Tool(description = "通过高德天气接口查询中国城市天气。建议传行政区划adcode；传城市名时由高德接口解析。")
//    public ToolResult searchWeather(
//            @ToolParam(description = "城市名或行政区划编码，例如杭州或330100") String cityOrAdcode,
//            @ToolParam(description = "base查询实况，all查询天气预报") String extensions) {
//        return ToolSupport.execute("searchWeather", () -> {
//            String ext = EXTENSIONS_ALL.equalsIgnoreCase(extensions) ? EXTENSIONS_ALL : EXTENSIONS_BASE;
//            return amap.get("/v3/weather/weatherInfo", Map.of(
//                    "city", cityOrAdcode == null ? "" : cityOrAdcode,
//                    "extensions", ext,
//                    "output", "JSON"));
//        });
//    }
//}
