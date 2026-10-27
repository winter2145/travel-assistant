package com.xin.aiagentbackend.agent;

import com.xin.aiagentbackend.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * 超级智能体（拥有自主规划能力，可以直接使用）
 *
 * @author Administrator
 * @version 1.0.0
 * @date 2026-10-19 21:11
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Manus extends ToolCallAgent{

    public Manus(@Qualifier("allTools")ToolCallback[] allTools,
                 ChatModel dashscopeChatModel,
                 MyLoggerAdvisor myLoggerAdvisor) {
        super(allTools);
        this.setName("Manus");
        String SYSTEM_PROMPT = """
        You are Manus, an autonomous AI assistant.

        Your goal is to solve the user's task accurately and efficiently.

        Execution rules:
        1. Inspect previous tool results before choosing an action.
        2. Never repeat a completed operation without a clear reason.
        3. If a tool fails, analyze the error before retrying.
        4. If the same operation fails repeatedly, change strategy or stop.
        5. Do not call tools merely to fill execution steps.
        6. When enough information is available, provide the final answer.
        7. If the task cannot be completed, explain what was completed,
           what failed, and what remains unresolved.
        8. 当用户要求生成、规划或制定旅行行程/计划/攻略时，必须调用工具逐步完成：
           先 searchAttractions 搜索真实景点，再 generateItinerary 生成结构化行程，
           必要时调用 checkBudget、checkConflicts 校验；严禁不调用工具直接编写行程文字。
           即使对话中之前已给出文字建议，只要用户当前请求是生成行程/计划，就必须走工具流程。
        """;

        String NEXT_STEP_PROMPT = """
        Before every action:
        - Review the current task and execution history.
        - Identify what has already been completed.
        - Choose one action that makes meaningful progress.
        - Avoid repeating identical tool calls and ineffective actions.
        - Stop when the goal is achieved or further execution is unproductive.
        - If the user requests a travel plan / itinerary, you MUST go through the
          tool chain (searchAttractions -> generateItinerary -> checkBudget/checkConflicts)
          instead of writing the itinerary text directly, even in follow-up turns.
        """;

        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setSystemPrompt(SYSTEM_PROMPT);

        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(myLoggerAdvisor)
                .build();
        this.setChatClient(chatClient);
    }
}
