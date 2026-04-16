package com.paiagent.engine.node.llm;

import com.paiagent.engine.parser.VariableResolver;
import com.paiagent.llm.factory.ChatClientFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * TranslateLLMNodeExecutor —— 翻译节点执行器。
 * 从 config 读取目标语言，自动注入"翻译为{目标语言}"系统提示词。
 */
@Component
public class TranslateLLMNodeExecutor extends AbstractLLMNodeExecutor {

    public TranslateLLMNodeExecutor(ChatClientFactory chatClientFactory,
                                    VariableResolver variableResolver) {
        super(chatClientFactory, variableResolver);
    }

    @Override
    public String getType() {
        return "TRANSLATE";
    }

    @Override
    protected String customizeSystemPrompt(String systemPrompt, Map<String, Object> config) {
        String targetLang = (String) config.getOrDefault("targetLanguage", "中文");
        return String.format(
                "你是一个专业的翻译助手。请将用户提供的文本翻译为%s，保持原文的语气和风格。",
                targetLang);
    }
}
