package com.paiagent.engine.parser;

import com.paiagent.engine.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VariableResolver {

    // Pattern for {{nodeId.output}} or {{nodeName.output}}
    private static final Pattern NODE_OUTPUT_PATTERN = Pattern.compile("\\{\\{(\\w+)\\.output}}");

    // Pattern for {{paramName}} - simple variable substitution
    private static final Pattern SIMPLE_VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    public String resolve(String template, ExecutionContext context) {
        if (template == null) return null;

        Matcher matcher = NODE_OUTPUT_PATTERN.matcher(template);
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

    /**
     * Resolve variables in a template using inputParams.
     * Supports {{paramName}} pattern where paramName matches an input parameter name.
     *
     * @param template The template string containing {{paramName}} placeholders
     * @param context The execution context
     * @param inputParams Map of parameter name to resolved value
     * @return The resolved string
     */
    public String resolveWithParams(String template, ExecutionContext context, Map<String, String> inputParams) {
        if (template == null) return null;

        String result = template;

        // First resolve node output references like {{nodeId.output}}
        result = resolve(result, context);

        // Then resolve input parameters like {{paramName}}
        if (inputParams != null && !inputParams.isEmpty()) {
            Matcher matcher = SIMPLE_VAR_PATTERN.matcher(result);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String paramName = matcher.group(1);
                String value = inputParams.get(paramName);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : ""));
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }

        return result;
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
