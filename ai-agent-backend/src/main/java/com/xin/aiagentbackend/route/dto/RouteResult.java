package com.xin.aiagentbackend.route.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 路线查询结果对象（DTO）。
 * <p>作为路线查询接口的响应返回，包含解析后的起终点坐标与完整路线数据。</p>
 *
 * @param origin               原始起点输入
 * @param destination          原始终点输入
 * @param originCoordinate     解析后的起点坐标（经度,纬度）
 * @param destinationCoordinate 解析后的终点坐标（经度,纬度）
 * @param mode                 交通方式
 * @param route                高德返回的完整路线 JSON
 */
public record RouteResult(String origin, String destination, String originCoordinate,
                          String destinationCoordinate, String mode, JsonNode route) { }
