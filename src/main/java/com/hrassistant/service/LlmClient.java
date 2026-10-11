package com.hrassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Thin wrapper over an OpenAI-compatible chat API (Groq). Swap this class to use another provider. */
@Service
public class LlmClient {
    private final RestClient http = RestClient.create();
    @Value("${llm.api-key:}")   private String apiKey;
    @Value("${llm.api-url}")    private String apiUrl;
    @Value("${llm.model}")      private String model;
    @Value("${llm.max-tokens}") private int maxTokens;

    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    /** turns: list of {"role": "...", "content": "..."} maps; the first must be "user". */
    public String complete(String systemPrompt, List<Map<String, String>> turns) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(turns);

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", messages);

        JsonNode res = http.post().uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("content-type", "application/json")
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return res.path("choices").path(0).path("message").path("content").asText("");
    }
}
