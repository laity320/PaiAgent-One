package com.paiagent.engine.node;

import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class InputNodeHandler extends AbstractNodeHandler {

    @Override
    public String getType() {
        return "INPUT";
    }

    @Override
    protected NodeResult doExecute(NodeDefinition node, ExecutionContext context) {
        String output = context.getFirstInput();

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("userInput", output);

        return NodeResult.builder()
                .nodeId(node.getId())
                .nodeName(node.getLabel())
                .status("SUCCESS")
                .inputs(inputs)
                .output(output)
                .outputType("text")
                .build();
    }
}
