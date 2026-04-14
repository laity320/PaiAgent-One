package com.paiagent.engine.node;

import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.parser.VariableResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

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

        String outputTemplate = (String) config.getOrDefault("outputTemplate", "{{output}}");

        // Collect all outputs
        StringBuilder allOutputs = new StringBuilder();
        String audioUrl = null;

        for (var entry : context.getNodeResults().entrySet()) {
            var result = entry.getValue();
            if (result.getOutput() != null) {
                // Check if the output looks like an audio URL (absolute or relative)
                String out = result.getOutput();
                if (out.matches(".*\\.(wav|mp3|ogg|aac|flac)(\\?.*)?$") ||
                    (out.contains("/audio/") && (out.startsWith("http") || out.startsWith("/")))) {
                    audioUrl = out;
                }
            }
        }

        // Resolve the output template
        String output = outputTemplate;
        if (output.contains("{{output}}")) {
            // Replace {{output}} with the last non-null output
            String lastOutput = context.getNodeResults().values().stream()
                    .filter(r -> r.getOutput() != null && !"SUCCESS".equals(r.getOutput()))
                    .reduce((a, b) -> b)
                    .map(r -> r.getOutput())
                    .orElse("");
            output = output.replace("{{output}}", lastOutput);
        }

        // If there is audio, append it
        if (audioUrl != null) {
            output = audioUrl;
        }

        return NodeResult.builder()
                .nodeId(node.getId())
                .nodeName(node.getLabel())
                .status("SUCCESS")
                .output(output)
                .build();
    }
}
