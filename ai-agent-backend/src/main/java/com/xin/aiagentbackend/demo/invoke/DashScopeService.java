/*
package com.xin.aiagentbackend.demo.invoke;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.Constants;
import com.xin.aiagentbackend.config.DashScopeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

//@Service
public class DashScopeService {

    @Autowired
    private DashScopeConfig config;

    public String simpleChat(String prompt) {
        try {
            MultiModalConversation conversation = new MultiModalConversation();

            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(Collections.singletonList(
                            Collections.singletonMap("text", prompt)))
                    .build();

            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(config.getApiKey())
                    .model(config.getModel())
                    .messages(Collections.singletonList(userMessage))
                    .build();

            MultiModalConversationResult result = conversation.call(param);

            // 防止返回为空
            if (result == null
                    || result.getOutput() == null
                    || result.getOutput().getChoices() == null
                    || result.getOutput().getChoices().isEmpty()) {
                throw new RuntimeException("模型未返回任何内容");
            }

            return (String) result.getOutput()
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent()
                    .get(0)
                    .get("text");

        } catch (NoApiKeyException e) {
            throw new RuntimeException("百炼 API Key 未配置或无效", e);

        } catch (UploadFileException e) {
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);

        } catch (ApiException e) {
            // 不要 JsonUtils.toJson(e)
            e.printStackTrace();
            throw new RuntimeException("百炼接口调用失败：" + e.getMessage(), e);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("调用模型失败：" + e.getMessage(), e);
        }
    }
}*/
