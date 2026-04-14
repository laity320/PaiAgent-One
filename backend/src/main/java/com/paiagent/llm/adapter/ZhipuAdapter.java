package com.paiagent.llm.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ZhipuAdapter extends OpenAiAdapter {

    @Value("${paiagent.llm.zhipu.base-url:https://open.bigmodel.cn/api/paas}")
    private String baseUrl;

    @Value("${paiagent.llm.zhipu.api-key:}")
    private String apiKey;

    public ZhipuAdapter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String getProvider() {
        return "zhipu";
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
