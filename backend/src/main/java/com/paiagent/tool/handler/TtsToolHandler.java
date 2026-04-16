package com.paiagent.tool.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiagent.engine.context.ExecutionContext;
import com.paiagent.engine.dto.NodeResult;
import com.paiagent.engine.model.NodeDefinition;
import com.paiagent.engine.parser.VariableResolver;
import com.paiagent.tool.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TtsToolHandler implements ToolHandler {

    private static final String DASHSCOPE_TTS_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    private static final int MAX_CONCURRENT_REQUESTS = 2; // 限制并发数，避免触发限流

    /** 每段最大字节数（UTF-8: 中文 3 字节/字符，英文 1 字节/字符，900 字节约等于 300 中文字符） */
    private static final int SEGMENT_MAX_BYTES = 900;

    /** 标点断句优先级：优先在以下标点处分段 */
    private static final Pattern SENTENCE_BREAK_PATTERN =
            Pattern.compile("([。！？!?\\.]+|[,，、;；：:]+|\\s+)");

    private final ObjectMapper objectMapper;
    private final VariableResolver variableResolver;
    private final FileStorageService fileStorageService;
    private final RestTemplate restTemplate;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Semaphore rateLimiter = new Semaphore(MAX_CONCURRENT_REQUESTS);

    public TtsToolHandler(ObjectMapper objectMapper, VariableResolver variableResolver,
                          FileStorageService fileStorageService) {
        this.objectMapper = objectMapper;
        this.variableResolver = variableResolver;
        this.fileStorageService = fileStorageService;

        // 配置超时
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30秒连接超时
        factory.setReadTimeout(60000);    // 60秒读取超时
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String getToolType() {
        return "tts";
    }

    @Override
    public String execute(String input, Map<String, Object> config) {
        return callDashScopeTtsSingle(input, "Cherry", "Auto",
                (String) config.getOrDefault("apiKey", ""),
                (String) config.getOrDefault("model", "qwen3-tts-flash"));
    }

    @Override
    public NodeResult executeNode(NodeDefinition node, ExecutionContext context) {
        Map<String, Object> config = variableResolver.resolveConfig(node.getConfig(), context);

        String apiKey = (String) config.getOrDefault("apiKey", "");
        String model = (String) config.getOrDefault("model", "qwen3-tts-flash");
        String nodeId = node.getId();
        String nodeLabel = node.getLabel();

        log.info("TTS 节点配置 apiKey 长度={}, model={}", apiKey == null ? 0 : apiKey.length(), model);

        // --- Resolve inputParams ---
        String text = "";
        String voice = "Cherry";
        String languageType = "Auto";

        Object inputParamsObj = config.get("inputParams");
        if (inputParamsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> inputParams = (Map<String, Object>) inputParamsObj;

            Object textObj = inputParams.get("text");
            if (textObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> textParam = (Map<String, Object>) textObj;
                String type = (String) textParam.getOrDefault("type", "input");
                String value = (String) textParam.getOrDefault("value", "");

                if ("ref".equals(type) && value != null && !value.isEmpty()) {
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

        if (text.isEmpty() || text.equals(context.getFirstInput())) {
            String upstreamOutput = context.getDirectUpstreamOutput(nodeId);
            if (upstreamOutput != null && !upstreamOutput.isEmpty()) {
                log.info("TTS 使用直接上游节点输出作为合成文本 (长度={})", upstreamOutput.length());
                text = upstreamOutput;
            }
        }

        String outputParamName = "voice_url";
        Object outputParamObj = config.get("outputParam");
        if (outputParamObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> outputParam = (Map<String, Object>) outputParamObj;
            outputParamName = (String) outputParam.getOrDefault("name", "voice_url");
        }

        log.info("TTS 合成: model={}, voice={}, languageType={}, text length={}", model, voice, languageType, text.length());

        if (text.isEmpty()) {
            throw new RuntimeException("TTS 合成失败: 输入文本为空");
        }

        // 分段并发合成
        String audioUrl = synthesizeParallel(text, voice, languageType, apiKey, model, context, nodeId, nodeLabel);

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put(outputParamName, audioUrl);

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("model", model);
        inputs.put("voice", voice);
        inputs.put("languageType", languageType);
        String textPreview = text.length() > 200 ? text.substring(0, 200) + "..." : text;
        inputs.put("text", textPreview);

        return NodeResult.builder()
                .nodeId(nodeId)
                .nodeName(nodeLabel)
                .status("SUCCESS")
                .inputs(inputs)
                .output(audioUrl)
                .outputType("audio")
                .outputs(outputs)
                .build();
    }

    /**
     * 分段并发合成音频
     */
    private String synthesizeParallel(String text, String voice, String languageType,
                                       String apiKey, String model,
                                       ExecutionContext context, String nodeId, String nodeLabel) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("TTS API Key 未配置，返回 Mock URL");
            return "https://mock-tts.example.com/audio-" + System.currentTimeMillis() + ".mp3";
        }

        // 1. 分段
        List<String> segments = splitText(text);
        int totalSegments = segments.size();
        log.info("TTS 文本分段: 共 {} 段", totalSegments);

        // 发送初始进度
        if (context != null) {
            context.sendProgressEvent(nodeId, nodeLabel, 5, "准备分段合成...");
        }

        // 2. 并发合成每段
        AtomicInteger completedCount = new AtomicInteger(0);
        List<CompletableFuture<byte[]>> futures = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            final int segmentIndex = i;
            final String segmentText = segments.get(i);

            CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(() -> {
                try {
                    rateLimiter.acquire(); // 限流

                    String url = callDashScopeTtsSingle(segmentText, voice, languageType, apiKey, model);
                    byte[] audioData = downloadAudio(url);

                    // 更新进度
                    int completed = completedCount.incrementAndGet();
                    int progress = 5 + (completed * 90 / totalSegments);
                    if (context != null) {
                        context.sendProgressEvent(nodeId, nodeLabel, progress,
                                "合成中 " + completed + "/" + totalSegments);
                    }

                    return audioData;
                } catch (Exception e) {
                    log.error("分段 {} 合成失败: {}", segmentIndex, e.getMessage());
                    return null;
                } finally {
                    rateLimiter.release();
                }
            }, executor);

            futures.add(future);
        }

        // 3. 等待所有分段完成（最多等待5分钟）
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, TimeUnit.MINUTES);
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("TTS 合成超时");
            throw new RuntimeException("TTS 合成超时（超过5分钟）");
        } catch (Exception e) {
            log.error("TTS 合成等待失败: {}", e.getMessage());
            throw new RuntimeException("TTS 合成失败: " + e.getMessage());
        }

        // 4. 收集结果
        List<byte[]> audioParts = new ArrayList<>();
        for (CompletableFuture<byte[]> future : futures) {
            try {
                byte[] data = future.get();
                if (data != null) {
                    audioParts.add(data);
                }
            } catch (Exception e) {
                log.warn("获取分段音频失败: {}", e.getMessage());
            }
        }

        if (audioParts.isEmpty()) {
            throw new RuntimeException("TTS 合成失败: 所有分段都失败");
        }

        // 5. 合并音频
        if (context != null) {
            context.sendProgressEvent(nodeId, nodeLabel, 95, "合并音频...");
        }

        byte[] mergedAudio;
        try {
            mergedAudio = mergeWavFiles(audioParts);
        } catch (IOException e) {
            throw new RuntimeException("合并音频失败: " + e.getMessage(), e);
        }

        // 6. 保存到本地
        String filename = "tts-" + System.currentTimeMillis() + ".wav";
        try {
            String localUrl = fileStorageService.store(filename, new ByteArrayInputStream(mergedAudio));
            log.info("TTS 合成成功 (本地文件): {}, 大小: {} bytes", localUrl, mergedAudio.length);

            if (context != null) {
                context.sendProgressEvent(nodeId, nodeLabel, 100, "完成");
            }

            return localUrl;
        } catch (IOException e) {
            throw new RuntimeException("保存音频失败: " + e.getMessage(), e);
        }
    }

    /**
     * 单次 TTS 请求
     */
    private String callDashScopeTtsSingle(String text, String voice, String languageType,
                                          String apiKey, String model) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

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

            JsonNode root = objectMapper.readTree(response.getBody());
            String audioUrl = root.path("output").path("audio").path("url").asText();

            if (audioUrl == null || audioUrl.isEmpty()) {
                throw new RuntimeException("响应中未找到 audio URL");
            }

            return audioUrl;
        } catch (Exception e) {
            throw new RuntimeException("TTS 请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载音频文件
     */
    private byte[] downloadAudio(String url) throws IOException {
        URL audioUrl = new URL(url);
        try (InputStream is = audioUrl.openStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    /**
     * 合并多个 WAV 文件
     */
    private byte[] mergeWavFiles(List<byte[]> wavFiles) throws IOException {
        if (wavFiles.size() == 1) {
            return wavFiles.get(0);
        }

        // 计算总数据大小
        int totalDataSize = 0;
        int sampleRate = 16000;
        short channels = 1;
        short bitsPerSample = 16;

        for (byte[] wav : wavFiles) {
            // 跳过 WAV header (44 bytes)
            totalDataSize += wav.length - 44;

            // 从第一个文件读取音频参数
            if (sampleRate == 16000 && wav.length > 44) {
                // 解析 WAV header
                java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(wav, 24, 16);
                bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                sampleRate = bb.getInt();
                bb.getInt(); // byte rate
                short blockAlign = bb.getShort();
                bitsPerSample = bb.getShort();
            }
        }

        // 构建合并后的 WAV
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        java.nio.ByteBuffer header = java.nio.ByteBuffer.allocate(44);
        header.order(java.nio.ByteOrder.LITTLE_ENDIAN);

        // RIFF header
        header.put("RIFF".getBytes());
        header.putInt(36 + totalDataSize);
        header.put("WAVE".getBytes());

        // fmt chunk
        header.put("fmt ".getBytes());
        header.putInt(16);
        header.putShort((short) 1); // PCM
        header.putShort(channels);
        header.putInt(sampleRate);
        header.putInt(sampleRate * channels * bitsPerSample / 8);
        header.putShort((short) (channels * bitsPerSample / 8));
        header.putShort(bitsPerSample);

        // data chunk
        header.put("data".getBytes());
        header.putInt(totalDataSize);

        baos.write(header.array());

        // 写入所有音频数据
        for (byte[] wav : wavFiles) {
            baos.write(wav, 44, wav.length - 44);
        }

        return baos.toByteArray();
    }

    /**
     * UTF-8 字节级智能分段。
     *
     * <p>分段策略：
     * <ol>
     *   <li>优先按句末标点（。！？!?）分段</li>
     *   <li>若单句超长，按次级标点（，、;：）分段</li>
     *   <li>若仍超长，按 UTF-8 字节边界截断（确保不截断多字节字符）</li>
     * </ol>
     */
    private List<String> splitText(String text) {
        List<String> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }

        // 1. 按句末标点分段
        List<String> sentences = splitByPunctuation(text, "[。！？!?\\.]+");
        log.debug("TTS 按句末标点分段: {} 段", sentences.size());

        StringBuilder currentSegment = new StringBuilder();
        int currentBytes = 0;

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;

            int sentenceBytes = getUtf8ByteLength(sentence);

            // 如果当前段 + 新句子不超过限制，直接追加
            if (currentBytes + sentenceBytes <= SEGMENT_MAX_BYTES || currentSegment.length() == 0) {
                currentSegment.append(sentence);
                currentBytes += sentenceBytes;
            } else {
                // 当前段已满，先保存
                if (currentSegment.length() > 0) {
                    segments.add(currentSegment.toString().trim());
                }
                // 检查新句子是否超限
                if (sentenceBytes > SEGMENT_MAX_BYTES) {
                    // 超长句子需要进一步分割
                    segments.addAll(splitLongSentence(sentence));
                    currentSegment = new StringBuilder();
                    currentBytes = 0;
                } else {
                    currentSegment = new StringBuilder(sentence);
                    currentBytes = sentenceBytes;
                }
            }
        }

        if (currentSegment.length() > 0) {
            segments.add(currentSegment.toString().trim());
        }

        // 确保至少有一段
        if (segments.isEmpty()) {
            segments.add(text);
        }

        log.info("TTS 文本分段完成: 共 {} 段", segments.size());
        return segments;
    }

    /**
     * 按指定标点正则分段，保留标点。
     */
    private List<String> splitByPunctuation(String text, String punctuationRegex) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("(.+?(" + punctuationRegex + "))");
        Matcher matcher = pattern.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            result.add(matcher.group(1));
            lastEnd = matcher.end();
        }

        // 添加剩余文本
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd).trim();
            if (!remaining.isEmpty()) {
                result.add(remaining);
            }
        }

        // 如果没有匹配到任何分隔符，返回原文本
        if (result.isEmpty()) {
            result.add(text);
        }

        return result;
    }

    /**
     * 分割超长句子：先按次级标点，若仍超长则按字节边界截断。
     */
    private List<String> splitLongSentence(String sentence) {
        List<String> result = new ArrayList<>();
        int sentenceBytes = getUtf8ByteLength(sentence);

        if (sentenceBytes <= SEGMENT_MAX_BYTES) {
            result.add(sentence);
            return result;
        }

        // 尝试按次级标点分段
        List<String> subParts = splitByPunctuation(sentence, "[,，、;；：:\\s]+");
        boolean allFit = true;
        for (String part : subParts) {
            if (getUtf8ByteLength(part) > SEGMENT_MAX_BYTES) {
                allFit = false;
                break;
            }
        }

        if (allFit) {
            return subParts;
        }

        // 仍超长，按 UTF-8 字节边界截断
        result.addAll(splitByByteBoundary(sentence));
        return result;
    }

    /**
     * 按 UTF-8 字节边界截断，确保不截断多字节字符。
     */
    private List<String> splitByByteBoundary(String text) {
        List<String> result = new ArrayList<>();
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        int start = 0;
        while (start < bytes.length) {
            int end = Math.min(start + SEGMENT_MAX_BYTES, bytes.length);

            // 向回调整到有效的 UTF-8 字符边界
            while (end < bytes.length && !isValidUtf8Start(bytes, end)) {
                end--;
            }

            String segment = new String(bytes, start, end - start, StandardCharsets.UTF_8);
            result.add(segment);
            start = end;
        }

        return result;
    }

    /**
     * 检查给定位置是否是有效的 UTF-8 字符起始字节。
     * UTF-8 起始字节特征：
     * - 0xxxxxxx (ASCII, 1 字节)
     * - 110xxxxx (2 字节起始)
     * - 1110xxxx (3 字节起始)
     * - 11110xxx (4 字节起始)
     * 后续字节格式为 10xxxxxx
     */
    private boolean isValidUtf8Start(byte[] bytes, int index) {
        if (index >= bytes.length) return true;
        int b = bytes[index] & 0xFF;
        // 起始字节不应是 10xxxxxx (0x80-0xBF)
        return (b & 0xC0) != 0x80;
    }

    /**
     * 计算 UTF-8 字节长度。
     */
    private int getUtf8ByteLength(String str) {
        if (str == null) return 0;
        return str.getBytes(StandardCharsets.UTF_8).length;
    }
}
