package com.paiagent.engine.node.llm;

import com.paiagent.engine.parser.VariableResolver;
import com.paiagent.llm.factory.ChatClientFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SummarizeLLMNodeExecutor —— 文本摘要节点执行器。
 * 自动注入"你是一个专业的文本摘要助手"系统提示词。
 */
@Component
public class SummarizeLLMNodeExecutor extends AbstractLLMNodeExecutor {

    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个专业的文本摘要助手。请将用户提供的文本提炼成简洁、准确的摘要，保留核心要点。";

    public SummarizeLLMNodeExecutor(ChatClientFactory chatClientFactory,
                                    VariableResolver variableResolver) {
        super(chatClientFactory, variableResolver);
    }

    @Override
    public String getType() {
        return "SUMMARIZE";
    }

    @Override
    protected String customizeSystemPrompt(String systemPrompt, Map<String, Object> config) {
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        return systemPrompt;
    }
}
