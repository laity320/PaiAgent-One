package com.paiagent.engine.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.ExecutionEvent;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.EdgeDefinition;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.model.WorkflowGraph;
import com.paiagent.engine.node.NodeHandler;
import com.paiagent.engine.node.NodeHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * LangGraphWorkflowCompiler —— 将 WorkflowGraph 编译为 LangGraph4j 的 CompiledGraph。
 *
 * <p>编译流程：
 * <ol>
 *   <li>创建 LangGraph4j StateGraph（基于 WorkflowAgentState）</li>
 *   <li>遍历 WorkflowGraph 中的所有节点，为每个节点注册异步 node action</li>
 *   <li>遍历 WorkflowGraph 中的所有边，注册图的边</li>
 *   <li>自动识别入口节点和出口节点，连接 START 和 END</li>
 *   <li>调用 compile() 返回可执行的 CompiledGraph</li>
 * </ol>
 *
 * <p>每个 node action 内部：
 * <ul>
 *   <li>检查 abort 状态（已中止则跳过）</li>
 *   <li>发送 SSE node_start 事件</li>
 *   <li>调用 NodeHandler.execute() 执行节点逻辑</li>
 *   <li>将结果写入 ExecutionContext</li>
 *   <li>发送 SSE node_complete / node_error 事件</li>
 *   <li>返回状态更新 Map</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LangGraphWorkflowCompiler {

    private final NodeHandlerRegistry nodeHandlerRegistry;

    /**
     * 将 WorkflowGraph 编译为 LangGraph4j CompiledGraph。
     *
     * @param graph   解析后的工作流图
     * @param context 执行上下文（含 SSE emitter 等运行时依赖）
     * @return 已编译的可执行图
     */
    public CompiledGraph<WorkflowAgentState> compile(WorkflowGraph graph, ExecutionContext context) {
        try {
            StateGraph<WorkflowAgentState> stateGraph =
                    new StateGraph<>(WorkflowAgentState.SCHEMA, WorkflowAgentState::new);

            // 1. 注册节点
            for (NodeDefinition node : graph.getAllNodes()) {
                NodeHandler handler = nodeHandlerRegistry.getHandler(node.getType());
                log.debug("LangGraphWorkflowCompiler 注册节点: id={}, type={}, label={}", 
                         node.getId(), node.getType(), node.getLabel());

                stateGraph = stateGraph.addNode(node.getId(), node_async(state -> {
                    // 已中止则跳过
                    if (state.isAborted()) {
                        log.warn("图已中止，跳过节点: {}", node.getId());
                        return Map.of();
                    }

                    // 发送 SSE node_start
                    emitSSE(context, "node_start", ExecutionEvent.builder()
                            .eventType("node_start")
                            .nodeId(node.getId())
                            .nodeType(node.getType())
                            .label(node.getLabel())
                            .build());

                    // 执行节点（putNodeResult 内部已处理 nodeOutputs 写入）
                    NodeResult result = handler.execute(node, context);
                    context.putNodeResult(node.getId(), result);

                    // 发送 SSE node_complete 或 node_error
                    Map<String, Object> stateUpdate = new HashMap<>();
                    if ("SUCCESS".equals(result.getStatus())) {
                        emitSSE(context, "node_complete", ExecutionEvent.builder()
                                .eventType("node_complete")
                                .nodeId(node.getId())
                                .nodeType(node.getType())
                                .label(node.getLabel())
                                .inputs(result.getInputs())
                                .output(result.getOutput())
                                .outputType(result.getOutputType())
                                .durationMs(result.getDurationMs())
                                .build());
                        stateUpdate.put("lastNodeResult", result.getOutput() != null ? result.getOutput() : "");
                    } else {
                        emitSSE(context, "node_error", ExecutionEvent.builder()
                                .eventType("node_error")
                                .nodeId(node.getId())
                                .label(node.getLabel())
                                .error(result.getOutput())
                                .build());
                        stateUpdate.put("aborted", true);
                        stateUpdate.put("abortReason", "节点 " + node.getId() + " 执行失败: " + result.getOutput());
                    }
                    return stateUpdate;
                }));
            }

            // 2. 注册边
            for (EdgeDefinition edge : graph.getEdges()) {
                stateGraph = stateGraph.addEdge(edge.getSource(), edge.getTarget());
            }

            // 3. 识别入口节点（没有入边）和出口节点（没有出边），连接 START/END
            Set<String> targets = graph.getEdges().stream()
                    .map(EdgeDefinition::getTarget)
                    .collect(Collectors.toSet());
            Set<String> sources = graph.getEdges().stream()
                    .map(EdgeDefinition::getSource)
                    .collect(Collectors.toSet());

            for (NodeDefinition node : graph.getAllNodes()) {
                if (!targets.contains(node.getId())) {
                    stateGraph = stateGraph.addEdge(START, node.getId());
                    log.debug("LangGraphWorkflowCompiler 连接 START -> {}", node.getId());
                }
                if (!sources.contains(node.getId())) {
                    stateGraph = stateGraph.addEdge(node.getId(), END);
                    log.debug("LangGraphWorkflowCompiler 连接 {} -> END", node.getId());
                }
            }

            log.info("LangGraphWorkflowCompiler 编译完成: {} 个节点, {} 条边",
                    graph.getAllNodes().size(), graph.getEdges().size());

            return stateGraph.compile();
        } catch (Exception e) {
            throw new RuntimeException("LangGraph4j 图编译失败: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // SSE 事件发送
    // -----------------------------------------------------------------------

    private void emitSSE(ExecutionContext context, String eventName, ExecutionEvent event) {
        SseEmitter emitter = context.getSseEmitter();
        if (emitter == null) return;

        try {
            ObjectMapper objectMapper = context.getObjectMapper();
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(objectMapper.writeValueAsString(event)));
        } catch (IOException e) {
            log.warn("SSE 发送失败: {}", e.getMessage());
        }
    }
}
