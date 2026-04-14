package com.paiagent.engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiagent.common.result.R;
import com.paiagent.common.util.SecurityUtil;
import com.paiagent.engine.dto.DebugRequest;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.executor.WorkflowExecutor;
import com.paiagent.workflow.entity.Workflow;
import com.paiagent.workflow.mapper.WorkflowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/execution")
@RequiredArgsConstructor
public class ExecutionController {

    private final WorkflowExecutor workflowExecutor;
    private final WorkflowMapper workflowMapper;
    private final ObjectMapper objectMapper;

    @PostMapping("/debug")
    public R<Map<String, NodeResult>> debug(@RequestBody DebugRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();

        Map<String, Object> graphJson = request.getGraphJson();
        if (graphJson == null && request.getWorkflowId() != null) {
            Workflow wf = workflowMapper.selectById(request.getWorkflowId());
            if (wf != null) {
                graphJson = wf.getGraphJson();
            }
        }

        if (graphJson == null) {
            return R.fail("工作流定义不能为空");
        }

        Map<String, NodeResult> results = workflowExecutor.execute(graphJson, request.getInput(), userId);
        return R.ok(results);
    }

    @PostMapping(value = "/debug/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter debugStream(@RequestBody DebugRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        SseEmitter emitter = new SseEmitter(300000L); // 5 min timeout

        Map<String, Object> graphJson = request.getGraphJson();
        if (graphJson == null && request.getWorkflowId() != null) {
            Workflow wf = workflowMapper.selectById(request.getWorkflowId());
            if (wf != null) {
                graphJson = wf.getGraphJson();
            }
        }

        if (graphJson == null) {
            emitter.completeWithError(new RuntimeException("工作流定义不能为空"));
            return emitter;
        }

        final Map<String, Object> finalGraphJson = graphJson;

        // Execute in a separate thread to not block
        new Thread(() -> {
            workflowExecutor.executeWithSSE(finalGraphJson, request.getInput(), userId, emitter);
        }).start();

        return emitter;
    }
}
