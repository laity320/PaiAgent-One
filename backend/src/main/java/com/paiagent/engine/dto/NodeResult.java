package com.paiagent.engine.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodeResult {
    private String nodeId;
    private String nodeName;
    private String status; // SUCCESS, FAILED
    private String output;
    private long durationMs;
}
