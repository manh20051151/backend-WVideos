package com.example.backendWVideos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client gọi Gemini API (Google AI Studio, free tier) dùng chung cho mọi tính năng dịch:
 * video, danh mục, tin tức...
 *
 * - Không có API key trong config -> tự tắt (isEnabled = false, không lỗi).
 * - Lỗi tạm thời (timeout, 503 quá tải, 429 rate limit) -> thử lại tối đa 3 lần.
 */
@Slf4j
@Component
public class GeminiApiClient {

    @Value("${app.translation.gemini-api-key:}")
    private String geminiApiKey;

    @Value("${app.translation.gemini-model:gemini-3.5-flash-lite}")
    private String geminiModel;

    // Các ngôn ngữ đích, phân tách bằng dấu phẩy. Bỏ "vi" vì là ngôn ngữ gốc.
    @Value("${app.translation.target-locales:en,zh-CN,ja,ko,hi,th,lo,km}")
    private String targetLocalesConfig;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean isEnabled() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    public List<String> getTargetLocales() {
        return Arrays.stream(targetLocalesConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("vi"))
                .collect(Collectors.toList());
    }

    /**
     * Gửi prompt, trả về text JSON Gemini sinh ra hoặc null nếu lỗi sau khi thử lại.
     */
    public String generateJson(String prompt) {
        if (!isEnabled()) {
            return null;
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.2)
        );

        String payload;
        try {
            payload = MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            log.error("🌐 [Gemini] Không serialize được request body: {}", e.getMessage());
            return null;
        }

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(120))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 503 || response.statusCode() == 429) {
                    log.warn("🌐 [Gemini] Quá tải (HTTP {}), thử lại lần {}/3 sau 10s",
                            response.statusCode(), attempt);
                    Thread.sleep(10_000);
                    continue;
                }

                if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null) {
                    log.error("🌐 [Gemini] HTTP {}: {}", response.statusCode(),
                            response.body() != null ? response.body().substring(0,
                                    Math.min(300, response.body().length())) : "(empty)");
                    return null;
                }

                JsonNode root = MAPPER.readTree(response.body());
                JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
                if (!parts.isArray() || parts.isEmpty()) {
                    log.error("🌐 [Gemini] Response không có parts: {}", response.body());
                    return null;
                }
                return parts.path(0).path("text").asText();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                // Timeout/mạng nghẽn là lỗi tạm thời -> thử lại thay vì bỏ cả job
                if (attempt < 3) {
                    log.warn("🌐 [Gemini] Lỗi tạm thời (lần {}): {} -> thử lại sau 10s",
                            attempt, e.getMessage());
                    try {
                        Thread.sleep(10_000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                    continue;
                }
                log.error("🌐 [Gemini] Lỗi sau 3 lần thử: {}", e.getMessage());
                return null;
            }
        }
        log.error("🌐 [Gemini] Không phản hồi sau 3 lần thử, bỏ qua lần gọi này");
        return null;
    }
}
