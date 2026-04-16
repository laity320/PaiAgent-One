package com.paiagent.engine.node.llm;

import com.paiagent.engine.parser.VariableResolver;
import com.paiagent.llm.factory.ChatClientFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * CodeGenLLMNodeExecutor —— 代码生成节点执行器。
 * 自动注入"你是一个专业的代码生成助手"系统提示词，响应后自动提取代码块。
 */
@Component
public class CodeGenLLMNodeExecutor extends AbstractLLMNodeExecutor {

    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个专业的代码生成助手。请根据用户需求生成高质量代码。" +
            "只输出代码块，不要添加多余的解释说明。使用 Markdown 代码块格式。";

    public CodeGenLLMNodeExecutor(ChatClientFactory chatClientFactory,
                                  VariableResolver variableResolver) {
        super(chatClientFactory, variableResolver);
    }

    @Override
    public String getType() {
        return "CODEGEN";
    }

    @Override
    protected String customizeSystemPrompt(String systemPrompt, Map<String, Object> config) {
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        return systemPrompt;
    }

    @Override
    protected String postProcessResponse(String content, Map<String, Object> config) {
        // 尝试提取第一个代码块
        if (content == null) return null;
        int start = content.indexOf("```");
        if (start == -1) return content;
        int end = content.lastIndexOf("```");
        if (end <= start) return content;
        // 跳过语言标识行
        String codeBlock = content.substring(start, end + 3);
        int firstNewline = codeBlock.indexOf('\n');
        if (firstNewline != -1 && firstNewline < codeBlock.length() - 1) {
            codeBlock = codeBlock.substring(firstNewline + 1);
        }
        // 移除结尾的 ```
        if (codeBlock.endsWith("```")) {
            codeBlock = codeBlock.substring(0, codeBlock.length() - 3);
        }
        return codeBlock.trim();
    }
}
