package com.paiagent.workflow.controller;

import com.paiagent.common.result.R;
import com.paiagent.common.util.SecurityUtil;
import com.paiagent.workflow.dto.WorkflowCreateRequest;
import com.paiagent.workflow.dto.WorkflowUpdateRequest;
import com.paiagent.workflow.dto.WorkflowVO;
import com.paiagent.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    public R<Long> create(@RequestBody @Valid WorkflowCreateRequest req) {
        Long id = workflowService.createWorkflow(SecurityUtil.getCurrentUserId(), req);
        return R.ok(id);
    }

    @GetMapping
    public R<List<WorkflowVO>> list() {
        return R.ok(workflowService.listWorkflows(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public R<WorkflowVO> get(@PathVariable Long id) {
        return R.ok(workflowService.getWorkflow(SecurityUtil.getCurrentUserId(), id));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody WorkflowUpdateRequest req) {
        workflowService.updateWorkflow(SecurityUtil.getCurrentUserId(), id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        workflowService.deleteWorkflow(SecurityUtil.getCurrentUserId(), id);
        return R.ok();
    }
}
