package com.paiagent.engine.dto;

import lombok.Data;
import java.util.Map;

@Data
public class DebugRequest {
    private Long workflowId;
    private Map<String, Object> graphJson;
    private String input;
}
