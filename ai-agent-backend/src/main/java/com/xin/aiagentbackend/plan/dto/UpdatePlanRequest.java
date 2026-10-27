package com.xin.aiagentbackend.plan.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新旅行计划请求对象（Request）。
 * <p>作为更新旅行计划接口的请求体，标题可选（为空时沿用原标题），
 * 行程 JSON 与乐观锁版本号必填。</p>
 *
 * @param title          新的计划标题，可为空
 * @param itinerary      新的完整行程 JSON
 * @param budgetSummary  新的预算结算摘要 JSON，可为空
 * @param version        期望的版本号（乐观锁），与数据库版本不一致时更新失败
 */
public record UpdatePlanRequest(
        @Size(max = 200) String title,
        @NotNull JsonNode itinerary,
        JsonNode budgetSummary,
        @NotNull Integer version) { }
