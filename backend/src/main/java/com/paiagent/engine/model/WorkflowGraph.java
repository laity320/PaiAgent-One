package com.paiagent.engine.model;

import lombok.Data;
import java.util.*;

@Data
public class WorkflowGraph {
    private Map<String, NodeDefinition> nodeMap = new LinkedHashMap<>();
    private List<EdgeDefinition> edges = new ArrayList<>();

    // adjacency list: nodeId -> list of downstream nodeIds
    private Map<String, List<String>> adjacency = new HashMap<>();
    // reverse adjacency: nodeId -> list of upstream nodeIds
    private Map<String, List<String>> reverseAdjacency = new HashMap<>();

    public void addNode(NodeDefinition node) {
        nodeMap.put(node.getId(), node);
        adjacency.putIfAbsent(node.getId(), new ArrayList<>());
        reverseAdjacency.putIfAbsent(node.getId(), new ArrayList<>());
    }

    public void addEdge(EdgeDefinition edge) {
        edges.add(edge);
        adjacency.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge.getTarget());
        reverseAdjacency.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>()).add(edge.getSource());
    }

    public NodeDefinition getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    public Collection<NodeDefinition> getAllNodes() {
        return nodeMap.values();
    }

    public List<String> getDownstream(String nodeId) {
        return adjacency.getOrDefault(nodeId, Collections.emptyList());
    }

    public List<String> getUpstream(String nodeId) {
        return reverseAdjacency.getOrDefault(nodeId, Collections.emptyList());
    }
}
