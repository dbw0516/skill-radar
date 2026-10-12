package com.skillradar.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 线上 API（OpenAI 兼容格式）的薄封装，简答题判分和 AI 模拟面试共用。
 * 只提供"要 JSON 输出"这一种调用：两处用法都要从回复里取结构化字段，json_object 模式保证可解析。
 * 密钥从 backend/.env（或环境变量）的 DEEPSEEK_API_KEY 读取，见 application.yml。
 */
@Slf4j
@Component
public class DeepSeekClient {

    private final RestClient restClient;
    private final String model;
    private final boolean hasApiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeepSeekClient(
            @Value("${app.deepseek.base-url}") String baseUrl,
            @Value("${app.deepseek.model}") String model,
            @Value("${app.deepseek.api-key:}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .requestFactory(factory)
                .build();
        this.model = model;
        this.hasApiKey = apiKey != null && !apiKey.isBlank();
        if (!hasApiKey) {
            log.warn("没有配置 DEEPSEEK_API_KEY，简答题 AI 判分和 AI 模拟面试不可用（选择题/填空题不受影响）");
        }
    }

    /** 单轮：system + 一条 user 消息。 */
    public JsonNode chatJson(String systemPrompt, String userPrompt, double temperature) {
        return chatJson(systemPrompt, List.of(Map.of("role", "user", "content", userPrompt)), temperature);
    }

    /**
     * 多轮：system + 若干 {role, content}。返回模型输出解析成的 JSON。
     * 网络抖动/偶发 5xx 重试一次——简答题现在是并发判分，一次测评里任何一道失败都会让整次提交失败，值得多给一次机会。
     */
    public JsonNode chatJson(String systemPrompt, List<Map<String, String>> messages, double temperature) {
        if (!hasApiKey) {
            throw new IllegalStateException("AI 服务未配置：请在 backend/.env 里填写 DEEPSEEK_API_KEY 后重启后端");
        }
        List<Map<String, String>> all = new ArrayList<>();
        all.add(Map.of("role", "system", "content", systemPrompt));
        all.addAll(messages);
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                // json_object 模式要求 prompt 里出现 "json" 字样，各调用方的 system prompt 都写了
                "response_format", Map.of("type", "json_object"),
                "temperature", temperature,
                "messages", all);

        Exception last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String raw = restClient.post().uri("/chat/completions").body(body).retrieve().body(String.class);
                String content = objectMapper.readTree(raw).path("choices").path(0).path("message").path("content").asText();
                return objectMapper.readTree(content);
            } catch (Exception e) {
                last = e;
                log.warn("DeepSeek 调用失败（第 {} 次）：{}", attempt + 1, e.getMessage());
            }
        }
        throw new IllegalStateException("AI 服务调用失败，请检查网络和 DEEPSEEK_API_KEY 是否有效（余额是否充足）", last);
    }
}
