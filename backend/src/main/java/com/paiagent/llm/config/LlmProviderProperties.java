package com.paiagent.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM 厂商配置属性，支持多厂商 baseUrl / apiKey 统一管理。
 */
@Data
@Component
@ConfigurationProperties(prefix = "paiagent.llm")
public class LlmProviderProperties {

    private Map<String, ProviderConfig> providers = new HashMap<>();

    @Data
    public static class ProviderConfig {
        private String baseUrl;
        private String apiKey;
    }

    /**
     * 根据 provider 名称获取配置，不存在则返回 null。
     */
    public ProviderConfig getProvider(String provider) {
        if (provider == null) return null;
        return providers.get(provider.toLowerCase());
    }
}
