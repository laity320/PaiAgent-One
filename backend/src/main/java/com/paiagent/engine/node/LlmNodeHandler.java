package com.paiagent.engine.node;

import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.parser.VariableResolver;
import com.paiagent.llm.adapter.LlmAdapter;
import com.paiagent.llm.adapter.LlmAdapterFactory;
import com.paiagent.llm.dto.ChatMessage;
import com.paiagent.llm.dto.ChatRequest;
import com.paiagent.llm.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmNodeHandler extends AbstractNodeHandler {

    private final LlmAdapterFactory llmAdapterFactory;
    private final VariableResolver variableResolver;

    @Override
    public String getType() {
        return "LLM";
    }

    @Override
    protected NodeResult doExecute(NodeDefinition node, ExecutionContext context) {
        Map<String, Object> config = variableResolver.resolveConfig(node.getConfig(), context);

        String provider = (String) config.getOrDefault("provider", "openai");
        String model = (String) config.getOrDefault("model", "gpt-4o-mini");
        String systemPrompt = (String) config.getOrDefault("systemPrompt", "");
        String userPrompt = (String) config.getOrDefault("userPromptTemplate", "");
        double temperature = config.containsKey("temperature")
                ? ((Number) config.get("temperature")).doubleValue() : 0.7;
        int maxTokens = config.containsKey("maxTokens")
                ? ((Number) config.get("maxTokens")).intValue() : 2048;

        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new ChatMessage("system", systemPrompt));
        }
        messages.add(new ChatMessage("user", userPrompt));

        ChatRequest request = ChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        LlmAdapter adapter = llmAdapterFactory.getAdapter(provider);
        ChatResponse response = adapter.chat(request);

        return NodeResult.builder()
                .nodeId(node.getId())
                .nodeName(node.getLabel())
                .status("SUCCESS")
                .output(response.getContent())
                .build();
    }
}
