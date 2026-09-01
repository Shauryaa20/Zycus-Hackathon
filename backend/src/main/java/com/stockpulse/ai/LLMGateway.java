package com.stockpulse.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class LLMGateway {

    @Value("${llm.provider}")
    private String provider;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model}")
    private String model;

    @Value("${llm.base-url}")
    private String baseUrl;

    private final WebClient webClient;

    public LLMGateway(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String callLLM(String prompt) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("dummy_key_for_testing")) {
            throw new RuntimeException("API key not configured");
        }

        switch (provider.toLowerCase()) {
            case "gemini":
                return callGemini(prompt);
            case "groq":
                return callOpenAICompatible(prompt, baseUrl + "/openai/v1/chat/completions");
            case "ollama":
                return callOpenAICompatible(prompt, baseUrl + "/v1/chat/completions");
            default:
                throw new IllegalStateException("Unknown provider: " + provider);
        }
    }

    private String callGemini(String prompt) {
        String url = baseUrl + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            ),
            "generationConfig", Map.of(
                "responseMimeType", "application/json"
            )
        );

        try {
            Map response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
            return "{}";
        } catch (Exception e) {
            throw new RuntimeException("Error calling Gemini API: " + e.getMessage(), e);
        }
    }

    private String callOpenAICompatible(String prompt, String url) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object")
        );

        try {
            Map response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
            return "{}";
        } catch (Exception e) {
             throw new RuntimeException("Error calling OpenAI-compatible API: " + e.getMessage(), e);
        }
    }
}
