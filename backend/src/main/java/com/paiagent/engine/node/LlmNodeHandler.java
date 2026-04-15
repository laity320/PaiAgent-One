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
import java.util.HashMap;
import java.util.LinkedHashMap;
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

        // Read userPrompt (frontend sends 'userPrompt', fallback to 'userPromptTemplate' for compatibility)
        String userPrompt = (String) config.getOrDefault("userPrompt",
                config.getOrDefault("userPromptTemplate", ""));

        // Read dynamic API configuration
        String apiUrl = (String) config.get("apiUrl");
        String apiKey = (String) config.get("apiKey");

        double temperature = config.containsKey("temperature")
                ? ((Number) config.get("temperature")).doubleValue() : 0.7;
        int maxTokens = config.containsKey("maxTokens")
                ? ((Number) config.get("maxTokens")).intValue() : 2048;

        // Process inputParams and build variable map for substitution
        Map<String, String> inputParamValues = processInputParams(config, context);

        // Resolve variables in userPrompt using inputParams
        String resolvedUserPrompt = variableResolver.resolveWithParams(userPrompt, context, inputParamValues);

        // Get output parameter configuration
        String outputParamName = "output";
        String outputParamDesc = "";
        if (config.get("outputParam") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> outputParam = (Map<String, Object>) config.get("outputParam");
            outputParamName = (String) outputParam.getOrDefault("name", "output");
            outputParamDesc = (String) outputParam.getOrDefault("description", "");
        }

        // Build chat messages
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new ChatMessage("system", systemPrompt));
        }
        messages.add(new ChatMessage("user", resolvedUserPrompt));

        // Build request with optional dynamic API config
        ChatRequest.ChatRequestBuilder requestBuilder = ChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens);

        // Set dynamic API URL and API key if provided
        if (apiUrl != null && !apiUrl.isEmpty()) {
            requestBuilder.apiUrl(apiUrl);
        }
        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.apiKey(apiKey);
        }

        ChatRequest request = requestBuilder.build();

        LlmAdapter adapter = llmAdapterFactory.getAdapter(provider);
        ChatResponse response = adapter.chat(request);

        // Build output with the configured parameter name
        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put(outputParamName, response.getContent());

        // Record inputs for display
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("model", model);
        inputs.put("provider", provider);
        for (Map.Entry<String, String> entry : inputParamValues.entrySet()) {
            String value = entry.getValue();
            // Truncate long values
            if (value != null && value.length() > 200) {
                value = value.substring(0, 200) + "...";
            }
            inputs.put(entry.getKey(), value);
        }
        if (resolvedUserPrompt != null && !resolvedUserPrompt.isEmpty()) {
            String promptPreview = resolvedUserPrompt.length() > 300
                    ? resolvedUserPrompt.substring(0, 300) + "..."
                    : resolvedUserPrompt;
            inputs.put("prompt", promptPreview);
        }

        return NodeResult.builder()
                .nodeId(node.getId())
                .nodeName(node.getLabel())
                .status("SUCCESS")
                .inputs(inputs)
                .output(response.getContent())
                .outputType("text")
                .outputs(outputs)
                .build();
    }

    /**
     * Process inputParams array and build a map of parameter name to resolved value.
     * InputParams format: [{ name: string, type: 'input' | 'ref', value: string }]
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> processInputParams(Map<String, Object> config, ExecutionContext context) {
        Map<String, String> result = new HashMap<>();

        Object inputParamsObj = config.get("inputParams");
        if (inputParamsObj instanceof List) {
            List<?> inputParams = (List<?>) inputParamsObj;
            for (Object paramObj : inputParams) {
                if (paramObj instanceof Map) {
                    Map<String, Object> param = (Map<String, Object>) paramObj;
                    String name = (String) param.get("name");
                    String type = (String) param.getOrDefault("type", "input");
                    Object valueObj = param.get("value");
                    String value = valueObj != null ? valueObj.toString() : "";

                    if (name == null || name.isEmpty()) {
                        continue;
                    }

                    // If type is 'ref', resolve the value from context
                    if ("ref".equals(type) && value != null && !value.isEmpty()) {
                        // Value should be like "nodeName.output" or just "nodeName"
                        String resolvedValue = resolveRefValue(value, context);
                        result.put(name, resolvedValue != null ? resolvedValue : value);
                    } else {
                        // type is 'input', use the value directly
                        result.put(name, value != null ? value : "");
                    }
                }
            }
        }

        return result;
    }

    /**
     * Resolve a reference value from the execution context.
     * Supports formats: "nodeName.output", "nodeId.output", or just "nodeName"
     */
    private String resolveRefValue(String ref, ExecutionContext context) {
        if (ref == null || ref.isEmpty()) {
            return null;
        }

        // Check if it's in node.output format
        if (ref.contains(".output")) {
            String nodeName = ref.replace(".output", "");
            String value = context.getNodeOutput(nodeName);
            if (value == null) {
                value = context.getNodeOutputByLabel(nodeName);
            }
            return value;
        }

        // Try to resolve as nodeId or nodeName directly
        String value = context.getNodeOutput(ref);
        if (value == null) {
            value = context.getNodeOutputByLabel(ref);
        }
        return value;
    }
}
