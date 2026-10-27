package com.xin.aiagentbackend.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.aiagentbackend.tools.common.AmapApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AmapWebClient {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String key;
    private final AtomicLong requestCount = new AtomicLong();

    public AmapWebClient(@Value("${amap.web.key:}") String key, ObjectMapper mapper) {
        this.key = key == null ? "" : key.trim();
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.client = RestClient.builder().baseUrl("https://restapi.amap.com").requestFactory(factory).build();
    }

    public String get(String path, Map<String, ?> params) {
        requestCount.incrementAndGet();
        if (key.isBlank()) throw new AmapApiException("未配置高德 Web 服务 Key，请设置 AMAP_WEB_KEY");
        try {
            String body = client.get().uri(builder -> {
                builder.path(path).queryParam("key", key);
                params.forEach((name, value) -> { if (value != null && !value.toString().isBlank()) builder.queryParam(name, value); });
                return builder.build();
            }).retrieve().body(String.class);
            if (body == null || body.isBlank()) throw new AmapApiException("高德接口返回空响应");
            JsonNode root = mapper.readTree(body);
            if ("0".equals(root.path("status").asText())) {
                throw new AmapApiException("高德接口失败：" + root.path("infocode").asText() + " " + root.path("info").asText());
            }
            return body;
        } catch (AmapApiException e) {
            throw e;
        } catch (Exception e) {
            throw new AmapApiException("高德接口调用异常：" + e.getMessage(), e);
        }
    }

    public long getRequestCount() { return requestCount.get(); }
}
