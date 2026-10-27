package com.xin.aiagentbackend.tools.common;

/**
 * 出行方式枚举，对应高德路线规划接口的 mode 参数。
 */
public enum TravelMode {

    DRIVING("driving"),
    WALKING("walking"),
    TRANSIT("transit");

    private final String value;

    TravelMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /**
     * 解析出行方式；不传或非法值给出明确错误。
     *
     * @param raw 原始入参（可空）
     * @return 非空时解析成功返回对应枚举，为空返回默认 {@link #DRIVING}
     * @throws IllegalArgumentException 传入非支持值
     */
    public static TravelMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return DRIVING;
        }
        for (TravelMode mode : values()) {
            if (mode.value.equalsIgnoreCase(raw.trim())) {
                return mode;
            }
        }
        throw new IllegalArgumentException("mode 仅支持 driving、walking、transit");
    }
}
