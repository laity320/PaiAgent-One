package com.paiagent.workflow.service;

import com.paiagent.workflow.dto.WorkflowCreateRequest;
import com.paiagent.workflow.dto.WorkflowUpdateRequest;
import com.paiagent.workflow.dto.WorkflowVO;

import java.util.List;

public interface WorkflowService {
    Long createWorkflow(Long userId, WorkflowCreateRequest req);
    List<WorkflowVO> listWorkflows(Long userId);
    WorkflowVO getWorkflow(Long userId, Long workflowId);
    void updateWorkflow(Long userId, Long workflowId, WorkflowUpdateRequest req);
    void deleteWorkflow(Long userId, Long workflowId);
}
