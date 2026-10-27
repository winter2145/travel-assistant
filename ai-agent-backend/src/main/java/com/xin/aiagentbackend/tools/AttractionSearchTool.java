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
// * 景点搜索工具：通过高德地图 POI 关键字搜索指定城市的景点。
// */
//@Slf4j
//@Component
//public class AttractionSearchTool {
//
//    private final AmapWebClient amap;
//
//    public AttractionSearchTool(AmapWebClient amap) {
//        this.amap = amap;
//    }
//
//    /**
//     * 通过高德地图 POI 关键字搜索指定城市的景点，返回名称、地址、坐标、类型等数据。
//     *
//     * @param city     城市名称，例如杭州
//     * @param keyword  关键词，例如西湖、博物馆、自然风景
//     * @param pageSize 每页条数，1到5
//     */
//    @Tool(description = "通过高德地图 POI 关键字搜索指定城市的景点，返回名称、地址、坐标、类型等数据。")
//    public ToolResult searchAttractions(
//            @ToolParam(description = "城市名称，例如杭州") String city,
//            @ToolParam(description = "关键词，例如西湖、博物馆、自然风景") String keyword,
//            @ToolParam(description = "每页条数，1到5") int pageSize) {
//        return ToolSupport.execute("searchAttractions", () -> {
//            int size = Math.max(1, Math.min(5, pageSize));
//            return amap.get("/v3/place/text", Map.of(
//                    "keywords", keyword == null ? "景点" : keyword,
//                    "city", city == null ? "" : city,
//                    "citylimit", "true",
//                    "offset", size,
//                    "page", 1,
//                    "extensions", "all",
//                    "output", "JSON"));
//        });
//    }
//}
