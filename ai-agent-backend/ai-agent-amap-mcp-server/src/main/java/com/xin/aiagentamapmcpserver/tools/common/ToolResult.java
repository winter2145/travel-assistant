package com.xin.aiagentamapmcpserver.tools.common;

/**
 * 工具统一返回契约。
 *
 * <p>所有 {@code @Tool} 方法统一返回该结构，保证 LLM 收到的工具输出格式一致：
 * <pre>
 * { "success": true, "code": "OK", "message": "ok", "data": {...} }
 * </pre>
 *
 * @param success 是否成功
 * @param code    业务码：OK / INVALID_ARGUMENT / INTERNAL_ERROR
 * @param message 面向 LLM 的可读说明（失败时为失败原因）
 * @param data    业务数据（成功时为实际结果）
 */
public record ToolResult(boolean success, String code, String message, Object data) {

    /** 成功业务码 */
    public static final String CODE_OK = "OK";
    /** 参数非法业务码 */
    public static final String CODE_INVALID_ARGUMENT = "INVALID_ARGUMENT";
    /** 内部错误业务码 */
    public static final String CODE_INTERNAL_ERROR = "INTERNAL_ERROR";

    /**
     * 成功结果。
     *
     * @param data 业务数据
     */
    public static ToolResult ok(Object data) {
        return new ToolResult(true, CODE_OK, "ok", data);
    }

    /**
     * 参数非法结果（如校验失败、JSON 解析失败）。
     *
     * @param message 失败原因
     */
    public static ToolResult invalid(String message) {
        return new ToolResult(false, CODE_INVALID_ARGUMENT, message, null);
    }

    /**
     * 内部错误结果（如高德接口异常、未预期异常）。
     *
     * @param message 失败原因
     */
    public static ToolResult fail(String message) {
        return new ToolResult(false, CODE_INTERNAL_ERROR, message, null);
    }
}
