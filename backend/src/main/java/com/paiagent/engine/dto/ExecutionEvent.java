package com.paiagent.engine.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ExecutionEvent {
    private String eventType; // node_start, node_progress, node_complete, node_error, execution_complete
    private String nodeId;
    private String nodeType;
    private String label;
    private Map<String, Object> inputs;  // 输入参数
    private String output;
    private String outputType; // text | audio
    private String error;
    private Long durationMs;
    private String status;
    private Long totalDurationMs;
    private Object finalOutput;

    // Progress fields for streaming operations
    private Integer progress;      // 0-100
    private String progressText;   // e.g., "分片 3/10"
}
