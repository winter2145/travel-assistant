package com.xin.aiagentbackend.agent.common;

/**
 * 代理执行状态的枚举类
 *
 * @author Administrator
 * @date 2026-10-19 20:17
 * @since 1.0.0
 */
public enum AgentState {

    // 空闲状态
    IDLE("idle"),

    // 运行中状态
    RUNNING("running"),

    // 已完成状态
    FINISHED("finished"),

    // 错误状态
    ERROR("error");

    private final String value;

    AgentState(String value) {
        this.value = value;
    }
    public String value() {
        return value;
    }

}