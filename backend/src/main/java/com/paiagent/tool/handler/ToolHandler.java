package com.paiagent.tool.handler;

import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;

import java.util.Map;

public interface ToolHandler {
    String getToolType();
    String execute(String input, Map<String, Object> config);

    /**
     * Execute tool with full node configuration
     * @param node The node definition
     * @param context The execution context
     * @return NodeResult with output and outputs map
     */
    default NodeResult executeNode(NodeDefinition node, ExecutionContext context) {
        Map<String, Object> config = node.getConfig();
        String input = "";
        // Get input from upstream nodes
        for (String upstreamId : context.getNodeResults().keySet()) {
            var result = context.getNodeResults().get(upstreamId);
            if (result != null && result.getOutput() != null) {
                input = result.getOutput();
            }
        }
        String output = execute(input, config);
        return NodeResult.builder()
                .nodeId(node.getId())
                .nodeName(node.getLabel())
                .status("SUCCESS")
                .output(output)
                .build();
    }
}
