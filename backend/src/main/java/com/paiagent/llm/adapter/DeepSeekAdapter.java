package com.paiagent.llm.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeepSeekAdapter extends OpenAiAdapter {

    @Value("${paiagent.llm.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${paiagent.llm.deepseek.api-key:}")
    private String apiKey;

    public DeepSeekAdapter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String getProvider() {
        return "deepseek";
    }

    @Override
    protected String getBaseUrl() {
        return baseUrl;
    }

    @Override
    protected String getApiKey() {
        return apiKey;
    }
}
