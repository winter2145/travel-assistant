package com.xin.aiagentbackend.tools.common;

import java.util.Locale;

/**
 * 行程调整操作枚举，对应 ItineraryAdjustTool 的 operation 参数。
 */
public enum ItineraryOperation {

    /** 删除活动 */
    REMOVE,
    /** 重命名活动 */
    RENAME,
    /** 修改活动时间 */
    SHIFT;

    /**
     * 解析操作类型。
     *
     * @param raw 原始入参（可空）
     * @return 对应枚举
     * @throws IllegalArgumentException 为空或非支持值
     */
    public static ItineraryOperation from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("operation 不能为空");
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("operation 仅支持 REMOVE、RENAME、SHIFT");
        }
    }
}
