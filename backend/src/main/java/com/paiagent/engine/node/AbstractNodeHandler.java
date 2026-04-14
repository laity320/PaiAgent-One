package com.paiagent.engine.node;

import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractNodeHandler implements NodeHandler {

    @Override
    public NodeResult execute(NodeDefinition node, ExecutionContext context) {
        long start = System.currentTimeMillis();
        log.info("开始执行节点: {} ({}), type={}", node.getLabel(), node.getId(), node.getType());
        try {
            NodeResult result = doExecute(node, context);
            long duration = System.currentTimeMillis() - start;
            result.setDurationMs(duration);
            log.info("节点执行完成: {} ({}), 耗时 {}ms", node.getLabel(), node.getId(), duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("节点执行失败: {} ({}), error={}", node.getLabel(), node.getId(), e.getMessage(), e);
            return NodeResult.builder()
                    .nodeId(node.getId())
                    .nodeName(node.getLabel())
                    .status("FAILED")
                    .output("执行失败: " + e.getMessage())
                    .durationMs(duration)
                    .build();
        }
    }

    protected abstract NodeResult doExecute(NodeDefinition node, ExecutionContext context);
}
