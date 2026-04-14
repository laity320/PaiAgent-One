package com.paiagent.llm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {
    private String content;
    private String model;
    private int promptTokens;
    private int completionTokens;
}
