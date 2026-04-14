package com.paiagent.tool.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class FileStorageService {

    @Value("${paiagent.file-storage.base-path:./uploads}")
    private String basePath;

    @Value("${paiagent.file-storage.url-prefix:/files}")
    private String urlPrefix;

    public String store(String filename, InputStream inputStream) throws IOException {
        Path dir = Paths.get(basePath, "audio");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        Path filePath = dir.resolve(filename);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        return urlPrefix + "/audio/" + filename;
    }
}
