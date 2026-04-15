package com.paiagent.engine.context;

import com.paiagent.engine.dto.NodeResult;
import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class ExecutionContext {

    private final String executionId;
    private final Long userId;
    private final Map<String, String> inputParams;
    private final Map<String, NodeResult> nodeResults = new LinkedHashMap<>();
    private final Map<String, String> nodeOutputs = new LinkedHashMap<>();
    // label -> nodeId mapping for variable resolution by label
    private final Map<String, String> labelToNodeId = new LinkedHashMap<>();
    private SseEmitter sseEmitter;

    public ExecutionContext(String executionId, Long userId, Map<String, String> inputParams) {
        this.executionId = executionId;
        this.userId = userId;
        this.inputParams = inputParams;
    }

    public void setSseEmitter(SseEmitter emitter) {
        this.sseEmitter = emitter;
    }

    public void registerNodeLabel(String nodeId, String label) {
        if (label != null) {
            labelToNodeId.put(label, nodeId);
        }
    }

    public void putNodeResult(String nodeId, NodeResult result) {
        nodeResults.put(nodeId, result);
        if (result.getOutput() != null) {
            nodeOutputs.put(nodeId, result.getOutput());
        }
        // Also register named outputs (e.g. voice_url, output) as nodeId__paramName
        if (result.getOutputs() != null) {
            result.getOutputs().forEach((paramName, value) -> {
                if (value != null) {
                    nodeOutputs.put(nodeId + "." + paramName, value.toString());
                }
            });
        }
    }

    public String getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }

    public String getNodeOutputByLabel(String label) {
        String nodeId = labelToNodeId.get(label);
        if (nodeId != null) {
            return nodeOutputs.get(nodeId);
        }
        return null;
    }

    public String getInput(String key) {
        return inputParams.get(key);
    }

    public String getFirstInput() {
        return inputParams.values().stream().findFirst().orElse("");
    }
}
