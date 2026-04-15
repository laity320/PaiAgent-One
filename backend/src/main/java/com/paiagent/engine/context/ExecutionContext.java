package com.paiagent.engine.context;

import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.WorkflowGraph;
import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
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
    private WorkflowGraph graph;

    public ExecutionContext(String executionId, Long userId, Map<String, String> inputParams) {
        this.executionId = executionId;
        this.userId = userId;
        this.inputParams = inputParams;
    }

    public void setGraph(WorkflowGraph graph) {
        this.graph = graph;
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

    /**
     * Get the output of the direct upstream node (connected via edge).
     * If multiple upstream nodes exist, returns the last one's output.
     */
    public String getDirectUpstreamOutput(String nodeId) {
        if (graph == null) return null;
        List<String> upstreamIds = graph.getUpstream(nodeId);
        if (upstreamIds == null || upstreamIds.isEmpty()) return null;
        // Return the last upstream node's output (closest in the chain)
        for (int i = upstreamIds.size() - 1; i >= 0; i--) {
            String upId = upstreamIds.get(i);
            NodeResult result = nodeResults.get(upId);
            if (result != null && result.getOutput() != null) {
                return result.getOutput();
            }
        }
        return null;
    }

    /**
     * Get the NodeResult of the direct upstream node (connected via edge).
     */
    public NodeResult getDirectUpstreamResult(String nodeId) {
        if (graph == null) return null;
        List<String> upstreamIds = graph.getUpstream(nodeId);
        if (upstreamIds == null || upstreamIds.isEmpty()) return null;
        for (int i = upstreamIds.size() - 1; i >= 0; i--) {
            String upId = upstreamIds.get(i);
            NodeResult result = nodeResults.get(upId);
            if (result != null && result.getOutput() != null) {
                return result;
            }
        }
        return null;
    }
}
