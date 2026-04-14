package com.paiagent.llm.adapter;

import com.paiagent.llm.dto.ChatRequest;
import com.paiagent.llm.dto.ChatResponse;

public interface LlmAdapter {
    String getProvider();
    ChatResponse chat(ChatRequest request);
}
