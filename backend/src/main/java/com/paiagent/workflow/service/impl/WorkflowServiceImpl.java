package com.paiagent.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paiagent.common.exception.BizException;
import com.paiagent.workflow.dto.WorkflowCreateRequest;
import com.paiagent.workflow.dto.WorkflowUpdateRequest;
import com.paiagent.workflow.dto.WorkflowVO;
import com.paiagent.workflow.entity.Workflow;
import com.paiagent.workflow.mapper.WorkflowMapper;
import com.paiagent.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowMapper workflowMapper;

    @Override
    public Long createWorkflow(Long userId, WorkflowCreateRequest req) {
        Workflow wf = new Workflow();
        wf.setUserId(userId);
        wf.setName(req.getName());
        wf.setDescription(req.getDescription());
        wf.setGraphJson(req.getGraphJson());
        wf.setStatus(1);
        workflowMapper.insert(wf);
        return wf.getId();
    }

    @Override
    public List<WorkflowVO> listWorkflows(Long userId) {
        List<Workflow> list = workflowMapper.selectList(
                new LambdaQueryWrapper<Workflow>()
                        .eq(Workflow::getUserId, userId)
                        .eq(Workflow::getStatus, 1)
                        .orderByDesc(Workflow::getUpdatedAt));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public WorkflowVO getWorkflow(Long userId, Long workflowId) {
        Workflow wf = workflowMapper.selectById(workflowId);
        if (wf == null || !wf.getUserId().equals(userId)) {
            throw new BizException(404, "工作流不存在");
        }
        return toVO(wf);
    }

    @Override
    public void updateWorkflow(Long userId, Long workflowId, WorkflowUpdateRequest req) {
        Workflow wf = workflowMapper.selectById(workflowId);
        if (wf == null || !wf.getUserId().equals(userId)) {
            throw new BizException(404, "工作流不存在");
        }
        if (req.getName() != null) wf.setName(req.getName());
        if (req.getDescription() != null) wf.setDescription(req.getDescription());
        if (req.getGraphJson() != null) wf.setGraphJson(req.getGraphJson());
        workflowMapper.updateById(wf);
    }

    @Override
    public void deleteWorkflow(Long userId, Long workflowId) {
        Workflow wf = workflowMapper.selectById(workflowId);
        if (wf == null || !wf.getUserId().equals(userId)) {
            throw new BizException(404, "工作流不存在");
        }
        wf.setStatus(0);
        workflowMapper.updateById(wf);
    }

    private WorkflowVO toVO(Workflow wf) {
        WorkflowVO vo = new WorkflowVO();
        vo.setId(wf.getId());
        vo.setName(wf.getName());
        vo.setDescription(wf.getDescription());
        vo.setGraphJson(wf.getGraphJson());
        vo.setCreatedAt(wf.getCreatedAt());
        vo.setUpdatedAt(wf.getUpdatedAt());
        return vo;
    }
}
