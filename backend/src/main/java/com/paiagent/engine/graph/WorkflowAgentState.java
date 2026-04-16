package com.paiagent.engine.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;
import java.util.Optional;

/**
 * WorkflowAgentState —— LangGraph4j AgentState 子类，管理工作流执行状态。
 *
 * <p>状态通道定义（均为 last-value 语义，使用 Channels.base）：
 * <ul>
 *   <li>executionId —— 执行 ID</li>
 *   <li>userInput —— 用户原始输入</li>
 *   <li>aborted —— 是否已中止（默认 false）</li>
 *   <li>abortReason —— 中止原因</li>
 *   <li>lastNodeResult —— 最近节点输出</li>
 * </ul>
 *
 * <p>注意：nodeResults 和 nodeOutputs 仍保留在 ExecutionContext 中，
 * 不通过 AgentState 传递，以避免 Channel 序列化复杂性。
 */
public class WorkflowAgentState extends AgentState {

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            "executionId", Channels.base(() -> ""),
            "userInput", Channels.base(() -> ""),
            "aborted", Channels.base(() -> false),
            "abortReason", Channels.base(() -> ""),
            "lastNodeResult", Channels.base(() -> "")
    );

    public WorkflowAgentState(Map<String, Object> initData) {
        super(initData);
    }

    public boolean isAborted() {
        Optional<Boolean> val = value("aborted");
        return val.orElse(false);
    }

    public String getExecutionId() {
        Optional<String> val = value("executionId");
        return val.orElse("");
    }

    public String getUserInput() {
        Optional<String> val = value("userInput");
        return val.orElse("");
    }

    public String getAbortReason() {
        Optional<String> val = value("abortReason");
        return val.orElse("");
    }

    public String getLastNodeResult() {
        Optional<String> val = value("lastNodeResult");
        return val.orElse("");
    }
}
