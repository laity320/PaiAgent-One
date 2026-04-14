package com.paiagent.engine.parser;

import com.paiagent.engine.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VariableResolver {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)\\.output}}");

    public String resolve(String template, ExecutionContext context) {
        if (template == null) return null;

        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String nodeId = matcher.group(1);
            // Also try to match by label
            String value = context.getNodeOutput(nodeId);
            if (value == null) {
                // Try resolving by label
                value = context.getNodeOutputByLabel(nodeId);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> resolveConfig(Map<String, Object> config, ExecutionContext context) {
        if (config == null) return Map.of();

        // Deep copy and resolve all string values
        java.util.Map<String, Object> resolved = new java.util.LinkedHashMap<>();
        for (var entry : config.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                resolved.put(entry.getKey(), resolve((String) value, context));
            } else if (value instanceof Map) {
                resolved.put(entry.getKey(), resolveConfig((Map<String, Object>) value, context));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }
}
