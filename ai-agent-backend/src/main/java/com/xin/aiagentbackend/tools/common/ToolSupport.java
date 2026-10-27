package com.xin.aiagentbackend.tools.common;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 工具执行模板：统一异常处理与日志。
 *
 * <p>企业级 Agent 中工具方法必须容错——任何异常都不能向上抛出打断 LLM 工具循环，
 * 统一转换为 {@link ToolResult} 回传给模型。所有 {@code @Tool} 方法应通过本类执行业务逻辑。
 */
@Slf4j
public final class ToolSupport {

    private ToolSupport() {
    }

    /**
     * 执行工具业务逻辑并统一封装结果。
     *
     * @param toolName 工具名（用于日志与错误码）
     * @param action   业务逻辑：抛 {@link IllegalArgumentException} 视为参数非法，其余异常视为内部错误
     * @return 统一 {@link ToolResult}
     */
    public static ToolResult execute(String toolName, Supplier<Object> action) {
        long start = System.currentTimeMillis();
        try {
            Object data = action.get();
            log.info("tool[{}] success, cost {}ms", toolName, System.currentTimeMillis() - start);
            return ToolResult.ok(data);
        } catch (IllegalArgumentException e) {
            log.warn("tool[{}] invalid argument: {}", toolName, e.getMessage());
            return ToolResult.invalid(e.getMessage());
        } catch (Exception e) {
            log.error("tool[{}] failed, cost {}ms", toolName, System.currentTimeMillis() - start, e);
            return ToolResult.fail(toolName + " 执行失败：" + e.getMessage());
        }
    }
}
