package com.paiagent.llm.factory;

import com.paiagent.llm.config.LlmProviderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * ChatClientFactory —— 运行时根据节点配置动态创建 Spring AI ChatModel。
 *
 * <p>基于 Spring AI 的 OpenAiChatModel 编程式创建，支持 OpenAI 兼容协议的多厂商 LLM
 * （OpenAI、DeepSeek、通义千问、智谱等），节点级 apiUrl / apiKey 可覆盖全局配置，
 * 实现厂商无缝切换。
 *
 * <p>用法：
 * <pre>
 *   ChatModel chatModel = chatClientFactory.create(provider, model, apiUrl, apiKey, temperature, maxTokens);
 *   ChatResponse response = chatModel.call(new Prompt(messages));
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatClientFactory {

    private final LlmProviderProperties providerProperties;

    /**
     * 根据节点参数动态创建 Spring AI ChatModel。
     *
     * @param provider    厂商标识（openai / deepseek / tongyi / zhipu）
     * @param model       模型名称
     * @param apiUrl      节点级 API 地址（可为 null，使用全局配置）
     * @param apiKey      节点级 API Key（可为 null，使用全局配置）
     * @param temperature 温度参数
     * @param maxTokens   最大 Token 数
     * @return Spring AI ChatModel
     */
    public ChatModel create(String provider, String model,
                            String apiUrl, String apiKey,
                            double temperature, int maxTokens) {
        String effectiveUrl = resolveUrl(provider, apiUrl);
        String effectiveKey = resolveKey(provider, apiKey);

        log.debug("ChatClientFactory 创建 ChatModel: provider={}, model={}, hasCustomUrl={}, hasCustomKey={}",
                provider, model,
                apiUrl != null && !apiUrl.isEmpty(),
                apiKey != null && !apiKey.isEmpty());

        // Spring AI 编程式创建 OpenAiChatModel（兼容所有 OpenAI 协议厂商）
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(effectiveUrl))
                .apiKey(effectiveKey)
                .build();

        OpenAiChatOptions opts = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(opts)
                .build();
    }

    /**
     * 使用默认参数创建 ChatModel（temperature=0.7, maxTokens=2048）。
     */
    public ChatModel create(String provider, String model,
                            String apiUrl, String apiKey) {
        return create(provider, model, apiUrl, apiKey, 0.7, 2048);
    }

    // -----------------------------------------------------------------------
    // 辅助方法
    // -----------------------------------------------------------------------

    private String resolveUrl(String provider, String apiUrl) {
        if (apiUrl != null && !apiUrl.isEmpty()) {
            return apiUrl;
        }
        LlmProviderProperties.ProviderConfig config = providerProperties.getProvider(provider);
        if (config != null && config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            return config.getBaseUrl();
        }
        // fallback 到 openai 默认配置
        LlmProviderProperties.ProviderConfig openaiConfig = providerProperties.getProvider("openai");
        return openaiConfig != null ? openaiConfig.getBaseUrl() : "https://api.openai.com";
    }

    private String resolveKey(String provider, String apiKey) {
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }
        LlmProviderProperties.ProviderConfig config = providerProperties.getProvider(provider);
        if (config != null && config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            return config.getApiKey();
        }
        // fallback 到 openai 默认配置
        LlmProviderProperties.ProviderConfig openaiConfig = providerProperties.getProvider("openai");
        return openaiConfig != null ? openaiConfig.getApiKey() : "";
    }

    /**
     * URL 标准化：Spring AI 的 OpenAiApi 会自动拼接 /v1/chat/completions，
     * 需要去除用户配置中多余的路径后缀。
     */
    private String normalizeBaseUrl(String url) {
        if (url == null || url.isEmpty()) return "https://api.openai.com";

        // 去除末尾斜杠
        url = url.replaceAll("/+$", "");

        // 去除常见的多余路径
        if (url.endsWith("/v1/chat/completions")) {
            url = url.substring(0, url.length() - "/v1/chat/completions".length());
        } else if (url.endsWith("/chat/completions")) {
            url = url.substring(0, url.length() - "/chat/completions".length());
        }

        // 去除末尾的 /v1（Spring AI 会自动加）
        if (url.endsWith("/v1")) {
            url = url.substring(0, url.length() - "/v1".length());
        }

        return url;
    }
}
