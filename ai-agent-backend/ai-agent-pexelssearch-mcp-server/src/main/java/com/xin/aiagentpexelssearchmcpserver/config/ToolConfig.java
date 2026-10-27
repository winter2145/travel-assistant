package com.xin.aiagentpexelssearchmcpserver.config;

import com.xin.aiagentpexelssearchmcpserver.tools.PexelsSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具装配配置：
 * 把 @Tool 工具收集为 ToolCallbackProvider。
 * spring-ai-starter-mcp-server（1.x）会自动把容器中的 ToolCallbackProvider
 * 注册为 MCP Server 的工具，供父模块 MCP 客户端调用。
 */
@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider pelesToolCallbackProvider(PexelsSearchTool pexelsSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(pexelsSearchTool)
                .build();
    }
}
