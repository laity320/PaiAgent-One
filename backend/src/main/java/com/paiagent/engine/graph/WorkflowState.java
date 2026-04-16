package com.paiagent.engine.graph;

import com.paiagent.engine.dto.NodeResult;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * StateManager —— 管理 LangGraph4j 风格的图执行状态。
 * 负责在各节点间传递和持久化输入/输出数据。
 */
@Getter
public class WorkflowState {

    /** 全局执行 ID */
    private final String executionId;

    /** 用户原始输入 */
    private final String userInput;

    /** 每个节点的执行结果，key = nodeId */
    private final Map<String, NodeResult> nodeResults = new LinkedHashMap<>();

    /** 扁平化节点输出，key = nodeId 或 nodeId.paramName */
    private final Map<String, String> nodeOutputs = new LinkedHashMap<>();

    /** 节点执行是否已中止（某节点失败后置为 true） */
    private boolean aborted = false;

    /** 中止原因 */
    private String abortReason;

    public WorkflowState(String executionId, String userInput) {
        this.executionId = executionId;
        this.userInput = userInput;
    }

    /**
     * 记录节点结果，同时展开具名输出到 nodeOutputs。
     */
    public void recordResult(String nodeId, NodeResult result) {
        nodeResults.put(nodeId, result);
        if (result.getOutput() != null) {
            nodeOutputs.put(nodeId, result.getOutput());
        }
        if (result.getOutputs() != null) {
            result.getOutputs().forEach((param, val) -> {
                if (val != null) {
                    nodeOutputs.put(nodeId + "." + param, val.toString());
                }
            });
        }
    }

    /** 根据 nodeId 或 nodeId.paramName 获取输出 */
    public String getOutput(String key) {
        return nodeOutputs.get(key);
    }

    /** 获取最近一个节点的输出（用于 fallback） */
    public String getLastOutput() {
        String last = null;
        for (String v : nodeOutputs.values()) {
            last = v;
        }
        return last;
    }

    public void abort(String reason) {
        this.aborted = true;
        this.abortReason = reason;
    }
}
