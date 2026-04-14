package com.paiagent.engine.node;

import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;

public interface NodeHandler {
    String getType(); // "INPUT", "LLM", "TOOL", "OUTPUT"
    NodeResult execute(NodeDefinition node, ExecutionContext context);
}
