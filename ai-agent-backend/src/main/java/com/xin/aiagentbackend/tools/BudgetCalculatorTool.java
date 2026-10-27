package com.xin.aiagentbackend.tools;

import com.xin.aiagentbackend.tools.common.ToolResult;
import com.xin.aiagentbackend.tools.common.ToolSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 预算计算工具：计算旅游预算分类明细、总额、人均费用和预算余额。
 */
@Slf4j
@Component
public class BudgetCalculatorTool {

    /** 金额统一保留 2 位小数 */
    private static final int SCALE = 2;

    /**
     * 计算旅游预算分类明细、总额、人均费用和预算余额，金额单位人民币元。
     *
     * @param accommodationPerNight 住宿每晚总价，元
     * @param nights                住宿晚数
     * @param foodPerPersonPerDay   每天每人餐饮预算，元
     * @param days                  旅游天数
     * @param transportTotal        交通总费用，元
     * @param ticketTotal           门票总费用，元
     * @param otherTotal            其他费用总额，元
     * @param travelers             出行人数
     * @param budget                总预算，元
     */
    @Tool(description = "计算旅游预算分类明细、总额、人均费用和预算余额，金额单位人民币元。")
    public ToolResult calculateBudget(
            @ToolParam(description = "住宿每晚总价，元") double accommodationPerNight,
            @ToolParam(description = "住宿晚数") int nights,
            @ToolParam(description = "每天每人餐饮预算，元") double foodPerPersonPerDay,
            @ToolParam(description = "旅游天数") int days,
            @ToolParam(description = "交通总费用，元") double transportTotal,
            @ToolParam(description = "门票总费用，元") double ticketTotal,
            @ToolParam(description = "其他费用总额，元") double otherTotal,
            @ToolParam(description = "出行人数") int travelers,
            @ToolParam(description = "总预算，元") double budget) {
        return ToolSupport.execute("calculateBudget", () -> {
            if (nights < 0 || days < 1 || travelers < 1) {
                throw new IllegalArgumentException("晚数不能为负，天数和人数必须大于0");
            }
            double[] amounts = {accommodationPerNight, foodPerPersonPerDay, transportTotal, ticketTotal, otherTotal, budget};
            for (double amount : amounts) {
                if (!Double.isFinite(amount) || amount < 0) {
                    throw new IllegalArgumentException("金额必须是非负有效数字");
                }
            }

            Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
            breakdown.put("accommodation", money(accommodationPerNight * nights));
            breakdown.put("food", money(foodPerPersonPerDay * days * travelers));
            breakdown.put("transport", money(transportTotal));
            breakdown.put("tickets", money(ticketTotal));
            breakdown.put("other", money(otherTotal));

            BigDecimal total = breakdown.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = money(budget).subtract(total);
            return Map.of(
                    "currency", "CNY",
                    "breakdown", breakdown,
                    "totalCost", total,
                    "budget", money(budget),
                    "remainingBudget", remaining,
                    "overBudget", remaining.signum() < 0,
                    "perPersonCost", total.divide(BigDecimal.valueOf(travelers), SCALE, RoundingMode.HALF_UP));
        });
    }

    private BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
