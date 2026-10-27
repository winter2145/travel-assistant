package com.xin.aiagentbackend.plan.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 旅行计划数据传输对象（DTO）。
 * <p>用于旅行计划创建、列表、详情、更新等接口的响应返回，
 * 承载行程标题、城市、日期、天数、预算等核心信息以及完整行程 JSON。</p>
 *
 * @param id            计划唯一标识
 * @param conversationId 关联的会话标识，可为空
 * @param title          计划标题
 * @param city           目的地城市
 * @param startDate      出发日期（yyyy-MM-dd）
 * @param days           行程天数
 * @param budget         计划预算
 * @param pace           旅行节奏（如 normal / relaxed / intense）
 * @param itinerary      完整行程 JSON（每日活动明细）
 * @param budgetSummary  预算结算摘要 JSON，可为空
 * @param createdAt      创建时间（毫秒时间戳）
 * @param updatedAt      最近更新时间（毫秒时间戳）
 * @param version        乐观锁版本号
 */
public record PlanDto(
        String id,
        String conversationId,
        String title,
        String city,
        String startDate,
        int days,
        double budget,
        String pace,
        JsonNode itinerary,
        JsonNode budgetSummary,
        long createdAt,
        long updatedAt,
        int version) { }
