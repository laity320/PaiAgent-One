package com.paiagent.workflow.dto;

import lombok.Data;

import java.util.Map;

@Data
public class WorkflowUpdateRequest {
    private String name;
    private String description;
    private Map<String, Object> graphJson;
}
