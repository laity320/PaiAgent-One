package com.paiagent.engine.node;

import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.parser.VariableResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        Map<String, Object> config = variableResolver.resolveConfig(node.getConfig(), context);

        // audioRef is like "nodeId.voice_url" or "nodeId.output"
        String audioRef = (String) config.get("audioRef");
        String output = null;
        String resolvedOutputType = "text";

        NodeResult upstreamResult = null;

        if (audioRef != null && !audioRef.isEmpty()) {
            // Try direct context lookup: "nodeId.paramName" stored in context
            output = context.getNodeOutput(audioRef);

            // Parse nodeId and paramName from the ref
            String nodeId = audioRef.contains(".") ? audioRef.substring(0, audioRef.lastIndexOf('.')) : audioRef;
            String paramName = audioRef.contains(".") ? audioRef.substring(audioRef.lastIndexOf('.') + 1) : "output";

            upstreamResult = context.getNodeResults().get(nodeId);

            if (output == null && upstreamResult != null) {
                if ("output".equals(paramName)) {
                    output = upstreamResult.getOutput();
                } else if (upstreamResult.getOutputs() != null) {
                    Object val = upstreamResult.getOutputs().get(paramName);
                    output = val != null ? val.toString() : null;
                }
            }

            if (output == null) {
                log.warn("输出节点未能解析 audioRef: {}", audioRef);
            }
        }

        // Fallback: use the last upstream node's output
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

        return NodeResult.builder()
                .nodeId(node.getId())
                .nodeName(node.getLabel())
                .status("SUCCESS")
                .output(output)
                .outputType(resolvedOutputType)
                .build();
    }
}
