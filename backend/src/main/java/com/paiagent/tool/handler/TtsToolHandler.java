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
                    // Resolve reference: value is like "nodeId.output"
                    String refNodeId = value.replace(".output", "");
                    String resolved = context.getNodeOutput(refNodeId);
                    if (resolved == null) {
                        resolved = context.getNodeOutputByLabel(refNodeId);
                    }
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

        // --- Get output param name ---
        String outputParamName = "voice_url";
        Object outputParamObj = config.get("outputParam");
        if (outputParamObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> outputParam = (Map<String, Object>) outputParamObj;
            outputParamName = (String) outputParam.getOrDefault("name", "voice_url");
        }

        log.info("TTS 合成: model={}, voice={}, languageType={}, text length={}", model, voice, languageType, text.length());

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
