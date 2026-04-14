package com.paiagent.tool.handler;

import com.paiagent.tool.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TtsToolHandler implements ToolHandler {

    private final FileStorageService fileStorageService;

    @Override
    public String getToolType() {
        return "tts";
    }

    @Override
    public String execute(String input, Map<String, Object> config) {
        String voice = (String) config.getOrDefault("voice", "xiaoyun");

        log.info("TTS 合成请求: voice={}, text length={}", voice, input.length());

        // Mock TTS: In production, call actual TTS API (Aliyun/XunFei/etc.)
        // Generate a valid WAV audio file (short tone) so the audio player works
        try {
            String filename = "tts-" + System.currentTimeMillis() + ".wav";
            byte[] wavBytes = generateMockWav(2);

            String audioUrl = fileStorageService.store(filename, new ByteArrayInputStream(wavBytes));
            log.info("TTS 音频已生成 (Mock WAV): {}", audioUrl);
            return audioUrl;
        } catch (Exception e) {
            log.error("TTS 合成失败", e);
            return "[TTS Error] 音频合成失败: " + e.getMessage();
        }
    }

    /**
     * Generate a valid WAV audio file with a simple sine wave tone.
     */
    private byte[] generateMockWav(int durationSeconds) throws IOException {
        int sampleRate = 16000;
        int bitsPerSample = 16;
        int channels = 1;
        int numSamples = sampleRate * durationSeconds;
        int dataSize = numSamples * channels * (bitsPerSample / 8);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteBuffer bb = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);

        // RIFF header
        bb.put("RIFF".getBytes());
        bb.putInt(36 + dataSize);
        bb.put("WAVE".getBytes());

        // fmt chunk
        bb.put("fmt ".getBytes());
        bb.putInt(16);                    // chunk size
        bb.putShort((short) 1);           // PCM format
        bb.putShort((short) channels);
        bb.putInt(sampleRate);
        bb.putInt(sampleRate * channels * bitsPerSample / 8);  // byte rate
        bb.putShort((short) (channels * bitsPerSample / 8));   // block align
        bb.putShort((short) bitsPerSample);

        // data chunk
        bb.put("data".getBytes());
        bb.putInt(dataSize);

        // Generate a 440Hz sine wave (A4 note)
        double frequency = 440.0;
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / sampleRate;
            // Fade in/out to avoid clicks
            double envelope = 1.0;
            if (i < sampleRate / 10) {
                envelope = (double) i / (sampleRate / 10);
            } else if (i > numSamples - sampleRate / 10) {
                envelope = (double) (numSamples - i) / (sampleRate / 10);
            }
            short sample = (short) (Short.MAX_VALUE * 0.3 * envelope * Math.sin(2.0 * Math.PI * frequency * t));
            bb.putShort(sample);
        }

        return bb.array();
    }
}
