// src/main/java/com/traffic/service/FileStorageService.java
package com.traffic.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@Service
@Slf4j
public class FileStorageService {

    @Value("${storage.upload-dir}")
    private String uploadDir;

    @Value("${storage.temp-dir}")
    private String tempDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp4", "avi", "mov", "mkv");
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 1024; // 500MB

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
        Files.createDirectories(Paths.get(tempDir));
        log.info("文件存储目录初始化完成: {}", uploadDir);
    }

    public String saveFile(MultipartFile file, String taskId) throws IOException {
        // 验证文件
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过500MB限制");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new IllegalArgumentException("文件名无效");
        }

        // 安全检查
        if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
            throw new IllegalArgumentException("非法文件名");
        }

        String ext = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件格式: " + ext);
        }

        // 保存文件
        String fileName = taskId + "." + ext;
        Path filePath  = Paths.get(uploadDir).resolve(fileName);

        // 使用临时文件然后原子移动
        try {
            // 使用Files.copy代替transferTo
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件已保存: {} ({} bytes)", filePath.toAbsolutePath(), Files.size(filePath));
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new IOException("文件保存失败: " + e.getMessage(), e);
        }

        return filePath.toString();
    }

    public void deleteFile(String filePath) {
        try {
            if (filePath != null) {
                Files.deleteIfExists(Paths.get(filePath));
                log.info("文件已删除: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("删除文件失败: {}", filePath, e);
        }
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf(".");
        return lastDot == -1 ? "" : filename.substring(lastDot + 1);
    }
}