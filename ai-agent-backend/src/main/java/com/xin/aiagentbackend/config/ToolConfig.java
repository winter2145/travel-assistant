package com.xin.aiagentbackend.config;

import com.xin.aiagentbackend.tools.*;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * 工具装配配置：
 * 将本地旅行工具和不同 MCP 服务端的工具分别装配。
 *
 * allTools：本地旅行工具 + amap MCP 工具。
 * pexelsMcpTools：Pexels MCP 服务端工具。
 */
@Configuration
public class ToolConfig {

    private static final String AMAP_MCP_SERVER_NAME =
            "ai-agent-amap-mcp-server";

    private static final String PEXELS_MCP_SERVER_NAME =
            "ai-agent-pexelssearch-mcp-server";

    /**
     * 注册本地旅行规划工具。
     */
    @Bean
    public ToolCallbackProvider travelToolCallbackProvider(
            BudgetCalculatorTool budgetCalculatorTool,
            BudgetCheckTool budgetCheckTool,
            ConflictCheckTool conflictCheckTool,
            ItineraryGenerateTool itineraryGenerateTool,
            ItineraryAdjustTool itineraryAdjustTool) {

        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        budgetCalculatorTool,
                        budgetCheckTool,
                        conflictCheckTool,
                        itineraryGenerateTool,
                        itineraryAdjustTool
                )
                .build();
    }

    /**
     * 本地旅行工具 + amap MCP + Pexels MCP服务端工具。
     * 保持 doChatWithMCP 原有行为。
     */
    @Bean
    public ToolCallback[] allTools(
            @Qualifier("travelToolCallbackProvider")
            ToolCallbackProvider travelToolCallbackProvider,
            @Qualifier("mcpSyncClients")
            List<McpSyncClient> mcpSyncClients) {

        ToolCallback[] travelTools =
                travelToolCallbackProvider.getToolCallbacks();

        ToolCallback[] amapTools =
                mcpToolsForServer(mcpSyncClients, AMAP_MCP_SERVER_NAME);

        ToolCallback[] pexelsMcpTools =
                mcpToolsForServer(mcpSyncClients, PEXELS_MCP_SERVER_NAME);

        return Stream.of(travelTools, amapTools, pexelsMcpTools)
                .flatMap(Arrays::stream)
                .toArray(ToolCallback[]::new);
    }

    /**
     * 单独的 Pexels MCP 服务端工具。
     * 供需要搜索旅游图片的业务方法使用。
     */
    @Bean
    public ToolCallback[] pexelsMcpTools(
            @Qualifier("mcpSyncClients")
            List<McpSyncClient> mcpSyncClients) {

        return mcpToolsForServer(
                mcpSyncClients,
                PEXELS_MCP_SERVER_NAME
        );
    }

    /**
     * 根据 MCP 服务端名称，获取对应的工具回调。
     */
    private ToolCallback[] mcpToolsForServer(
            List<McpSyncClient> clients,
            String serverName) {

        McpSyncClient client = clients.stream()
                .filter(c -> serverName.equals(c.getServerInfo().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "未找到已连接的 MCP 服务端: " + serverName
                                + "，可用服务端: "
                                + clients.stream()
                                .map(c -> c.getServerInfo().name())
                                .toList()
                ));

        return new SyncMcpToolCallbackProvider(client)
                .getToolCallbacks();
    }
}
