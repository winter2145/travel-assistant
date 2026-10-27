package com.xin.aiagentbackend.advisor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置属性类
 * 把 application.yml 里的配置绑定成 Java 对象。
 */
@ConfigurationProperties(prefix = "spring.ai.advisor.logger")
public class MyLoggerProperties {

    /**
     * 日志级别，默认INFO
     */
    private LogLevel level = LogLevel.INFO;

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }
}
