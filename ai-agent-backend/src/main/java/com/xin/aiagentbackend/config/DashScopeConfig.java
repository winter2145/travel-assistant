package com.xin.aiagentbackend.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class DashScopeConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Value("${spring.ai.dashscope.chat.options.model}")
    private String model;

    // 1. 手动注册 DashScopeApi 到Spring容器
    @Bean
    public DashScopeApi dashScopeApi() {
        return DashScopeApi.builder()
                .apiKey(dashScopeApiKey)
                .build();
    }

    /**
     * 让大模型生成回答
     * @param dashScopeApi
     * @return
     */
    @Bean
    public DashScopeChatModel dashScopeChatModel(DashScopeApi dashScopeApi) {

        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .model(model)
                // 核心！！开启多模态模式，切换到multimodal-generation接口
                .multiModel(true)
                .build();

        log.info("[DashScopeBean初始化] 本次绑定模型：{}", model);

        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(options)
                .build();
    }

    /**
     * 把文本转换成向量
     * @param dashScopeApi
     * @return
     */
    @Bean
    public DashScopeEmbeddingModel dashScopeEmbeddingModel(DashScopeApi dashScopeApi) {
        return DashScopeEmbeddingModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
    }
}
