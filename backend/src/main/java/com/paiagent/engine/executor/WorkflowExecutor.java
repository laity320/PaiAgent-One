package com.paiagent.engine.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.ExecutionEvent;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.graph.GraphBuilder;
import com.paiagent.engine.graph.NodeAdapter;
import com.paiagent.engine.graph.StateGraph;
import com.paiagent.engine.graph.WorkflowState;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.model.WorkflowGraph;
import com.paiagent.engine.node.NodeHandlerRegistry;
import com.paiagent.engine.parser.WorkflowParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WorkflowExecutor —— 基于 LangGraph4j 风格的 StateGraph 驱动工作流执行。
 *
 * <p>执行流程：
 * <ol>
 *   <li>解析 graphJson → WorkflowGraph</li>
 *   <li>GraphBuilder 注册节点 + 拓扑排序 → StateGraph（编译阶段）</li>
 *   <li>StateGraph#execute() 按顺序驱动各 NodeAdapter，状态写入 WorkflowState</li>
 *   <li>每个节点执行前后通过 SSE 推送进度事件</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowExecutor {

    private final WorkflowParser workflowParser;
    private final TopologicalSorter topologicalSorter;
    private final NodeHandlerRegistry nodeHandlerRegistry;
    private final ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // 公开入口
    // -----------------------------------------------------------------------

    public Map<String, NodeResult> execute(Map<String, Object> graphJson, String input, Long userId) {
        String execId = "exec-" + System.currentTimeMillis();
        ExecutionContext context = new ExecutionContext(execId, userId, Map.of("input", input));
        WorkflowState state = new WorkflowState(execId, input);
        return doExecute(graphJson, context, state);
    }

    public void executeWithSSE(Map<String, Object> graphJson, String input, Long userId, SseEmitter emitter) {
        String execId = "exec-" + System.currentTimeMillis();
        ExecutionContext context = new ExecutionContext(execId, userId, Map.of("input", input));
        context.setSseEmitter(emitter);
        context.setObjectMapper(objectMapper);

        WorkflowState state = new WorkflowState(execId, input);

        try {
            Map<String, NodeResult> results = doExecuteWithSSE(graphJson, context, state, emitter);

            // 优先取 OUTPUT 节点结果
            WorkflowGraph graph = workflowParser.parse(graphJson);
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
            String status = state.isAborted() ? "FAILED" : "SUCCESS";

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
    // 内部执行（无 SSE）
    // -----------------------------------------------------------------------

    private Map<String, NodeResult> doExecute(Map<String, Object> graphJson,
                                               ExecutionContext context,
                                               WorkflowState state) {
        WorkflowGraph graph = workflowParser.parse(graphJson);
        context.setGraph(graph);

        for (NodeDefinition node : graph.getAllNodes()) {
            context.registerNodeLabel(node.getId(), node.getLabel());
        }

        // 构建 StateGraph（GraphBuilder 注册节点 + 拓扑排序）
        StateGraph stateGraph = GraphBuilder.from(graph, nodeHandlerRegistry, topologicalSorter).build();
        stateGraph.execute(state, context);

        return new LinkedHashMap<>(state.getNodeResults());
    }

    // -----------------------------------------------------------------------
    // 内部执行（有 SSE）：手动遍历以在每节点前后推送事件
    // -----------------------------------------------------------------------

    private Map<String, NodeResult> doExecuteWithSSE(Map<String, Object> graphJson,
                                                      ExecutionContext context,
                                                      WorkflowState state,
                                                      SseEmitter emitter) {
        WorkflowGraph graph = workflowParser.parse(graphJson);
        context.setGraph(graph);

        for (NodeDefinition node : graph.getAllNodes()) {
            context.registerNodeLabel(node.getId(), node.getLabel());
        }

        // 编译 StateGraph
        StateGraph stateGraph = GraphBuilder.from(graph, nodeHandlerRegistry, topologicalSorter).build();

        for (NodeAdapter adapter : stateGraph.getExecutionOrder()) {
            if (state.isAborted()) break;

            NodeDefinition node = graph.getNode(adapter.getNodeId());

            // node_start
            sendSSEEvent(emitter, "node_start", ExecutionEvent.builder()
                    .eventType("node_start")
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .label(node.getLabel())
                    .build());

            // 执行：NodeAdapter 内部调用 handler 并写入 state
            NodeResult result = adapter.execute(state, context);
            context.putNodeResult(node.getId(), result);

            // node_complete / node_error
            if ("SUCCESS".equals(result.getStatus())) {
                sendSSEEvent(emitter, "node_complete", ExecutionEvent.builder()
                        .eventType("node_complete")
                        .nodeId(node.getId())
                        .nodeType(node.getType())
                        .label(node.getLabel())
                        .inputs(result.getInputs())
                        .output(result.getOutput())
                        .outputType(result.getOutputType())
                        .durationMs(result.getDurationMs())
                        .build());
            } else {
                sendSSEEvent(emitter, "node_error", ExecutionEvent.builder()
                        .eventType("node_error")
                        .nodeId(node.getId())
                        .label(node.getLabel())
                        .error(result.getOutput())
                        .build());
                state.abort("节点 " + node.getId() + " 执行失败");
                break;
            }
        }

        return new LinkedHashMap<>(state.getNodeResults());
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
