package com.xin.aiagentbackend.agent;


import cn.hutool.core.util.StrUtil;
import com.xin.aiagentbackend.agent.common.AgentExecutionContext;
import com.xin.aiagentbackend.agent.common.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 *
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示
    private String systemPrompt;
    private String nextStepPrompt;

    // 执行控制
    private final int maxStep = 4;
    private int currentStep = 0;

    // LLM
    private ChatClient chatClient;

    // 代理状态
    private AgentState state = AgentState.IDLE;

    // Memory（需要自主维护会话上下文）
    private List<Message> messageList = new ArrayList<>();

    private AgentExecutionContext executionContext =
            new AgentExecutionContext();

    protected void beginTask(String userPrompt) {
        beginTask(userPrompt, Collections.emptyList());
    }

    protected void beginTask(String userPrompt, List<Message> history) {
        currentStep = 0;
        executionContext.reset();
        messageList = new ArrayList<>(history == null ? Collections.emptyList() : history);
        messageList.add(new UserMessage(userPrompt));
        state = AgentState.RUNNING;
    }

    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {

        // 1、基础校验
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 2、执行，更改状态
        beginTask(userPrompt);
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            // 执行循环；若子类判定任务需要续跑（如行程请求未走工具链），再补一轮
            boolean needsContinuation;
            do {
                needsContinuation = false;
                for (int i = 0; i < maxStep
                        && state != AgentState.FINISHED
                        && state != AgentState.ERROR; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxStep);
                    // 单步执行
                    String stepResult = step();
                    String result = "Step " + stepNumber + ": " + stepResult;
                    results.add(result);

                    if (state == AgentState.FINISHED || state == AgentState.ERROR) break;
                    if ("思考完成 - 无需行动".equals(stepResult)) break;

                }
                if (state == AgentState.FINISHED && shouldContinueAfterFinish()) {
                    prepareContinuation();
                    needsContinuation = true;
                }
            } while (needsContinuation);
            // 检查是否超出步骤限制
            boolean finishedByState = state == AgentState.FINISHED;
            boolean reachedStepLimit = !finishedByState && currentStep >= maxStep;
            if (reachedStepLimit) {
                state = AgentState.FINISHED;
                finalizeAtStepLimit();
            }
            // 循环结束后
            if (state == AgentState.FINISHED) {
                String finalAnswer = getLastAssistantText();
                if (StrUtil.isNotBlank(finalAnswer)) {
                    results.add(finalAnswer);   // 最后一条
                }
            }

            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 3、清理资源
            this.cleanUp();
        }

    }

    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public SseEmitter runStream(String userPrompt) {
        return runStream(userPrompt, Collections.emptyList());
    }

    /** Run with previous user/assistant turns restored from the conversation store. */
    public SseEmitter runStream(String userPrompt, List<Message> history) {

        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(300000L); // 5 分钟超时
        // 客户端断开/超时/出错时置为 true，工作线程据此停止后续 LLM 与工具调用（防止继续产生费用）
        AtomicBoolean cancelled = new AtomicBoolean(false);
        // 使用线程异步处理，避免阻塞主线程；保留引用以便取消时中断
        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            // 1、基础校验
            try {
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误：不能使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }

                // 2、执行，更改状态
                beginTask(userPrompt, history);
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
                return;
            }

            // 保存结果列表
            List<String> results = new ArrayList<>();
            try {
                // 执行循环；若子类判定任务需要续跑（如行程请求未走工具链），再补一轮
                boolean needsContinuation;
                do {
                    needsContinuation = false;
                    for (int i = 0; i < maxStep
                            && state != AgentState.FINISHED
                            && state != AgentState.ERROR
                            && !cancelled.get(); i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("Executing step {}/{}", stepNumber, maxStep);
                        // 单步执行
                        String stepResult = step();
                        String result = "Step " + stepNumber + ": " + stepResult;
                        results.add(result);
                        // 输出当前每一步的结果到 SSE（连接已断开时抛异常，视为客户端取消）
                        if (cancelled.get()) break;
                        sseEmitter.send(result);

                        if (state == AgentState.FINISHED || state == AgentState.ERROR) break;
                        if ("思考完成 - 无需行动".equals(stepResult)) break;

                    }
                    if (state == AgentState.FINISHED && shouldContinueAfterFinish()) {
                        prepareContinuation();
                        needsContinuation = true;
                    }
                } while (needsContinuation && !cancelled.get());

                if (cancelled.get()) {
                    // 客户端已断开或主动停止：不再发送任何事件，静默收尾
                    try {
                        sseEmitter.complete();
                    } catch (Exception ignored) {
                        // 连接已断开，complete 失败可忽略
                    }
                    return;
                }

                // 检查是否超出步骤限制
                boolean finishedByState = state == AgentState.FINISHED;
                boolean reachedStepLimit = !finishedByState && currentStep >= maxStep;
                if (reachedStepLimit) {
                    state = AgentState.FINISHED;
                    sseEmitter.send("正在汇总已获得的信息");
                    finalizeAtStepLimit();
                }
                // 循环结束后
                if (state == AgentState.FINISHED) {
                    String finalAnswer = getLastAssistantText();
                    if (StrUtil.isNotBlank(finalAnswer)) {
                        sseEmitter.send(SseEmitter.event()
                                .name("answer")          // 用 event 名区分
                                .data(finalAnswer));
                    }
                }
                // A named terminal event lets fetch based SSE consumers distinguish a
                // successful agent run from a failed run or an interrupted transport.
                if (state == AgentState.FINISHED) {
                    sseEmitter.send(SseEmitter.event().name("done").data("completed"));
                } else {
                    sseEmitter.send(SseEmitter.event().name("error").data("执行错误：规划请求失败，请稍后重试"));
                }
                // 正常完成
                sseEmitter.complete();
            } catch (IOException connectionLost) {
                // 客户端断开连接导致的写入失败：中断任务，不再向已断开的连接发送错误事件
                cancelled.set(true);
                log.warn("SSE connection lost while streaming, agent run cancelled");
                try {
                    sseEmitter.complete();
                } catch (Exception ignored) {
                    // 连接已断开，complete 失败可忽略
                }
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("error executing agent", e);
                try {
                    sseEmitter.send(SseEmitter.event().name("error").data("执行错误：规划请求失败，请稍后重试"));
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                // 3、清理资源
                this.cleanUp();
            }
        });

        // 中断运行中的任务：置取消标记并打断工作线程，阻止后续 LLM/工具调用继续产生费用
        Runnable cancel = () -> {
            if (cancelled.compareAndSet(false, true)) {
                log.warn("SSE connection closed, cancelling agent run");
                task.cancel(true);   // 中断工作线程（对进行中的模型请求尽力而为）
                this.cleanUp();
            }
        };
        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            cancel.run();
            log.warn("SSE connection timeout");
        });
        // 客户端断开连接：Spring 容器会触发 onError（部分容器触发 onCompletion），两者都按取消处理
        sseEmitter.onError(error -> cancel.run());
        // 设置完成回调；正常运行结束时状态已由执行循环置为 FINISHED，这里不再改写，
        // 避免客户端中途断开时把未完成的运行误标记为成功（导致部分文本被持久化）
        sseEmitter.onCompletion(() -> {
            cancel.run();
            log.info("SSE connection completed");
        });
        return sseEmitter;
    }

    /**
     * 定义单个步骤
     *
     * @return
     */
    public abstract String step();

    /** Final response hook for agents that exhaust their action loop after a tool result. */
    protected String finalizeAtStepLimit() {
        return getLastAssistantText();
    }

    /**
     * 任务正常结束后是否还需要补一轮执行（子类按需覆写）。
     * 例如旅行 Agent 在用户要求生成行程、但本轮未走行程工具链时返回 true。
     */
    protected boolean shouldContinueAfterFinish() {
        return false;
    }

    /** 为续跑做准备：追加纠正指令并把状态置回 RUNNING（子类按需覆写）。 */
    protected void prepareContinuation() {
        setState(AgentState.RUNNING);
    }

    /**
     * 清理资源
     */
    protected void cleanUp(){
        //
    }

    /**
     * 输出最终结果
     *
     * @return 最终结果
     */
    public String getLastAssistantText() {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            if (messageList.get(i) instanceof AssistantMessage am) {
                String text = am.getText();
                if (StrUtil.isNotBlank(text)) return text;
            }
        }
        return "";
    }
}
