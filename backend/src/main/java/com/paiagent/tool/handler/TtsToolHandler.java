package com.paiagent.tool.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.parser.VariableResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TtsToolHandler implements ToolHandler {

    private static final String DASHSCOPE_TTS_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    private final ObjectMapper objectMapper;
    private final VariableResolver variableResolver;

    @Override
    public String getToolType() {
        return "tts";
    }

    @Override
    public String execute(String input, Map<String, Object> config) {
        // Legacy path — full execution now handled via executeNode
        return callDashScopeTts(input, "Cherry", "Auto",
                (String) config.getOrDefault("apiKey", ""),
                (String) config.getOrDefault("model", "qwen3-tts-flash"));
    }

    @Override
    public NodeResult executeNode(NodeDefinition node, ExecutionContext context) {
        Map<String, Object> config = variableResolver.resolveConfig(node.getConfig(), context);

        String apiKey = (String) config.getOrDefault("apiKey", "");
        String model = (String) config.getOrDefault("model", "qwen3-tts-flash");

        log.info("TTS 节点配置 apiKey 长度={}, model={}", apiKey == null ? 0 : apiKey.length(), model);

        // --- Resolve inputParams ---
        String text = "";
        String voice = "Cherry";
        String languageType = "Auto";

        Object inputParamsObj = config.get("inputParams");
        if (inputParamsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> inputParams = (Map<String, Object>) inputParamsObj;

            // Resolve text (supports input or ref)
            Object textObj = inputParams.get("text");
            if (textObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> textParam = (Map<String, Object>) textObj;
                String type = (String) textParam.getOrDefault("type", "input");
                String value = (String) textParam.getOrDefault("value", "");

                if ("ref".equals(type) && value != null && !value.isEmpty()) {
                    // value is like "nodeId.output" — strip the ".output" suffix to get nodeId
                    String refNodeId = value.contains(".")
                            ? value.substring(0, value.lastIndexOf('.'))
                            : value;
                    String resolved = context.getNodeOutput(refNodeId);
                    if (resolved == null) {
                        resolved = context.getNodeOutputByLabel(refNodeId);
                    }
                    log.info("TTS ref 解析: value={}, refNodeId={}, resolved length={}",
                            value, refNodeId, resolved != null ? resolved.length() : 0);
                    text = resolved != null ? resolved : "";
                } else {
                    text = value != null ? value : "";
                }
            } else if (textObj instanceof String) {
                text = (String) textObj;
            }

            voice = (String) inputParams.getOrDefault("voice", "Cherry");
            languageType = (String) inputParams.getOrDefault("languageType", "Auto");
        }

        // If text is empty or same as raw user input, try using direct upstream node's output
        // This ensures data flows along edges (e.g., LLM output → TTS input)
        if (text.isEmpty() || text.equals(context.getFirstInput())) {
            String upstreamOutput = context.getDirectUpstreamOutput(node.getId());
            if (upstreamOutput != null && !upstreamOutput.isEmpty()) {
                log.info("TTS 使用直接上游节点输出作为合成文本 (长度={})", upstreamOutput.length());
                text = upstreamOutput;
            }
        }

        // --- Get output param name ---
        String outputParamName = "voice_url";
        Object outputParamObj = config.get("outputParam");
        if (outputParamObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> outputParam = (Map<String, Object>) outputParamObj;
            outputParamName = (String) outputParam.getOrDefault("name", "voice_url");
        }

        log.info("TTS 合成: model={}, voice={}, languageType={}, text length={}", model, voice, languageType, text.length());

        if (text.isEmpty()) {
            throw new RuntimeException("TTS 合成失败: 输入文本为空，请检查上游节点（LLM）是否有输出，以及 TTS 节点的 text 参数是否正确配置为引用");
        }

        String audioUrl = callDashScopeTts(text, voice, languageType, apiKey, model);

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put(outputParamName, audioUrl);

        return NodeResult.builder()
                .nodeId(node.getId())
                .nodeName(node.getLabel())
                .status("SUCCESS")
                .output(audioUrl)
                .outputType("audio")
                .outputs(outputs)
                .build();
    }

    private String callDashScopeTts(String text, String voice, String languageType, String apiKey, String model) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("TTS API Key 未配置，返回 Mock URL");
            return "https://mock-tts.example.com/audio-" + System.currentTimeMillis() + ".mp3";
        }

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Build request body per DashScope TTS API spec
            Map<String, Object> input = new HashMap<>();
            input.put("text", text);
            input.put("voice", voice);
            if (languageType != null && !languageType.isEmpty()) {
                input.put("language_type", languageType);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", input);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    DASHSCOPE_TTS_URL, HttpMethod.POST, entity, String.class);

            // Parse response: output.audio.url
            JsonNode root = objectMapper.readTree(response.getBody());
            String audioUrl = root.path("output").path("audio").path("url").asText();

            if (audioUrl == null || audioUrl.isEmpty()) {
                log.error("TTS 响应中未找到 audio URL，响应体: {}", response.getBody());
                throw new RuntimeException("TTS 响应中未找到 audio URL");
            }

            log.info("TTS 合成成功，audio URL: {}", audioUrl);
            return audioUrl;

        } catch (Exception e) {
            log.error("TTS API 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("TTS 合成失败: " + e.getMessage(), e);
        }
    }
}
