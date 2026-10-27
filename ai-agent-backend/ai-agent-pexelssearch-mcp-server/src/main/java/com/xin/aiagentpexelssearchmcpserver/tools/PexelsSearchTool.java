package com.xin.aiagentpexelssearchmcpserver.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Pexels 图片搜索工具。
 *
 * 技术栈：Spring Boot 3.5.x + Spring AI 1.x。
 * API Key 通过配置项 pexels.api-key 注入，不要硬编码在源码中。
 */
@Component
public class PexelsSearchTool {

    private static final Logger log =
            LoggerFactory.getLogger(PexelsSearchTool.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public PexelsSearchTool(
            ObjectMapper objectMapper,
            @Value("${pexels.api-key:}") String apiKey) {

        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(20));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.pexels.com")
                .requestFactory(requestFactory)
                .defaultHeader("Accept", "application/json");

        // 没有配置 Key 时不添加 Authorization 请求头。
        // 调用工具时会给出明确提示，而不是发起必然失败的请求。
        if (!this.apiKey.isBlank()) {
            builder.defaultHeader("Authorization", this.apiKey);
        }

        this.restClient = builder.build();
    }

    /**
     * 搜索 Pexels 图片。
     *
     * @param keyword 搜索关键词，例如 Kyoto temple 或 Tokyo Tower
     * @return 图片地址、摄影师、Pexels 图片页面和替代文本
     */
    @Tool(description = "搜索 Pexels 旅游景点图片，返回图片地址、摄影师和图片页面链接")
    public String searchImages(
            @ToolParam(description = "图片搜索关键词，建议使用景点英文名称，例如 Tokyo Tower")
            String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return "图片搜索关键词不能为空。";
        }

        if (apiKey.isBlank()) {
            return "Pexels 图片搜索暂不可用：尚未配置 API Key。请在 application.yml 中配置 pexels.api-key，"
                    + "或通过环境变量 PEXELS_API_KEY 注入。注意：Pexels 官方目前暂停签发新的 API Key。";
        }

        try {
            URI requestUri = UriComponentsBuilder
                    .fromPath("/v1/search")
                    .queryParam("query", keyword.trim())
                    .queryParam("per_page", 3)
                    .queryParam("locale", "zh-CN")
                    .build()
                    .encode()
                    .toUri();

            String json = restClient.get()
                    .uri(requestUri)
                    .retrieve()
                    .body(String.class);

            if (json == null || json.isBlank()) {
                log.warn("Pexels API 返回空响应，keyword={}", keyword);
                return "Pexels 图片搜索服务返回了空响应，请稍后重试。";
            }

            JsonNode root = objectMapper.readTree(json);
            JsonNode photos = root.path("photos");
            List<String> results = new ArrayList<>();

            if (photos.isArray()) {
                for (JsonNode photo : photos) {
                    String id = photo.path("id").asText("");
                    String photoPageUrl = photo.path("url").asText("");
                    String photographer = photo.path("photographer").asText("未知");
                    String photographerUrl = photo.path("photographer_url").asText("");
                    String alt = photo.path("alt").asText("");

                    JsonNode src = photo.path("src");
                    // 优先返回 large 图片；如果没有则依次回退到 medium、original。
                    String imageUrl = src.path("large").asText("");
                    if (imageUrl.isBlank()) {
                        imageUrl = src.path("medium").asText("");
                    }
                    if (imageUrl.isBlank()) {
                        imageUrl = src.path("original").asText("");
                    }

                    if (imageUrl.isBlank()) {
                        continue;
                    }

                    results.add(
                            "图片 ID：" + id
                                    + "\n图片地址：" + imageUrl
                                    + "\nPexels 页面：" + photoPageUrl
                                    + "\n摄影师：" + photographer
                                    + "\n摄影师主页：" + photographerUrl
                                    + "\n图片描述：" + (alt.isBlank() ? "暂无描述" : alt)
                                    + "\n图片来源：Pexels"
                    );
                }
            }

            if (results.isEmpty()) {
                log.info("Pexels 图片搜索无结果，keyword={}", keyword);
                return "Pexels 没有找到相关图片，请尝试英文景点名称或更换关键词：" + keyword;
            }

            log.info("Pexels 图片搜索成功，keyword={}，结果数={}",
                    keyword, results.size());

            return String.join("\n\n", results);

        } catch (RestClientResponseException e) {
            log.error("Pexels API 返回 HTTP 错误，keyword={}，status={}",
                    keyword, e.getStatusCode(), e);

            if (e.getStatusCode().value() == 401) {
                return "Pexels 图片搜索授权失败：API Key 无效或未获授权，请检查 pexels.api-key 配置。";
            }
            if (e.getStatusCode().value() == 429) {
                return "Pexels 图片搜索请求次数已达到限额，请稍后再试。";
            }

            return "Pexels 图片搜索失败，HTTP 状态码："
                    + e.getStatusCode().value();

        } catch (Exception e) {
            log.error("Pexels 图片搜索失败，keyword={}", keyword, e);
            return "Pexels 图片搜索暂时失败。错误类型："
                    + e.getClass().getSimpleName()
                    + "；原因：" + safeMessage(e);
        }
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return "未提供详细错误信息";
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }
}
