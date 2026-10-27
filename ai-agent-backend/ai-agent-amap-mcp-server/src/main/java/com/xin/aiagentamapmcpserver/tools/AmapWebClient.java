package com.xin.aiagentamapmcpserver.tools;

import com.xin.aiagentamapmcpserver.tools.common.AmapApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class AmapWebClient {

    private static final String AMAP_BASE_URL = "https://restapi.amap.com";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String key;

    private final AtomicLong requestCount = new AtomicLong();

    public AmapWebClient(@Value("${amap.web.key:}") String key) {
        this.key = key == null ? "" : key.trim();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.client = RestClient.builder()
                .baseUrl(AMAP_BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    public String get(String path, Map<String, ?> params) {
        long seq = requestCount.incrementAndGet();
        long start = System.currentTimeMillis();
        if (key.isBlank()) {
            throw new AmapApiException("未配置高德 Web 服务 Key，请配置 amap.web.key 或环境变量 AMAP_WEB_KEY");
        }
        try {
            String body = client.get()
                    .uri(builder -> {
                        builder.path(path).queryParam("key", key);
                        params.forEach((k, v) -> {
                            if (v != null && !v.toString().isBlank()) {
                                builder.queryParam(k, v);
                            }
                        });
                        return builder.build();
                    })
                    .retrieve()
                    .body(String.class);
            assertAmapSuccess(body);
            log.info("[amap#{}] GET {} success in {}ms, respLen={}",
                    seq, path, System.currentTimeMillis() - start, body == null ? 0 : body.length());
            return body;
        } catch (AmapApiException e) {
            log.warn("[amap#{}] GET {} business error in {}ms: {}",
                    seq, path, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[amap#{}] GET {} failed in {}ms", seq, path, System.currentTimeMillis() - start, e);
            throw new AmapApiException("高德接口调用异常：" + e.getMessage(), e);
        }
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    private void assertAmapSuccess(String body) {
        if (body == null || body.isBlank()) {
            throw new AmapApiException("高德接口返回空响应");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if ("0".equals(root.path("status").asText(""))) {
                String infocode = root.path("infocode").asText("");
                String info = root.path("info").asText("未知错误");
                throw new AmapApiException("高德接口返回失败：infocode=" + infocode + ", info=" + info);
            }
        } catch (AmapApiException e) {
            throw e;
        } catch (Exception ignored) {
            // 非 JSON 响应不做业务校验，原样透传
        }
    }
}