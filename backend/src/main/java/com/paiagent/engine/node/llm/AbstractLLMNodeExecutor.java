package com.paiagent.engine.node.llm;

import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.node.AbstractNodeHandler;
import com.paiagent.engine.parser.VariableResolver;
import com.paiagent.llm.client.DynamicChatClient;
import com.paiagent.llm.dto.ChatResponse;
import com.paiagent.llm.factory.ChatClientFactory;
import com.paiagent.skill.injector.SkillInjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AbstractLLMNodeExecutor —— LLM 节点执行器模板基类。
 *
 * <p>模板方法模式：公共流程固定在 doExecute()，子类仅需实现 {@link #getType()} 返回
 * 节点类型字符串，其余 800+ 行逻辑统一收敛至此，每个子类代码量约 10 行。
 *
 * <p>子类可通过覆写 {@link #customizeSystemPrompt} / {@link #postProcessResponse}
 * 进行个性化扩展，无需重写主流程。
 *
 * <p>支持 Skill 预置知识包注入：若节点配置中包含 skillId，自动将对应 Skill 注入到系统提示词。
 */
@Slf4j
public abstract class AbstractLLMNodeExecutor extends AbstractNodeHandler {

    protected final ChatClientFactory chatClientFactory;
    protected final VariableResolver variableResolver;

    /** Skill 注入器（可选依赖，若未注入则跳过 Skill 功能） */
    protected SkillInjector skillInjector;

    @Autowired
    public void setSkillInjector(SkillInjector skillInjector) {
        this.skillInjector = skillInjector;
    }

    protected AbstractLLMNodeExecutor(ChatClientFactory chatClientFactory,
                                       VariableResolver variableResolver) {
        this.chatClientFactory = chatClientFactory;
        this.variableResolver = variableResolver;
    }

    // -----------------------------------------------------------------------
    // 模板方法：子类必须实现
    // -----------------------------------------------------------------------

    /**
     * 返回该执行器处理的节点类型，如 "LLM"、"SUMMARIZE"、"TRANSLATE" 等。
     */
    @Override
    public abstract String getType();

    // -----------------------------------------------------------------------
    // 模板方法：子类可覆写（可选扩展点）
    // -----------------------------------------------------------------------

    /**
     * 对系统提示词进行个性化处理（默认直接返回原始值）。
     */
    protected String customizeSystemPrompt(String systemPrompt, Map<String, Object> config) {
        return systemPrompt;
    }

    /**
     * 对 LLM 响应进行后处理（默认直接返回原始内容）。
     */
    protected String postProcessResponse(String content, Map<String, Object> config) {
        return content;
    }

    // -----------------------------------------------------------------------
    // 固定执行流程
    // -----------------------------------------------------------------------

    @Override
    protected final NodeResult doExecute(NodeDefinition node, ExecutionContext context) {
        Map<String, Object> config = variableResolver.resolveConfig(node.getConfig(), context);

        String provider    = (String) config.getOrDefault("provider", "openai");
        String model       = (String) config.getOrDefault("model", "gpt-4o-mini");
        String systemPrompt = (String) config.getOrDefault("systemPrompt", "");
        String userPrompt  = (String) config.getOrDefault("userPrompt",
                config.getOrDefault("userPromptTemplate", ""));
        String apiUrl = (String) config.get("apiUrl");
        String apiKey = (String) config.get("apiKey");

        double temperature = config.containsKey("temperature")
                ? ((Number) config.get("temperature")).doubleValue() : 0.7;
        int maxTokens = config.containsKey("maxTokens")
                ? ((Number) config.get("maxTokens")).intValue() : 2048;

        // 处理 inputParams 并解析变量
        Map<String, String> inputParamValues = resolveInputParams(config, context);
        String resolvedUserPrompt = variableResolver.resolveWithParams(userPrompt, context, inputParamValues);

        // 子类个性化系统提示词
        systemPrompt = customizeSystemPrompt(systemPrompt, config);

        // Skill 预置知识包注入
        String skillId = (String) config.get("skillId");
        if (skillId != null && !skillId.isEmpty() && skillInjector != null) {
            systemPrompt = skillInjector.inject(systemPrompt, skillId);
            log.debug("已注入 Skill: {}", skillId);
        }

        // 读取输出参数配置
        String outputParamName = "output";
        if (config.get("outputParam") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> op = (Map<String, Object>) config.get("outputParam");
            outputParamName = (String) op.getOrDefault("name", "output");
        }

        // 动态创建 ChatClient 并调用
        DynamicChatClient client = chatClientFactory.create(provider, model, apiUrl, apiKey, temperature, maxTokens);
        ChatResponse response = client.chat(systemPrompt, resolvedUserPrompt);

        // 子类后处理响应
        String content = postProcessResponse(response.getContent(), config);

        // 构建输出
        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put(outputParamName, content);

        Map<String, Object> inputs = buildInputsForDisplay(model, provider, inputParamValues, resolvedUserPrompt);

        return NodeResult.builder()
                .nodeId(node.getId())
                .nodeName(node.getLabel())
                .status("SUCCESS")
                .inputs(inputs)
                .output(content)
                .outputType("text")
                .outputs(outputs)
                .build();
    }

    // -----------------------------------------------------------------------
    // 私有工具方法
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, String> resolveInputParams(Map<String, Object> config, ExecutionContext context) {
        Map<String, String> result = new HashMap<>();
        Object inputParamsObj = config.get("inputParams");
        if (!(inputParamsObj instanceof List)) return result;

        for (Object paramObj : (List<?>) inputParamsObj) {
            if (!(paramObj instanceof Map)) continue;
            Map<String, Object> param = (Map<String, Object>) paramObj;
            String name  = (String) param.get("name");
            String type  = (String) param.getOrDefault("type", "input");
            Object valueObj = param.get("value");
            String value = valueObj != null ? valueObj.toString() : "";

            if (name == null || name.isEmpty()) continue;

            if ("ref".equals(type) && value != null && !value.isEmpty()) {
                String resolved = resolveRef(value, context);
                result.put(name, resolved != null ? resolved : value);
            } else {
                result.put(name, value != null ? value : "");
            }
        }
        return result;
    }

    private String resolveRef(String ref, ExecutionContext context) {
        if (ref == null || ref.isEmpty()) return null;
        String nodeName = ref.contains(".output") ? ref.replace(".output", "") : ref;
        String value = context.getNodeOutput(nodeName);
        return value != null ? value : context.getNodeOutputByLabel(nodeName);
    }

    private Map<String, Object> buildInputsForDisplay(String model, String provider,
                                                       Map<String, String> inputParamValues,
                                                       String resolvedUserPrompt) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("model", model);
        inputs.put("provider", provider);
        inputParamValues.forEach((k, v) -> {
            String display = v != null && v.length() > 200 ? v.substring(0, 200) + "..." : v;
            inputs.put(k, display);
        });
        if (resolvedUserPrompt != null && !resolvedUserPrompt.isEmpty()) {
            inputs.put("prompt", resolvedUserPrompt.length() > 300
                    ? resolvedUserPrompt.substring(0, 300) + "..."
                    : resolvedUserPrompt);
        }
        return inputs;
    }
}
