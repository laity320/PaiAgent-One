package com.paiagent.llm.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private double temperature;
    private int maxTokens;
}
