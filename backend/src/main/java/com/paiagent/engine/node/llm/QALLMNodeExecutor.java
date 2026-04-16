package com.paiagent.engine.node.llm;

import com.paiagent.engine.parser.VariableResolver;
import com.paiagent.llm.factory.ChatClientFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * QALLMNodeExecutor —— QA 问答节点执行器。
 * 自动注入"你是一个知识渊博的问答助手"系统提示词。
 */
@Component
public class QALLMNodeExecutor extends AbstractLLMNodeExecutor {

    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个知识渊博的问答助手。请根据用户的问题，提供准确、详细且有逻辑的回答。" +
            "如果问题不明确，可以请求澄清。";

    public QALLMNodeExecutor(ChatClientFactory chatClientFactory,
                             VariableResolver variableResolver) {
        super(chatClientFactory, variableResolver);
    }

    @Override
    public String getType() {
        return "QA";
    }

    @Override
    protected String customizeSystemPrompt(String systemPrompt, Map<String, Object> config) {
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        return systemPrompt;
    }
}
