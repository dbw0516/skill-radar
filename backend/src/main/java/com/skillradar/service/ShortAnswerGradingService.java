package com.skillradar.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 简答题 AI 判分：调用本机 Ollama（本地大模型），判断学生作答是否覆盖了参考答案的要点。
 * 用本地模型而不是云端 API，是因为团队这台共享主机有独显，本地推理零成本、不用管密钥；
 * 代价是换一台机器当共享主机时，那台机器需要自己装好 Ollama 并拉取 app.ollama.model 配置的模型。
 */
@Slf4j
@Service
public class ShortAnswerGradingService {

    private static final String SYSTEM_PROMPT = """
            你是客观公正的技术判卷助手。给定题目、参考答案要点、学生作答，判断学生作答是否体现出对
            参考答案核心机制的理解，不要求逐字匹配。

            判断标准：
            - 学生答案说出了参考答案里的核心机制即可算对，不需要覆盖参考答案里的每一条要点，
              抓住主要的一两个关键机制、即使遗漏了部分细节或措辞不同，也算对。
            - 如果学生答案含糊笼统、只是复述题目、承认自己不确定/不知道，或完全没有说出参考答案里
              任何具体要点，就算错。

            先在 key_points_covered 字段里说明学生答案覆盖了参考答案的哪些要点、遗漏了哪些，
            再给出最终判断 correct（先分析后下结论，不要跳过 key_points_covered 直接下结论）。
            只输出 JSON，不要输出其他文字，格式：
            {"key_points_covered": "...", "correct": true 或 false, "feedback": "一句话简评"}""";

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ShortAnswerGradingService(
            @Value("${app.ollama.base-url}") String baseUrl,
            @Value("${app.ollama.model}") String model) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(60)); // 模型冷启动加载进显存可能要几十秒
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.model = model;
    }

    public boolean isCorrect(String questionText, String referenceAnswer, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return false;
        }
        String userPrompt = "题目：" + questionText
                + "\n参考答案要点：" + referenceAnswer
                + "\n学生作答：" + studentAnswer;

        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "format", "json",
                // think:false —— qwen3 这类混合推理模型默认会先输出一大段思考过程再给结论，
                // 关掉后判断结果不变、但从 20+ 秒降到 1 秒内，值得关掉；不支持 thinking 的模型忽略此参数
                "think", false,
                // temperature 0：判分要的是同一份作答每次都给同一个结果，不要采样多样性
                "options", Map.of("temperature", 0),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)));

        try {
            String raw = restClient.post()
                    .uri("/api/chat")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode verdict = objectMapper.readTree(root.path("message").path("content").asText());
            boolean correct = verdict.path("correct").asBoolean(false);
            log.info("AI 判分 - 题目「{}」 correct={} feedback={}",
                    questionText, correct, verdict.path("feedback").asText(""));
            return correct;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "AI 判分服务调用失败，请确认本机 Ollama 已启动且已执行 `ollama pull " + model + "`", e);
        }
    }
}
