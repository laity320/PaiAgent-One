package com.paiagent.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class WorkflowVO {
    private Long id;
    private String name;
    private String description;
    private Map<String, Object> graphJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
