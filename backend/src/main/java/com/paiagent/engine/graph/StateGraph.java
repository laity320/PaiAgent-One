package com.paiagent.engine.graph;

import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.WorkflowGraph;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * StateGraph —— LangGraph4j 风格的状态图，持有编译后的执行计划。
 * 由 GraphBuilder#build() 创建，对外提供 execute() 入口。
 */
@Slf4j
public class StateGraph {

    private final WorkflowGraph workflowGraph;
    private final List<NodeAdapter> executionOrder;

    StateGraph(WorkflowGraph workflowGraph, List<NodeAdapter> executionOrder) {
        this.workflowGraph = workflowGraph;
        this.executionOrder = executionOrder;
    }

    /**
     * 按拓扑顺序依次执行各节点，失败时中止。
     *
     * @param state   WorkflowState（StateManager）
     * @param context ExecutionContext（含 SSE 等运行时依赖）
     */
    public void execute(WorkflowState state, ExecutionContext context) {
        for (NodeAdapter adapter : executionOrder) {
            if (state.isAborted()) {
                log.warn("StateGraph 已中止，跳过节点: {}", adapter.getNodeId());
                break;
            }

            log.info("StateGraph 执行节点: id={}, type={}", adapter.getNodeId(), adapter.getNodeType());
            NodeResult result = adapter.execute(state, context);
            context.putNodeResult(adapter.getNodeId(), result);

            if ("FAILED".equals(result.getStatus())) {
                state.abort("节点 " + adapter.getNodeId() + " 执行失败: " + result.getOutput());
                log.warn("StateGraph 中止: {}", state.getAbortReason());
                break;
            }
        }
    }

    public WorkflowGraph getWorkflowGraph() {
        return workflowGraph;
    }

    public List<NodeAdapter> getExecutionOrder() {
        return executionOrder;
    }
}
