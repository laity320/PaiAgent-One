package com.paiagent.common.enums;

public enum LlmProvider {
    OPENAI("openai", "https://api.openai.com"),
    DEEPSEEK("deepseek", "https://api.deepseek.com"),
    QWEN("qwen", "https://dashscope.aliyuncs.com"),
    ZHIPU("zhipu", "https://open.bigmodel.cn");

    private final String code;
    private final String defaultBaseUrl;

    LlmProvider(String code, String defaultBaseUrl) {
        this.code = code;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    public String getCode() { return code; }
    public String getDefaultBaseUrl() { return defaultBaseUrl; }

    public static LlmProvider fromCode(String code) {
        for (LlmProvider p : values()) {
            if (p.code.equalsIgnoreCase(code)) return p;
        }
        throw new IllegalArgumentException("未知的大模型提供商: " + code);
    }
}
