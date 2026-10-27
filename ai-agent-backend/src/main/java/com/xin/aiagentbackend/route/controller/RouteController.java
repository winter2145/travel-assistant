package com.xin.aiagentbackend.route.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.aiagentbackend.api.ApiException;
import com.xin.aiagentbackend.api.ApiResponse;
import com.xin.aiagentbackend.route.dto.RouteRequest;
import com.xin.aiagentbackend.route.dto.RouteResult;
import com.xin.aiagentbackend.tools.AmapWebClient;
import com.xin.aiagentbackend.tools.MapRouteTool;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 路线查询控制器（Controller）。
 * <p>对外暴露路线查询 REST 接口（路径前缀 /routes），
 * 支持将地点名称解析为经纬度坐标后，调用高德地图查询路线。</p>
 */
@RestController
@RequestMapping("/routes")
public class RouteController {
    private final AmapWebClient amap;
    private final MapRouteTool routeTool;
    private final ObjectMapper mapper;

    public RouteController(AmapWebClient amap, MapRouteTool routeTool, ObjectMapper mapper) {
        this.amap = amap;
        this.routeTool = routeTool;
        this.mapper = mapper;
    }

    /** 查询路线：起终点支持地点名称或坐标，地点名称先经高德地理编码解析为坐标 */
    @PostMapping("/query")
    public ApiResponse<RouteResult> query(@Valid @RequestBody RouteRequest request) {
        String origin = resolve(request.origin(), request.city());
        String destination = resolve(request.destination(), request.city());
        JsonNode route = routeTool.query(origin, destination, request.mode(), request.city());
        return ApiResponse.ok(new RouteResult(request.origin(), request.destination(), origin, destination, request.mode(), route));
    }

    /** 将地点解析为经纬度坐标：已是坐标格式则原样返回，否则调用高德地理编码接口 */
    private String resolve(String place, String city) {
        if (place.matches("-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?")) return place;
        try {
            JsonNode root = mapper.readTree(amap.get("/v3/geocode/geo", Map.of("address", place, "city", city, "output", "JSON")));
            JsonNode geocodes = root.path("geocodes");
            if (!geocodes.isArray() || geocodes.isEmpty() || geocodes.get(0).path("location").asText().isBlank()) {
                throw ApiException.badRequest("高德地图未找到地点：" + place);
            }
            return geocodes.get(0).path("location").asText();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("地点解析失败：" + place, e);
        }
    }
}
