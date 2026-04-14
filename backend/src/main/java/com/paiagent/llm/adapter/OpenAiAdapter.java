package com.paiagent.llm.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiagent.llm.dto.ChatMessage;
import com.paiagent.llm.dto.ChatRequest;
import com.paiagent.llm.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OpenAiAdapter implements LlmAdapter {

    @Value("${paiagent.llm.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${paiagent.llm.openai.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiAdapter(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    // Allow subclasses to override
    protected String getBaseUrl() { return baseUrl; }
    protected String getApiKey() { return apiKey; }

    @Override
    public String getProvider() {
        return "openai";
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String url = getBaseUrl() + "/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        body.put("messages", request.getMessages().stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList()));
        body.put("temperature", request.getTemperature());
        body.put("max_tokens", request.getMaxTokens());

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);

            return ChatResponse.builder()
                    .content(content)
                    .model(request.getModel())
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .build();
        } catch (Exception e) {
            log.error("OpenAI API 调用失败: {}", e.getMessage(), e);
            // Return mock response for development
            return ChatResponse.builder()
                    .content("[Mock] 这是一段 AI 生成的模拟回复。您的输入已收到，当前未配置有效的 API Key。请在大模型配置中设置正确的 API Key。原始输入: "
                            + (request.getMessages().isEmpty() ? ""
                            : request.getMessages().get(request.getMessages().size() - 1).getContent()))
                    .model(request.getModel())
                    .promptTokens(0)
                    .completionTokens(0)
                    .build();
        }
    }
}
