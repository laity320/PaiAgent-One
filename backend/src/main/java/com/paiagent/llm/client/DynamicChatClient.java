package com.paiagent.llm.client;

import com.paiagent.llm.adapter.LlmAdapter;
import com.paiagent.llm.dto.ChatMessage;
import com.paiagent.llm.dto.ChatRequest;
import com.paiagent.llm.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * DynamicChatClient —— 面向单次调用的 OpenAI 兼容 Chat 客户端。
 *
 * <p>持有节点级配置（model / apiUrl / apiKey / temperature / maxTokens），
 * 调用 chat() 时封装消息、委托给对应厂商的 LlmAdapter 发起请求。
 */
@Slf4j
public class DynamicChatClient {

    private final LlmAdapter adapter;
    private final String model;
    private final String apiUrl;
    private final String apiKey;
    private final double temperature;
    private final int maxTokens;

    public DynamicChatClient(LlmAdapter adapter, String model,
                              String apiUrl, String apiKey,
                              double temperature, int maxTokens) {
        this.adapter = adapter;
        this.model = model;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    /**
     * 发送聊天请求。
     *
     * @param systemPrompt 系统提示词（可为 null 或空）
     * @param userPrompt   用户提示词
     * @return ChatResponse
     */
    public ChatResponse chat(String systemPrompt, String userPrompt) {
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new ChatMessage("system", systemPrompt));
        }
        messages.add(new ChatMessage("user", userPrompt));

        ChatRequest request = ChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .apiUrl(apiUrl)
                .apiKey(apiKey)
                .build();

        log.debug("DynamicChatClient 发起请求: provider={}, model={}", adapter.getProvider(), model);
        return adapter.chat(request);
    }

    public String getProvider() {
        return adapter.getProvider();
    }

    public String getModel() {
        return model;
    }
}
