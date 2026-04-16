package com.paiagent.engine.node.llm;

import com.paiagent.engine.parser.VariableResolver;
import com.paiagent.llm.factory.ChatClientFactory;
import org.springframework.stereotype.Component;

/**
 * DefaultLLMNodeExecutor —— 默认 LLM 节点执行器。
 * 直接透传用户配置的 systemPrompt 与 userPrompt，无额外处理。
 */
@Component
public class DefaultLLMNodeExecutor extends AbstractLLMNodeExecutor {

    public DefaultLLMNodeExecutor(ChatClientFactory chatClientFactory,
                                  VariableResolver variableResolver) {
        super(chatClientFactory, variableResolver);
    }

    @Override
    public String getType() {
        return "LLM";
    }
}
