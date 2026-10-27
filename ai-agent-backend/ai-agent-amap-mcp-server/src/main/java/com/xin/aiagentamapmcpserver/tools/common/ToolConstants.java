package com.xin.aiagentamapmcpserver.tools.common;

import java.time.format.DateTimeFormatter;

/**
 * 工具公共常量：时间格式与行程 JSON 结构字段名。
 */
public final class ToolConstants {

    private ToolConstants() {
    }

    /** 活动时间格式 */
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /** 出发日期格式 */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 行程 JSON：天数组字段 */
    public static final String FIELD_DAYS = "days";

    /** 行程 JSON：单日活动数组字段 */
    public static final String FIELD_ACTIVITIES = "activities";

    /** 行程 JSON：活动名 */
    public static final String FIELD_NAME = "name";

    /** 行程 JSON：活动开始时间 */
    public static final String FIELD_START_TIME = "startTime";

    /** 行程 JSON：活动结束时间 */
    public static final String FIELD_END_TIME = "endTime";

    /** 行程 JSON：活动预估费用 */
    public static final String FIELD_ESTIMATED_COST = "estimatedCost";

    /** 行程 JSON：与前一个活动的通勤分钟数 */
    public static final String FIELD_TRAVEL_MINUTES = "travelMinutesFromPrevious";
}
