package com.xin.aiagentbackend.plan.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建旅行计划请求对象（Request）。
 * <p>作为创建旅行计划接口的请求体，标题不能为空且最长 200 个字符，行程 JSON 必填。</p>
 *
 * @param conversationId 关联的会话标识，可为空
 * @param title          计划标题
 * @param itinerary      完整行程 JSON（含目的地、每日活动、预算等）
 * @param budgetSummary  预算结算摘要 JSON，可为空
 */
public record PlanRequest(
        String conversationId,
        @NotBlank @Size(max = 200) String title,
        @NotNull JsonNode itinerary,
        JsonNode budgetSummary) { }
