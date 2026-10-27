package com.xin.aiagentbackend.app;

import com.xin.aiagentbackend.advisor.MyLoggerAdvisor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    // 系统提示词，旅行规划智能体总约束
    private static final String SYSTEM_PROMPT = """
            你是专业旅行规划智能体，请严格遵循知识库内规则：
            1. 用户信息不全时按规范主动追问必填字段；
            2. 生成行程严格按照输出格式规范输出markdown；
            3. 参考场景化规则库适配不同人群、预算、游玩风格；
            4. 所有费用均为预估，提示用户以官方实时信息为准；
            5. 禁止输出高危、不合规出行方案。
            """;

    public LoveApp (ChatModel dashscopeChatModel,
                    MyLoggerAdvisor myLoggerAdvisor,
                    @Qualifier("localRetrievalAugmentationAdvisor")
                    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor){
        // 修改2：使用MessageWindowChatMemory替代InMemoryChatMemory
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)  // 设置最大消息数量
                .build();
        this.chatClient = ChatClient
                .builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 开启日志，便于观察效果
                        myLoggerAdvisor,

                        // 应用增强检索服务（云知识库服务）
                        //retrievalAugmentationAdvisor

                        // 使用@Qualifier指定本地 Markdown 知识库
                        retrievalAugmentationAdvisor

                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat (String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        log.info("[模型调用] 调用路径: doChat, 响应模型: {}, responseId: {}",
                chatResponse.getMetadata().getModel(), chatResponse.getMetadata().getId());
        String content = chatResponse.getResult().getOutput().getText();
//        log.info("content: {}", content);
        return content;
    }


    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ToolCallback[] pexelsMcpTools;

    @Resource
    private ToolCallTracker toolCallTracker;

    // AI 基础对话 (工具调用)
    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .toolCallbacks(recordingTools(allTools, chatId))
//                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        log.info("[模型调用] 调用路径: doChatWithTools, 响应模型: {}, responseId: {}",
                response.getMetadata().getModel(), response.getMetadata().getId());
        String content = response.getResult().getOutput().getText();
//        log.info("content: {}", content);
        return content;
    }

    /**
     * 包装全部工具回调：每个工具被 LLM 调用时，把工具名记录到 ToolCallTracker，
     * 便于集成测试验证 10 个工具类是否都已被实际调用。
     */
    private ToolCallback[] recordingTools(ToolCallback[] callbacks, String chatId) {
        ToolCallback[] wrapped = new ToolCallback[callbacks.length];
        for (int i = 0; i < callbacks.length; i++) {
            ToolCallback origin = callbacks[i];
            String toolName = origin.getToolDefinition().name();
            wrapped[i] = new ToolCallback() {
                @Override
                public String call(String toolInput) {
                    toolCallTracker.record(chatId, toolName);
                    return origin.call(toolInput);
                }

                @Override
                public ToolDefinition getToolDefinition() {
                    return origin.getToolDefinition();
                }
            };
        }
        return wrapped;
    }

    /**
     * 查询某会话实际被调用过的工具名称（供测试与观测使用）
     */
    public Set<String> getInvokedTools(String chatId) {
        return toolCallTracker.getInvokedTools(chatId);
    }

    /**
     * AI 基础对话（调用 MCP 服务）
     */
    public String doChatWithMCP (String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .toolCallbacks(recordingTools(allTools, chatId))
                .call()
                .chatResponse();
        log.info("[模型调用] 调用路径: doChatWithMCP, 响应模型: {}, responseId: {}",
                response.getMetadata().getModel(), response.getMetadata().getId());
        return response.getResult().getOutput().getText();

    }

    /**
     * AI 基础对话（调用 Pexels Search MCP 服务，SSE 连接）
     * 仅注册 wikimedia MCP 服务端（ai-agent-wikimedia-mcp-server）的工具，
     * 与 doChatWithMCP（amap）互不混用。
     */
    public String doChatWithPexelsSearchMCP (String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .toolCallbacks(recordingTools(pexelsMcpTools, chatId))
                .call()
                .chatResponse();
        log.info("[模型调用] 调用路径: doChatWithPexelsSearchMCP, 响应模型: {}, responseId: {}",
                response.getMetadata().getModel(), response.getMetadata().getId());
        return response.getResult().getOutput().getText();

    }

}
