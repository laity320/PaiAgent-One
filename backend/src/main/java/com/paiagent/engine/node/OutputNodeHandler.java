package com.paiagent.engine.node;

import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.parser.VariableResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutputNodeHandler extends AbstractNodeHandler {

    private final VariableResolver variableResolver;

    @Override
    public String getType() {
        return "OUTPUT";
    }

    @Override
    protected NodeResult doExecute(NodeDefinition node, ExecutionContext context) {
        String output = null;
        String resolvedOutputType = "text";
        String audioRef = null;
        NodeResult upstreamResult = null;

        // Priority 1: Use the direct upstream node's output (based on graph edges)
        upstreamResult = context.getDirectUpstreamResult(node.getId());
        if (upstreamResult != null && upstreamResult.getOutput() != null) {
            output = upstreamResult.getOutput();
            log.info("输出节点使用直接上游节点输出: nodeId={}", upstreamResult.getNodeId());
        }

        // Priority 2: Try audioRef from config
        if (output == null) {
            Map<String, Object> config = variableResolver.resolveConfig(node.getConfig(), context);
            audioRef = (String) config.get("audioRef");

            if (audioRef != null && !audioRef.isEmpty()) {
                output = context.getNodeOutput(audioRef);

                String refNodeId = audioRef.contains(".") ? audioRef.substring(0, audioRef.lastIndexOf('.')) : audioRef;
                String paramName = audioRef.contains(".") ? audioRef.substring(audioRef.lastIndexOf('.') + 1) : "output";

                upstreamResult = context.getNodeResults().get(refNodeId);

                if (output == null && upstreamResult != null) {
                    if ("output".equals(paramName)) {
                        output = upstreamResult.getOutput();
                    } else if (upstreamResult.getOutputs() != null) {
                        Object val = upstreamResult.getOutputs().get(paramName);
                        output = val != null ? val.toString() : null;
                    }
                }

                if (output != null) {
                    log.info("输出节点使用 audioRef 输出: {}", audioRef);
                } else {
                    log.warn("输出节点未能解析 audioRef: {}", audioRef);
                }
            }
        }

        // Priority 3: Fallback — use the last node result
        if (output == null) {
            for (Map.Entry<String, NodeResult> entry : context.getNodeResults().entrySet()) {
                if (!entry.getKey().equals(node.getId()) && entry.getValue().getOutput() != null) {
                    output = entry.getValue().getOutput();
                    upstreamResult = entry.getValue();
                }
            }
            log.info("输出节点使用 fallback 输出");
        }

        if (output == null) output = "";

        // Inherit outputType from the upstream node that provided the value
        if (upstreamResult != null && "audio".equals(upstreamResult.getOutputType())) {
            resolvedOutputType = "audio";
        }

        log.info("输出节点最终输出 type={}, content={}", resolvedOutputType, output);

        // Record inputs for display
        Map<String, Object> inputs = new LinkedHashMap<>();
        if (audioRef != null && !audioRef.isEmpty()) {
            inputs.put("audioRef", audioRef);
        }

        return NodeResult.builder()
                .nodeId(node.getId())
                .nodeName(node.getLabel())
                .status("SUCCESS")
                .inputs(inputs)
                .output(output)
                .outputType(resolvedOutputType)
                .build();
    }
}
