package com.paiagent.engine.node;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NodeHandlerRegistry {

    private final Map<String, NodeHandler> handlers = new HashMap<>();

    public NodeHandlerRegistry(List<NodeHandler> handlerList) {
        for (NodeHandler handler : handlerList) {
            handlers.put(handler.getType(), handler);
        }
    }

    public NodeHandler getHandler(String type) {
        NodeHandler handler = handlers.get(type.toUpperCase());
        if (handler == null) {
            throw new IllegalArgumentException("未知的节点类型: " + type);
        }
        return handler;
    }
}
