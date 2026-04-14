package com.paiagent.engine.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutionEvent {
    private String eventType; // node_start, node_complete, node_error, execution_complete
    private String nodeId;
    private String nodeType;
    private String label;
    private String output;
    private String error;
    private Long durationMs;
    private String status;
    private Long totalDurationMs;
    private Object finalOutput;
}
