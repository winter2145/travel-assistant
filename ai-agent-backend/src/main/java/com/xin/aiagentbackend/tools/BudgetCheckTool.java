package com.xin.aiagentbackend.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.aiagentbackend.tools.common.ToolConstants;
import com.xin.aiagentbackend.tools.common.ToolResult;
import com.xin.aiagentbackend.tools.common.ToolSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 预算检查工具：汇总行程中 activities[].estimatedCost 并检查是否超预算，给出优化建议。
 */
@Slf4j
@Component
public class BudgetCheckTool {

    private final ObjectMapper objectMapper;

    public BudgetCheckTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 汇总行程中 activities[].estimatedCost 并检查是否超预算，给出优化建议。
     *
     * @param itineraryJson   结构化行程JSON字符串
     * @param budget          用户总预算，元
     * @param additionalCosts 未写进行程的住宿餐饮交通等额外费用，元
     */
    @Tool(description = "汇总行程中 activities[].estimatedCost 并检查是否超预算，给出优化建议。")
    public ToolResult checkBudget(
            @ToolParam(description = "结构化行程JSON字符串") String itineraryJson,
            @ToolParam(description = "用户总预算，元") double budget,
            @ToolParam(description = "未写进行程的住宿餐饮交通等额外费用，元") double additionalCosts) {
        return ToolSupport.execute("checkBudget", () -> {
            if (budget < 0 || additionalCosts < 0) {
                throw new IllegalArgumentException("金额不能为负数");
            }
            JsonNode root = parseItinerary(itineraryJson);

            double activityCost = 0;
            int activityCount = 0;
            for (JsonNode day : root.path(ToolConstants.FIELD_DAYS)) {
                for (JsonNode activity : day.path(ToolConstants.FIELD_ACTIVITIES)) {
                    JsonNode cost = activity.get(ToolConstants.FIELD_ESTIMATED_COST);
                    if (cost != null && cost.isNumber()) {
                        activityCost += Math.max(0, cost.asDouble());
                        activityCount++;
                    }
                }
            }

            double total = activityCost + additionalCosts;
            double remaining = budget - total;
            List<String> tips = new ArrayList<>();
            if (remaining < 0) {
                tips.add("当前估算超预算 " + round(-remaining) + " 元");
                tips.add("优先替换免费或低门票景点，减少付费项目");
                tips.add("按地理区域安排景点，降低交通成本");
                tips.add("比较住宿与餐饮方案，并重新核算交通费用");
            } else {
                tips.add("当前已知费用剩余 " + round(remaining) + " 元");
                tips.add("预留应急金，并确认住宿、餐饮、交通是否全部计入");
            }
            return Map.of(
                    "budget", round(budget),
                    "knownActivityCosts", round(activityCost),
                    "additionalCosts", round(additionalCosts),
                    "estimatedTotal", round(total),
                    "remainingBudget", round(remaining),
                    "overBudget", remaining < 0,
                    "activitiesWithCost", activityCount,
                    "suggestions", tips,
                    "warning", "缺少 estimatedCost 的项目不会计入，结果不是最终报价");
        });
    }

    private JsonNode parseItinerary(String itineraryJson) {
        try {
            return objectMapper.readTree(itineraryJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("行程JSON解析失败：" + e.getMessage());
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
