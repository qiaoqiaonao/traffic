package com.traffic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
@Slf4j
public class AiService {

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 上传视频到Python服务分析
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeVideo(MultipartFile file, Integer frameSkip) {
        try {
            // 保存临时文件
            Path tempDir = Files.createTempDirectory("traffic_");
            Path tempFile = tempDir.resolve(file.getOriginalFilename());
            file.transferTo(tempFile.toFile());

            log.info("上传视频到AI服务: {}", tempFile);

            // 构建multipart请求
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFile.toFile()));
            body.add("frame_skip", frameSkip != null ? frameSkip : 3);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            // 调用Python服务
            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServiceUrl + "/api/analyze/upload",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            // 清理临时文件
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempDir);

            Map<String, Object> result = response.getBody();
            log.info("AI服务响应: {}", result);

            return result;

        } catch (IOException e) {
            log.error("文件处理失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("调用AI服务失败", e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 查询分析结果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getResult(String taskId) {
        try {
            String url = aiServiceUrl + "/api/analyze/result/" + taskId;
            log.debug("查询任务结果: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();

        } catch (Exception e) {
            log.error("查询任务结果失败: {}", taskId, e);
            throw new RuntimeException("查询结果失败: " + e.getMessage());
        }
    }

    /**
     * 单帧图片检测（快速测试）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeFrame(MultipartFile file) {
        log.info("单张图片检测开始: {}", file.getOriginalFilename());
        try {
            Path tempDir = Files.createTempDirectory("frame_");
            Path tempFile = tempDir.resolve(file.getOriginalFilename());
            file.transferTo(tempFile.toFile());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFile.toFile()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServiceUrl + "/api/analyze/frame",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempDir);

            return response.getBody();

        } catch (Exception e) {
            log.error("单帧检测失败", e);
            throw new RuntimeException("检测失败: " + e.getMessage());
        }
    }
}