package com.paiagent.engine.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class NodeResult {
    private String nodeId;
    private String nodeName;
    private String status; // SUCCESS, FAILED
    private String output;
    private Map<String, Object> outputs; // Named outputs for multiple parameters
    private long durationMs;
}
