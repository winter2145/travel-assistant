package com.xin.aiagentbackend.tools.common;

/**
 * 高德开放平台接口调用异常。
 *
 * <p>覆盖三类场景：Key 未配置、HTTP/网络异常、高德业务返回失败（status=0）。
 */
public class AmapApiException extends RuntimeException {

    public AmapApiException(String message) {
        super(message);
    }

    public AmapApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
