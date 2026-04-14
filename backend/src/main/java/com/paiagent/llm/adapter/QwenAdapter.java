package com.paiagent.llm.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QwenAdapter extends OpenAiAdapter {

    @Value("${paiagent.llm.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String baseUrl;

    @Value("${paiagent.llm.qwen.api-key:}")
    private String apiKey;

    public QwenAdapter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String getProvider() {
        return "tongyi";
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
