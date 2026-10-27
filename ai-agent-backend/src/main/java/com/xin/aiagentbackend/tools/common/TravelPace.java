package com.xin.aiagentbackend.tools.common;

/**
 * 行程节奏枚举，对应 ItineraryGenerateTool 的 pace 参数。
 */
public enum TravelPace {

    /** 宽松：每天 2 个景点 */
    RELAXED("relaxed", 2),
    /** 常规：每天 3 个景点 */
    NORMAL("normal", 3),
    /** 紧凑：每天 4 个景点 */
    COMPACT("compact", 4);

    private final String value;
    private final int activitiesPerDay;

    TravelPace(String value, int activitiesPerDay) {
        this.value = value;
        this.activitiesPerDay = activitiesPerDay;
    }

    public String value() {
        return value;
    }

    public int activitiesPerDay() {
        return activitiesPerDay;
    }

    /**
     * 解析节奏；为空默认 {@link #NORMAL}。
     *
     * @param raw 原始入参（可空）
     * @return 对应枚举
     * @throws IllegalArgumentException 传入非支持值
     */
    public static TravelPace from(String raw) {
        if (raw == null || raw.isBlank()) {
            return NORMAL;
        }
        for (TravelPace pace : values()) {
            if (pace.value.equalsIgnoreCase(raw.trim())) {
                return pace;
            }
        }
        throw new IllegalArgumentException("pace 仅支持 relaxed、normal、compact");
    }
}
