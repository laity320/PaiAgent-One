package com.paiagent.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class WorkflowCreateRequest {
    @NotBlank(message = "工作流名称不能为空")
    private String name;

    private String description;

    @NotNull(message = "工作流图定义不能为空")
    private Map<String, Object> graphJson;
}
