package com.xin.aiagentamapmcpserver.config;

import com.xin.aiagentamapmcpserver.tools.AttractionSearchTool;
import com.xin.aiagentamapmcpserver.tools.FoodCuisineSearchTool;
import com.xin.aiagentamapmcpserver.tools.MapRouteTool;
import com.xin.aiagentamapmcpserver.tools.WeatherSearchTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具装配配置：
 * 把 tools 包下 9 个 @Tool 工具收集为 ToolCallbackProvider，
 * 并导出 ToolCallback[] allTools Bean，供 LoveApp.doChatWithTools 注册给 LLM。
 */
@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider amapToolCallbackProvider(
            AttractionSearchTool attractionSearchTool,
            FoodCuisineSearchTool foodCuisineSearchTool,
            WeatherSearchTool weatherSearchTool,
            MapRouteTool mapRouteTool) {

        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        attractionSearchTool,
                        foodCuisineSearchTool,
                        weatherSearchTool,
                        mapRouteTool

                )
                .build();
    }

    @Bean
    public ToolCallback[] allTools(ToolCallbackProvider amapToolCallbackProvider) {
        return amapToolCallbackProvider.getToolCallbacks();
    }
}
