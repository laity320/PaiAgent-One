package com.paiagent.llm.factory;

import com.paiagent.llm.adapter.LlmAdapter;
import com.paiagent.llm.adapter.LlmAdapterFactory;
import com.paiagent.llm.client.DynamicChatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ChatClientFactory —— 运行时根据节点配置动态创建 ChatClient。
 *
 * <p>支持 OpenAI 兼容协议的多厂商 LLM（OpenAI、DeepSeek、通义千问、智谱等），
 * 节点级 apiUrl / apiKey 可覆盖全局配置，实现厂商无缝切换。
 *
 * <p>用法：
 * <pre>
 *   DynamicChatClient client = chatClientFactory.create(provider, model, apiUrl, apiKey, temperature, maxTokens);
 *   ChatResponse resp = client.chat(systemPrompt, userPrompt);
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatClientFactory {

    private final LlmAdapterFactory adapterFactory;

    /**
     * 根据节点参数创建 DynamicChatClient。
     *
     * @param provider    厂商标识（openai / deepseek / tongyi / zhipu）
     * @param model       模型名称
     * @param apiUrl      节点级 API 地址（可为 null，使用全局配置）
     * @param apiKey      节点级 API Key（可为 null，使用全局配置）
     * @param temperature 温度参数
     * @param maxTokens   最大 Token 数
     * @return 可直接调用的 DynamicChatClient
     */
    public DynamicChatClient create(String provider, String model,
                                    String apiUrl, String apiKey,
                                    double temperature, int maxTokens) {
        LlmAdapter adapter = adapterFactory.getAdapter(provider);
        log.debug("ChatClientFactory 创建 client: provider={}, model={}, hasCustomUrl={}, hasCustomKey={}",
                provider, model,
                apiUrl != null && !apiUrl.isEmpty(),
                apiKey != null && !apiKey.isEmpty());
        return new DynamicChatClient(adapter, model, apiUrl, apiKey, temperature, maxTokens);
    }

    /**
     * 使用默认参数创建 client（temperature=0.7, maxTokens=2048）。
     */
    public DynamicChatClient create(String provider, String model,
                                    String apiUrl, String apiKey) {
        return create(provider, model, apiUrl, apiKey, 0.7, 2048);
    }
}
