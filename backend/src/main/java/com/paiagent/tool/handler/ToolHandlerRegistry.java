package com.paiagent.tool.handler;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolHandlerRegistry {

    private final Map<String, ToolHandler> handlers = new HashMap<>();

    public ToolHandlerRegistry(List<ToolHandler> handlerList) {
        for (ToolHandler handler : handlerList) {
            handlers.put(handler.getToolType(), handler);
        }
    }

    public ToolHandler getHandler(String toolType) {
        ToolHandler handler = handlers.get(toolType);
        if (handler == null) {
            throw new IllegalArgumentException("未知的工具类型: " + toolType);
        }
        return handler;
    }
}
