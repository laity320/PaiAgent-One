package com.paiagent.engine.executor;

import com.paiagent.common.exception.WorkflowExecutionException;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.model.WorkflowGraph;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TopologicalSorter {

    public List<NodeDefinition> sort(WorkflowGraph graph) {
        Map<String, Integer> inDegree = new HashMap<>();
        for (NodeDefinition node : graph.getAllNodes()) {
            inDegree.put(node.getId(), 0);
        }

        for (var edge : graph.getEdges()) {
            inDegree.merge(edge.getTarget(), 1, Integer::sum);
        }

        Queue<String> queue = new LinkedList<>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<NodeDefinition> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            NodeDefinition node = graph.getNode(nodeId);
            if (node != null) {
                sorted.add(node);
            }

            for (String downstream : graph.getDownstream(nodeId)) {
                int newDegree = inDegree.get(downstream) - 1;
                inDegree.put(downstream, newDegree);
                if (newDegree == 0) {
                    queue.offer(downstream);
                }
            }
        }

        if (sorted.size() != graph.getAllNodes().size()) {
            throw new WorkflowExecutionException("工作流中存在循环依赖");
        }

        return sorted;
    }
}
