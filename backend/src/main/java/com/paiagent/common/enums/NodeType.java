package com.paiagent.common.enums;

public enum NodeType {
    INPUT,
    LLM,
    TOOL,
    OUTPUT;

    public static NodeType fromString(String type) {
        try {
            return NodeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("未知节点类型: " + type);
        }
    }
}
