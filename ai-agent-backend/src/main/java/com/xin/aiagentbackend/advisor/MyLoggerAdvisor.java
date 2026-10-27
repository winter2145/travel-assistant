package com.xin.aiagentbackend.advisor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    private final LogLevel level;

    public MyLoggerAdvisor(MyLoggerProperties properties) {
        this.level = properties.getLevel();
    }

    @Override
    public ChatClientResponse adviseCall(
            ChatClientRequest request,
            CallAdvisorChain chain) {

        // 1. 请求日志
        logRequest(request);

        // 2. 继续执行 Advisor 链
        ChatClientResponse response = chain.nextCall(request);

        // 3. 响应日志
        logResponse(response);

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
            ChatClientRequest request,
            StreamAdvisorChain chain) {
        // 1. 请求日志
        logRequest(request);

        // 2. 继续执行流式 Advisor 链
        Flux<ChatClientResponse> responses =
                chain.nextStream(request);

        // 3. 聚合完整响应后，只打印一次
        return new ChatClientMessageAggregator()
                .aggregateChatClientResponse(
                        responses,
                        this::logResponse
                );
    }

    /**
     * 输出用户真正输入的文本
     * @param request
     */
    private void logRequest(ChatClientRequest request) {

        String userText = request.prompt()
                .getUserMessage()
                .getText();

        logMessage("用户 Request: {}", userText);
    }

    /**
     * 输出 AI 最终返回的文本
     * @param response
     */
    private void logResponse(ChatClientResponse response) {

        String aiText = response.chatResponse()
                .getResult()
                .getOutput()
                .getText();

        logMessage("AI Response: {}", aiText);
    }

    private void logMessage(String message, Object... args) {

        switch (level) {
            case DEBUG -> log.debug(message, args);
            case INFO -> log.info(message, args);
            case WARN -> log.warn(message, args);
            case ERROR -> log.error(message, args);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
