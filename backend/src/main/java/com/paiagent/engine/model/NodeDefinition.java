package com.paiagent.engine.model;

import lombok.Data;
import java.util.Map;

@Data
public class NodeDefinition {
    private String id;
    private String type; // INPUT, LLM, TOOL, OUTPUT
    private String label;
    private Map<String, Object> config;
    private Map<String, Double> position;
}
