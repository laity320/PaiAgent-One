package com.paiagent.engine.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.ExecutionEvent;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.graph.LangGraphWorkflowCompiler;
import com.paiagent.engine.graph.WorkflowAgentState;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.model.WorkflowGraph;
import com.paiagent.engine.node.NodeHandlerRegistry;
import com.paiagent.engine.parser.WorkflowParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * WorkflowExecutor —— 基于 LangGraph4j CompiledGraph 驱动工作流执行。
 *
 * <p>执行流程：
 * <ol>
 *   <li>解析 graphJson → WorkflowGraph</li>
 *   <li>LangGraphWorkflowCompiler 编译 → CompiledGraph（注册节点 + 边 + START/END）</li>
 *   <li>CompiledGraph.invoke() 按图结构驱动各节点执行，状态写入 ExecutionContext</li>
 *   <li>每个节点执行前后通过 SSE 推送进度事件（在 node action 内部完成）</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowExecutor {

    private final WorkflowParser workflowParser;
    private final LangGraphWorkflowCompiler graphCompiler;
    private final NodeHandlerRegistry nodeHandlerRegistry;
    private final ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // 公开入口
    // -----------------------------------------------------------------------

    public Map<String, NodeResult> execute(Map<String, Object> graphJson, String input, Long userId) {
        String execId = "exec-" + System.currentTimeMillis();
        ExecutionContext context = new ExecutionContext(execId, userId, Map.of("input", input));
        context.setObjectMapper(objectMapper);
        return doExecute(graphJson, context);
    }

    public void executeWithSSE(Map<String, Object> graphJson, String input, Long userId, SseEmitter emitter) {
        String execId = "exec-" + System.currentTimeMillis();
        ExecutionContext context = new ExecutionContext(execId, userId, Map.of("input", input));
        context.setSseEmitter(emitter);
        context.setObjectMapper(objectMapper);

        try {
            Map<String, NodeResult> results = doExecute(graphJson, context);

            // 提取最终输出（优先取 OUTPUT 节点）
            WorkflowGraph graph = context.getGraph();
            String finalOutput = "";
            String outputType = "text";

            for (NodeDefinition node : graph.getAllNodes()) {
                if ("OUTPUT".equals(node.getType())) {
                    NodeResult r = results.get(node.getId());
                    if (r != null && r.getOutput() != null) {
                        finalOutput = r.getOutput();
                        if ("audio".equals(r.getOutputType())) {
                            outputType = "audio";
                        }
                    }
                }
            }

            // Fallback：最后一个有输出的节点
            if (finalOutput.isEmpty()) {
                for (NodeResult r : results.values()) {
                    if (r.getOutput() != null) {
                        finalOutput = r.getOutput();
                        if ("audio".equals(r.getOutputType())) {
                            outputType = "audio";
                        }
                    }
                }
            }

            long totalDuration = results.values().stream().mapToLong(NodeResult::getDurationMs).sum();
            boolean aborted = results.values().stream().anyMatch(r -> "FAILED".equals(r.getStatus()));
            String status = aborted ? "FAILED" : "SUCCESS";

            sendSSEEvent(emitter, "execution_complete", ExecutionEvent.builder()
                    .eventType("execution_complete")
                    .status(status)
                    .totalDurationMs(totalDuration)
                    .finalOutput(Map.of("type", outputType, "content", finalOutput))
                    .build());

            emitter.complete();
        } catch (Exception e) {
            log.error("工作流执行异常", e);
            try {
                sendSSEEvent(emitter, "execution_complete", ExecutionEvent.builder()
                        .eventType("execution_complete")
                        .status("FAILED")
                        .error(e.getMessage())
                        .build());
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(e);
            }
        }
    }

    // -----------------------------------------------------------------------
    // 内部执行
    // -----------------------------------------------------------------------

    private Map<String, NodeResult> doExecute(Map<String, Object> graphJson,
                                               ExecutionContext context) {
        WorkflowGraph graph = workflowParser.parse(graphJson);
        context.setGraph(graph);

        for (NodeDefinition node : graph.getAllNodes()) {
            context.registerNodeLabel(node.getId(), node.getLabel());
        }

        // 使用 LangGraph4j 编译并执行
        CompiledGraph<WorkflowAgentState> compiled = graphCompiler.compile(graph, context);

        Map<String, Object> initState = new LinkedHashMap<>();
        initState.put("executionId", context.getExecutionId());
        initState.put("userInput", context.getFirstInput());
        initState.put("aborted", false);

        try {
            Optional<WorkflowAgentState> result = compiled.invoke(initState);
            log.info("LangGraph4j 执行完成, aborted={}", 
                    result.map(WorkflowAgentState::isAborted).orElse(false));
        } catch (Exception e) {
            log.error("LangGraph4j 执行异常: {}", e.getMessage(), e);
            throw new RuntimeException("工作流执行失败: " + e.getMessage(), e);
        }

        return new LinkedHashMap<>(context.getNodeResults());
    }

    // -----------------------------------------------------------------------
    // 工具方法
    // -----------------------------------------------------------------------

    private void sendSSEEvent(SseEmitter emitter, String eventName, ExecutionEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(objectMapper.writeValueAsString(event)));
        } catch (IOException e) {
            log.warn("SSE 发送失败: {}", e.getMessage());
        }
    }
}
