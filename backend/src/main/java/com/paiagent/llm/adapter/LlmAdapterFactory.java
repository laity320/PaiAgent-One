package com.paiagent.llm.adapter;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LlmAdapterFactory {

    private final Map<String, LlmAdapter> adapters = new HashMap<>();

    public LlmAdapterFactory(List<LlmAdapter> adapterList) {
        for (LlmAdapter adapter : adapterList) {
            adapters.put(adapter.getProvider(), adapter);
        }
    }

    public LlmAdapter getAdapter(String provider) {
        LlmAdapter adapter = adapters.get(provider.toLowerCase());
        if (adapter == null) {
            // Fallback to OpenAI adapter
            adapter = adapters.get("openai");
        }
        if (adapter == null) {
            throw new IllegalArgumentException("未找到大模型适配器: " + provider);
        }
        return adapter;
    }
}
