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

    /**
     * 从 Spring AI ChatResponse 转换为内部 ChatResponse。
     */
    public static ChatResponse fromSpringAi(org.springframework.ai.chat.model.ChatResponse aiResponse) {
        if (aiResponse == null || aiResponse.getResult() == null) {
            return ChatResponse.builder()
                    .content("")
                    .model("")
                    .promptTokens(0)
                    .completionTokens(0)
                    .build();
        }

        String content = "";
        if (aiResponse.getResult().getOutput() != null) {
            content = aiResponse.getResult().getOutput().getText();
            if (content == null) content = "";
        }

        String model = "";
        long promptTokens = 0;
        long completionTokens = 0;

        if (aiResponse.getMetadata() != null) {
            if (aiResponse.getMetadata().getModel() != null) {
                model = aiResponse.getMetadata().getModel();
            }
            if (aiResponse.getMetadata().getUsage() != null) {
                promptTokens = aiResponse.getMetadata().getUsage().getPromptTokens();
                completionTokens = aiResponse.getMetadata().getUsage().getCompletionTokens();
            }
        }

        return ChatResponse.builder()
                .content(content)
                .model(model)
                .promptTokens((int) promptTokens)
                .completionTokens((int) completionTokens)
                .build();
    }
}
