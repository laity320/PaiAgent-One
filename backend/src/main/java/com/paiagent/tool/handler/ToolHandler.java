package com.paiagent.tool.handler;

import java.util.Map;

public interface ToolHandler {
    String getToolType();
    String execute(String input, Map<String, Object> config);
}
