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

        ToolHandler handler = toolHandlerRegistry.getHandler(toolType);

        // Delegate full execution to the handler so it can access context directly
        return handler.executeNode(node, context);
    }
}
