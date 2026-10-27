package com.xin.aiagentbackend.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.xin.aiagentbackend.agent.common.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent{

    // 可用工具
    private final ToolCallback[] availableTools;

    // 保存工具调用信息的响应结果（要调用那些工具）
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .multiModel(true)          // 与 DashScopeConfig 保持一致
                .internalToolExecutionEnabled(false)
                .build();
    }

    @Override
    protected String finalizeAtStepLimit() {
        List<Message> history = getMessageList();
        if (history.isEmpty() || !(history.get(history.size() - 1) instanceof ToolResponseMessage)) {
            return getLastAssistantText();
        }
        boolean lastAssistantRequestedTools = false;
        for (int i = history.size() - 2; i >= 0; i--) {
            if (history.get(i) instanceof AssistantMessage assistant) {
                lastAssistantRequestedTools = assistant.hasToolCalls();
                break;
            }
        }
        if (!lastAssistantRequestedTools) return getLastAssistantText();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode itinerary = null;
            JsonNode budget = null;
            JsonNode conflicts = null;
            String toolFailure = null;
            for (int i = 0; i < history.size(); i++) {
                if (!(history.get(i) instanceof ToolResponseMessage toolResponses)) continue;
                for (ToolResponseMessage.ToolResponse response : toolResponses.getResponses()) {
                    String name = response.name();
                    JsonNode result = mapper.readTree(response.responseData());
                    if (result.has("success") && !result.path("success").asBoolean()) {
                        toolFailure = result.path("message").asText("某项检查没有完成");
                        continue;
                    }
                    JsonNode data = result.path("data");
                    if ("generateItinerary".equals(name) && data.path("days").isArray()) itinerary = data;
                    if ("checkBudget".equals(name) && data.has("estimatedTotal")) budget = data;
                    if ("checkConflicts".equals(name) && data.has("hasConflict")) conflicts = data;
                }
            }
            if (itinerary == null) return getLastAssistantText();

            StringBuilder summary = new StringBuilder();
            summary.append(itinerary.path("city").asText("目的地"))
                    .append("行程已整理（")
                    .append(itinerary.path("startDate").asText("日期待定"))
                    .append("起，共 ").append(itinerary.path("days").size()).append(" 天）。\n");
            for (JsonNode day : itinerary.path("days")) {
                summary.append("\n第").append(day.path("day").asInt()).append("天 ")
                        .append(day.path("date").asText()).append("：");
                List<String> activities = new java.util.ArrayList<>();
                for (JsonNode activity : day.path("activities")) {
                    activities.add(activity.path("startTime").asText("") + " "
                            + activity.path("name").asText("活动待定")
                            + "（¥" + activity.path("estimatedCost").asDouble(0) + "）");
                }
                summary.append(String.join("；", activities));
            }
            if (conflicts != null) {
                summary.append("\n\n时间检查：").append(conflicts.path("hasConflict").asBoolean()
                        ? "发现冲突，需按建议调整。" : "未发现明显时间冲突；通勤时间仍需以地图路线核实。");
            }
            if (budget != null) {
                summary.append("\n预算检查：目标 ¥").append(budget.path("budget").asDouble())
                        .append("，已估算 ¥").append(budget.path("estimatedTotal").asDouble())
                        .append("，剩余 ¥").append(budget.path("remainingBudget").asDouble())
                        .append(budget.path("overBudget").asBoolean() ? "，已超预算。" : "，当前未超预算。");
                String warning = budget.path("warning").asText("");
                if (!warning.isBlank()) summary.append(warning);
            } else {
                summary.append("\n预算尚未完成检查，请继续在对话中核算。");
            }
            if (toolFailure != null) summary.append("\n部分工具未完成：").append(toolFailure);
            String answer = summary.toString();
            history.add(new AssistantMessage(answer));
            log.info("Agent reached its tool-call limit; summarized structured tool results");
            return answer;
        } catch (Exception error) {
            log.warn("Could not build a structured final summary from tool results", error);
            return getLastAssistantText();
        }
    }


    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {

        // 1. 请求模型时动态组合系统提示词
        String systemPrompt = getSystemPrompt()
                + "\n\n"
                + getNextStepPrompt()
                + "\n\n"
                + getExecutionContext().buildContextPrompt();
        // 用户请求生成/规划行程时，追加硬性要求：必须走工具链，禁止凭空编写行程文字。
        // 放在提示词末尾以取得最强约束效果（多轮对话中模型容易仅凭历史直接作答）。
        if (isItineraryRequest(getLastUserText())) {
            systemPrompt = systemPrompt + "\n\n## 本次对话硬性要求\n"
                    + "用户当前请求涉及生成/规划旅行行程，你必须调用工具逐步完成："
                    + "先 searchAttractions 搜索真实景点，再 generateItinerary 生成结构化行程，"
                    + "最后按需调用 checkBudget、checkConflicts 校验；"
                    + "即使对话中之前已经给出过文字建议，只要用户要求的是行程/计划，就必须走工具流程，"
                    + "禁止不调用工具直接编写行程文字。";
        }

        // 2. 调用 AI 大模型，获取工具调用结果
        List<Message> messagesList = super.getMessageList();
        Prompt prompt = new Prompt(messagesList, this.chatOptions);
        DashScopeChatOptions promptOptions = (DashScopeChatOptions) prompt.getOptions();
        log.info("[模型调用] Agent 请求级模型选项: {}（null表示继承application.yml中的默认模型）", promptOptions.getModel());


        try {
            // 获取带工具选项的响应
            ChatResponse chatResponse = getChatClient()
                    .prompt(prompt)
                    .system(systemPrompt)
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();

            log.info("[模型调用] Agent 响应模型: {}, responseId: {}",
                    chatResponse.getMetadata().getModel(), chatResponse.getMetadata().getId());

            // 记录响应, 供act 用
            this.toolCallChatResponse = chatResponse;

            // 3. 解析工具调用结果，获取想要的工具
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();

            // 如果不需要调用工具，返回 false
            if (toolCalls.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                setState(AgentState.FINISHED);
                return false;
            }

            // 4. 检查本轮工具调用是否会造成重复执行
            if (!super.getExecutionContext().allowToolCalls(toolCalls)) {
                log.warn("检测到重复工具调用，停止当前任务");

                getMessageList().add(new AssistantMessage(
                        "检测到重复工具调用：相同工具和参数已经执行多次。"
                                + "为避免无限循环，任务已停止。"
                                + "请根据已有执行结果总结当前进展，不要再次执行相同操作。"
                ));

                setState(AgentState.FINISHED);
                return false;
            } else {
                // 输出提示信息
                String result = assistantMessage.getText();
                log.info("{}的思考：{}", getName(), result);
                log.info("{}选择了 {} 个工具来使用", getName(), toolCalls.size());
                String toolCallInfo = toolCalls.stream()
                        .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                        .collect(Collectors.joining("\n"));
                log.info(toolCallInfo);
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error( "{}的思考过程遇到了问题：{}", getName(), e.getMessage());
            setState(AgentState.ERROR);
            getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            return false;
        }

    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (toolCallChatResponse == null
                || !toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }

        Prompt prompt = new Prompt(
                getMessageList(), this.chatOptions);

        ToolExecutionResult executionResult =
                toolCallingManager.executeToolCalls(
                        prompt, toolCallChatResponse);

        // 换了一个新 List
        setMessageList(executionResult.conversationHistory());

        Message lastMessage = CollUtil.getLast(
                executionResult.conversationHistory());

        if (!(lastMessage instanceof ToolResponseMessage toolResponse)) {
            setState(AgentState.ERROR);
            throw new IllegalStateException(
                    "工具执行后没有返回预期的工具结果消息");
        }

        String results = toolResponse.getResponses().stream()
                .map(response -> "工具 " + response.name()
                        + " 返回的结果：" + response.responseData())
                .collect(Collectors.joining("\n"));

        log.info("工具执行结果：{}", results);

        // 记录执行摘要
        getExecutionContext().appendSummary(results);

        // 检查连续无进展
        if (!getExecutionContext().recordResult(results)) {
            log.warn("连续多轮没有取得进展，停止 Agent");
            setState(AgentState.FINISHED);
            return results + "\n任务已停止：连续多轮没有取得新的进展。";
        }

        // 识别终止工具
        boolean terminateToolCalled = toolResponse.getResponses()
                .stream()
                .anyMatch(response ->
                        "doTerminate".equals(response.name()));

        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }

        return results;
    }

    /**
     * 用户消息中的行程请求信号。
     * <p>用于识别"生成/规划行程"类诉求（含多轮对话中的后续轮次），
     * 避免模型仅凭历史直接输出行程文字而绕过工具链。</p>
     */
    private static final Pattern ITINERARY_REQUEST = Pattern.compile(
            "行程|旅行计划|旅游计划|旅游攻略|规划|制定.{0,8}(?:行程|计划)|生成.{0,12}(?:行程|计划|攻略)|[一二两三四五六七八九十\\d]{1,3}日游");

    /** 续跑只允许一次，防止模型一直不调用工具时无限循环 */
    private boolean continuationAttempted = false;

    /** 当前消息是否要求生成/规划行程 */
    private boolean isItineraryRequest(String text) {
        return text != null && ITINERARY_REQUEST.matcher(text).find();
    }

    /** 取消息列表中最后一条非空用户消息（多轮对话时即本轮请求） */
    private String getLastUserText() {
        List<Message> messages = getMessageList();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage user) {
                String text = user.getText();
                if (text != null && !text.isBlank()) return text;
            }
        }
        return "";
    }

    /** 本次运行中是否已真正调用过 generateItinerary 工具 */
    private boolean itineraryGenerated() {
        for (Message message : getMessageList()) {
            if (message instanceof ToolResponseMessage toolResponses) {
                for (ToolResponseMessage.ToolResponse response : toolResponses.getResponses()) {
                    if ("generateItinerary".equals(response.name())) return true;
                }
            }
        }
        return false;
    }

    /**
     * 正常结束时，若用户要求生成行程但本轮未调用行程工具，则需要补一轮强制执行。
     */
    @Override
    protected boolean shouldContinueAfterFinish() {
        if (continuationAttempted) return false;
        return isItineraryRequest(getLastUserText()) && !itineraryGenerated();
    }

    /** 追加纠正指令并把状态置回 RUNNING，让 Agent 再走一轮工具流程 */
    @Override
    protected void prepareContinuation() {
        continuationAttempted = true;
        getMessageList().add(new UserMessage(
                "【系统指令】用户当前请求是生成/规划旅行行程，但你本轮尚未调用任何行程生成工具。"
                        + "请立即调用工具完成行程：先 searchAttractions 搜索真实景点，"
                        + "再 generateItinerary 生成结构化行程，最后按需调用 checkBudget、checkConflicts 校验；"
                        + "禁止不调用工具直接编写行程文字。"));
        setState(AgentState.RUNNING);
        log.info("Agent finished without generating an itinerary; forcing a tool-driven continuation");
    }
}
