package com.paiagent.engine.graph;

import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;

/**
 * NodeAdapter —— 桥接 LangGraph4j 图节点与现有 NodeHandler 执行器。
 * 每个适配器实例对应图中的一个节点，调用时将 WorkflowState 传递给底层执行器
 * 并将结果写回 WorkflowState。
 */
public class NodeAdapter {

    private final NodeDefinition nodeDefinition;
    private final com.paiagent.engine.node.NodeHandler handler;

    public NodeAdapter(NodeDefinition nodeDefinition,
                       com.paiagent.engine.node.NodeHandler handler) {
        this.nodeDefinition = nodeDefinition;
        this.handler = handler;
    }

    public String getNodeId() {
        return nodeDefinition.getId();
    }

    public String getNodeType() {
        return nodeDefinition.getType();
    }

    /**
     * 执行节点逻辑，将结果写入 WorkflowState。
     *
     * @param state   共享状态对象（StateManager）
     * @param context 完整执行上下文（含 SSE Emitter 等）
     * @return 节点执行结果
     */
    public NodeResult execute(WorkflowState state, ExecutionContext context) {
        NodeResult result = handler.execute(nodeDefinition, context);
        state.recordResult(nodeDefinition.getId(), result);
        return result;
    }
}
