package com.paiagent.engine.node;

import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.parser.VariableResolver;
import com.paiagent.tool.handler.ToolHandler;
import com.paiagent.tool.handler.ToolHandlerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ToolNodeHandler extends AbstractNodeHandler {

    private final ToolHandlerRegistry toolHandlerRegistry;
    private final VariableResolver variableResolver;

    @Override
    public String getType() {
        return "TOOL";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NodeResult doExecute(NodeDefinition node, ExecutionContext context) {
        Map<String, Object> config = variableResolver.resolveConfig(node.getConfig(), context);

        String toolType = (String) config.getOrDefault("toolType", "");
        Map<String, Object> toolConfig = (Map<String, Object>) config.getOrDefault("toolConfig", Map.of());

        // Get the upstream node's output as input text for the tool
        String inputText = "";
        // Find the input from upstream nodes
        for (String upstreamId : context.getNodeResults().keySet()) {
            var result = context.getNodeResults().get(upstreamId);
            if (result != null && result.getOutput() != null) {
                inputText = result.getOutput(); // Use the latest upstream output
            }
        }

        ToolHandler handler = toolHandlerRegistry.getHandler(toolType);
        String output = handler.execute(inputText, toolConfig);

        return NodeResult.builder()
                .nodeId(node.getId())
                .nodeName(node.getLabel())
                .status("SUCCESS")
                .output(output)
                .build();
    }
}
