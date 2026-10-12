package com.skillradar.controller;

import com.skillradar.dto.QuizResult;
import com.skillradar.dto.QuizSubmitRequest;
import com.skillradar.service.QuizService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    @PreDestroy
    void shutdown() {
        streamExecutor.shutdownNow();
    }

    /** 一次性返回结果（脚本/测试用）；前端走下面的 /stream，能边判边显示进度。 */
    @PostMapping("/api/quiz-attempts")
    public QuizResult submit(@RequestBody QuizSubmitRequest request) {
        return quizService.submit(request);
    }

    /**
     * 同样的判分，但以 Server-Sent Events 推送：每判完一道题推一个 progress 事件 {done, total}，
     * 最后推 result 事件（完整 QuizResult），出错推 error 事件 {message}。
     * 简答题要等 AI，前端据此显示"AI 正在判分 3/8"，而不是干等一个"提交中…"。
     */
    @PostMapping(value = "/api/quiz-attempts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter submitStream(@RequestBody QuizSubmitRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);
        streamExecutor.execute(() -> {
            try {
                QuizResult result = quizService.submit(request,
                        (done, total) -> send(emitter, "progress", Map.of("done", done, "total", total)));
                send(emitter, "result", result);
                emitter.complete();
            } catch (Exception e) {
                log.warn("测评提交失败：{}", e.getMessage());
                String message = e.getMessage() != null ? e.getMessage() : "提交失败，请稍后重试";
                send(emitter, "error", Map.of("message", message));
                emitter.complete();
            }
        });
        return emitter;
    }

    /** 判分回调来自多个线程，SseEmitter 不保证并发 send 安全，统一加锁。用户中途关页面导致发送失败就忽略。 */
    private void send(SseEmitter emitter, String name, Object data) {
        synchronized (emitter) {
            try {
                emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException ignored) {
                // 客户端已断开
            }
        }
    }
}
