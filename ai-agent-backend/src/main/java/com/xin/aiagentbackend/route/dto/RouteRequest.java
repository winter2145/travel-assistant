package com.xin.aiagentbackend.route.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 路线查询请求对象（Request）。
 * <p>作为路线查询接口的请求体，起点与终点可为地点名称或经纬度坐标，交通方式默认驾车。</p>
 *
 * @param origin      起点（地点名称或“经度,纬度”坐标）
 * @param destination 终点（地点名称或“经度,纬度”坐标）
 * @param city        所在城市，用于地点解析
 * @param mode        交通方式（driving / walking / transit 等），为空时默认 driving
 */
public record RouteRequest(@NotBlank String origin, @NotBlank String destination,
                           @NotBlank String city, String mode) {
    /** 紧凑构造器：交通方式为空时默认使用驾车（driving） */
    public RouteRequest { if (mode == null || mode.isBlank()) mode = "driving"; }
}
