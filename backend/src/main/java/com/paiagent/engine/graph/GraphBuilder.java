package com.paiagent.engine.graph;

import com.paiagent.engine.executor.TopologicalSorter;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.model.WorkflowGraph;
import com.paiagent.engine.node.NodeHandler;
import com.paiagent.engine.node.NodeHandlerRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GraphBuilder —— 负责节点注册和边连接，构建可执行的 StateGraph。
 *
 * <p>用法示例：
 * <pre>
 *   StateGraph graph = GraphBuilder.from(workflowGraph, registry, sorter)
 *                                   .build();
 *   graph.compile().execute(state, context);
 * </pre>
 */
@Slf4j
public class GraphBuilder {

    private final WorkflowGraph workflowGraph;
    private final NodeHandlerRegistry handlerRegistry;
    private final TopologicalSorter topologicalSorter;

    /** nodeId -> NodeAdapter */
    private final Map<String, NodeAdapter> adapters = new LinkedHashMap<>();

    /** 拓扑排序后的执行顺序 */
    private List<NodeAdapter> executionOrder = new ArrayList<>();

    private GraphBuilder(WorkflowGraph workflowGraph,
                         NodeHandlerRegistry handlerRegistry,
                         TopologicalSorter topologicalSorter) {
        this.workflowGraph = workflowGraph;
        this.handlerRegistry = handlerRegistry;
        this.topologicalSorter = topologicalSorter;
    }

    public static GraphBuilder from(WorkflowGraph workflowGraph,
                                    NodeHandlerRegistry handlerRegistry,
                                    TopologicalSorter topologicalSorter) {
        return new GraphBuilder(workflowGraph, handlerRegistry, topologicalSorter);
    }

    /**
     * 注册所有节点并按拓扑顺序排列，返回已编译的 StateGraph。
     */
    public StateGraph build() {
        // 1. 注册节点
        for (NodeDefinition node : workflowGraph.getAllNodes()) {
            NodeHandler handler = handlerRegistry.getHandler(node.getType());
            NodeAdapter adapter = new NodeAdapter(node, handler);
            adapters.put(node.getId(), adapter);
            log.debug("GraphBuilder 注册节点: id={}, type={}, label={}", node.getId(), node.getType(), node.getLabel());
        }

        // 2. 拓扑排序确定执行顺序
        List<NodeDefinition> sorted = topologicalSorter.sort(workflowGraph);
        for (NodeDefinition node : sorted) {
            NodeAdapter adapter = adapters.get(node.getId());
            if (adapter != null) {
                executionOrder.add(adapter);
            }
        }

        log.info("GraphBuilder 构建完成: {} 个节点, 执行顺序: {}",
                adapters.size(),
                executionOrder.stream().map(NodeAdapter::getNodeId).toList());

        return new StateGraph(workflowGraph, executionOrder);
    }
}
