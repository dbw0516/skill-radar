package com.skillradar.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 简答题 AI 判分：调用 DeepSeek，判断学生作答是否覆盖了参考答案的要点。
 * 返回的不只是对错，还有 AI 对要点覆盖情况的分析和一句点评——交卷后逐题展示给用户，
 * 光告诉人"0/8"却不说错在哪，用户没法从测评里学到东西。
 *
 * gradeAsync 走一个固定大小的线程池并发判分：之前一道一道串行调，8 道简答题要等十几秒。
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
            feedback 是直接写给学生看的一句话：答对了就肯定他抓住的关键点，答错了就指出最关键的遗漏，
            语气友善、具体，不要空泛地说"继续加油"。
            只输出 JSON，不要输出其他文字，格式：
            {"key_points_covered": "...", "correct": true 或 false, "feedback": "..."}""";

    /** 同时在途的 DeepSeek 请求上限：一次测评最多 10 题，再留点余量给多人同时交卷。 */
    private static final int MAX_CONCURRENT_CALLS = 12;

    public record Grade(boolean correct, String feedback, String keyPointsCovered) {}

    private final DeepSeekClient deepSeekClient;
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_CALLS);

    public ShortAnswerGradingService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public CompletableFuture<Grade> gradeAsync(String questionText, String referenceAnswer, String studentAnswer) {
        return CompletableFuture.supplyAsync(() -> grade(questionText, referenceAnswer, studentAnswer), executor);
    }

    public Grade grade(String questionText, String referenceAnswer, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return new Grade(false, "没有作答。", null);
        }
        String userPrompt = "题目：" + questionText
                + "\n参考答案要点：" + referenceAnswer
                + "\n学生作答：" + studentAnswer;
        // temperature 0：判分要的是同一份作答每次都给同一个结果，不要采样多样性
        JsonNode verdict = deepSeekClient.chatJson(SYSTEM_PROMPT, userPrompt, 0);
        boolean correct = verdict.path("correct").asBoolean(false);
        String feedback = verdict.path("feedback").asText("");
        log.info("AI 判分 - 题目「{}」 correct={} feedback={}", questionText, correct, feedback);
        return new Grade(correct, feedback, verdict.path("key_points_covered").asText(null));
    }
}
