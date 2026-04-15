package com.paiagent.engine.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.ExecutionEvent;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.model.WorkflowGraph;
import com.paiagent.engine.node.NodeHandler;
import com.paiagent.engine.node.NodeHandlerRegistry;
import com.paiagent.engine.parser.WorkflowParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowExecutor {

    private final WorkflowParser workflowParser;
    private final TopologicalSorter topologicalSorter;
    private final NodeHandlerRegistry nodeHandlerRegistry;
    private final ObjectMapper objectMapper;

    public Map<String, NodeResult> execute(Map<String, Object> graphJson, String input, Long userId) {
        ExecutionContext context = new ExecutionContext(
                "exec-" + System.currentTimeMillis(),
                userId,
                Map.of("input", input)
        );
        return doExecute(graphJson, context);
    }

    public void executeWithSSE(Map<String, Object> graphJson, String input, Long userId, SseEmitter emitter) {
        ExecutionContext context = new ExecutionContext(
                "exec-" + System.currentTimeMillis(),
                userId,
                Map.of("input", input)
        );
        context.setSseEmitter(emitter);
        context.setObjectMapper(objectMapper);

        try {
            Map<String, NodeResult> results = doExecute(graphJson, context);

            // Find the OUTPUT node result as the final output
            WorkflowGraph graph = workflowParser.parse(graphJson);
            String finalOutput = "";
            String outputType = "text";

            // First try to find the OUTPUT node explicitly
            for (NodeDefinition node : graph.getAllNodes()) {
                if ("OUTPUT".equals(node.getType())) {
                    NodeResult outputResult = results.get(node.getId());
                    if (outputResult != null && outputResult.getOutput() != null) {
                        finalOutput = outputResult.getOutput();
                        // Use the outputType set by the OutputNodeHandler directly
                        if ("audio".equals(outputResult.getOutputType())) {
                            outputType = "audio";
                        }
                    }
                }
            }

            // Fallback: use the last node result
            if (finalOutput.isEmpty()) {
                for (NodeResult result : results.values()) {
                    if (result.getOutput() != null) {
                        finalOutput = result.getOutput();
                        if ("audio".equals(result.getOutputType())) {
                            outputType = "audio";
                        }
                    }
                }
            }

            long totalDuration = results.values().stream().mapToLong(NodeResult::getDurationMs).sum();

            sendSSEEvent(emitter, "execution_complete", ExecutionEvent.builder()
                    .eventType("execution_complete")
                    .status("SUCCESS")
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

    private Map<String, NodeResult> doExecute(Map<String, Object> graphJson, ExecutionContext context) {
        WorkflowGraph graph = workflowParser.parse(graphJson);
        context.setGraph(graph);

        // Register labels for variable resolution
        for (NodeDefinition node : graph.getAllNodes()) {
            context.registerNodeLabel(node.getId(), node.getLabel());
        }

        List<NodeDefinition> sortedNodes = topologicalSorter.sort(graph);
        Map<String, NodeResult> results = new LinkedHashMap<>();

        for (NodeDefinition node : sortedNodes) {
            SseEmitter emitter = context.getSseEmitter();

            // Send node_start event
            if (emitter != null) {
                sendSSEEvent(emitter, "node_start", ExecutionEvent.builder()
                        .eventType("node_start")
                        .nodeId(node.getId())
                        .nodeType(node.getType())
                        .label(node.getLabel())
                        .build());
            }

            // Execute node
            NodeHandler handler = nodeHandlerRegistry.getHandler(node.getType());
            NodeResult result = handler.execute(node, context);
            context.putNodeResult(node.getId(), result);
            results.put(node.getId(), result);

            // Send node_complete or node_error event
            if (emitter != null) {
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
                }
            }

            // Stop execution if node failed
            if ("FAILED".equals(result.getStatus())) {
                log.warn("节点 {} 执行失败，终止工作流", node.getId());
                break;
            }
        }

        return results;
    }

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
