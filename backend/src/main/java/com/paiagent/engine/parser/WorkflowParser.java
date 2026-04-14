package com.paiagent.engine.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiagent.engine.model.EdgeDefinition;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.model.WorkflowGraph;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WorkflowParser {

    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public WorkflowGraph parse(Map<String, Object> graphJson) {
        WorkflowGraph graph = new WorkflowGraph();

        List<Map<String, Object>> nodesList = (List<Map<String, Object>>) graphJson.get("nodes");
        List<Map<String, Object>> edgesList = (List<Map<String, Object>>) graphJson.get("edges");

        if (nodesList != null) {
            for (Map<String, Object> nodeMap : nodesList) {
                NodeDefinition node = new NodeDefinition();
                node.setId((String) nodeMap.get("id"));
                // React Flow stores node type in "type" field
                String type = (String) nodeMap.get("type");
                node.setType(type != null ? type.toUpperCase() : "UNKNOWN");

                Map<String, Object> data = (Map<String, Object>) nodeMap.get("data");
                if (data != null) {
                    node.setLabel((String) data.get("label"));
                    Map<String, Object> config = (Map<String, Object>) data.get("config");
                    node.setConfig(config != null ? config : Map.of());
                }

                Map<String, Object> position = (Map<String, Object>) nodeMap.get("position");
                if (position != null) {
                    node.setPosition(Map.of(
                            "x", ((Number) position.get("x")).doubleValue(),
                            "y", ((Number) position.get("y")).doubleValue()));
                }

                graph.addNode(node);
            }
        }

        if (edgesList != null) {
            for (Map<String, Object> edgeMap : edgesList) {
                EdgeDefinition edge = new EdgeDefinition();
                edge.setId((String) edgeMap.get("id"));
                edge.setSource((String) edgeMap.get("source"));
                edge.setTarget((String) edgeMap.get("target"));
                edge.setSourceHandle((String) edgeMap.get("sourceHandle"));
                edge.setTargetHandle((String) edgeMap.get("targetHandle"));
                graph.addEdge(edge);
            }
        }

        return graph;
    }
}
