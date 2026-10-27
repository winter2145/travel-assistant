package com.xin.aiagentbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.aiagentbackend.api.ApiException;
import com.xin.aiagentbackend.api.ApiResponse;
import com.xin.aiagentbackend.tools.AmapWebClient;
import com.xin.aiagentbackend.tools.MapRouteTool;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    @PostMapping("/query")
    public ApiResponse<RouteResult> query(@Valid @RequestBody RouteRequest request) {
        String origin = resolve(request.origin(), request.city());
        String destination = resolve(request.destination(), request.city());
        JsonNode route = routeTool.query(origin, destination, request.mode(), request.city());
        return ApiResponse.ok(new RouteResult(request.origin(), request.destination(), origin, destination, request.mode(), route));
    }

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

    public record RouteRequest(@NotBlank String origin, @NotBlank String destination,
                               @NotBlank String city, String mode) {
        public RouteRequest { if (mode == null || mode.isBlank()) mode = "driving"; }
    }

    public record RouteResult(String origin, String destination, String originCoordinate,
                              String destinationCoordinate, String mode, JsonNode route) { }
}
