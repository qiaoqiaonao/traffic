package com.traffic.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
@Slf4j
public class PythonAiClient {

    private static final String PATH_UPLOAD = "/api/analyze/upload";
    private static final String PATH_RESULT = "/api/analyze/result/";
    private static final String PATH_FRAME  = "/api/analyze/frame";
    private static final String PATH_VIDEO  = "/api/analyze/video/";
    private static final String PATH_CANCEL = "/api/analyze/cancel/";

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public PythonAiClient(RestTemplate restTemplate,
                          @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restTemplate = restTemplate;
        this.aiServiceUrl = aiServiceUrl;
    }

    /**
     * Upload a video file for analysis (used by async flow with pre-saved file).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeVideo(Path videoFile, String taskId,
                                             Integer frameSkip, String detectionLinesJson,
                                             double metersPerPixel) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("task_id", taskId);
        body.add("file", new FileSystemResource(videoFile.toFile()));
        body.add("frame_skip", frameSkip);
        body.add("meters_per_pixel", String.valueOf(metersPerPixel));
        if (detectionLinesJson != null) {
            body.add("detection_lines", detectionLinesJson);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("Uploading video to AI service: taskId={}, file={}", taskId, videoFile);
        ResponseEntity<Map> response = restTemplate.exchange(
                aiServiceUrl + PATH_UPLOAD, HttpMethod.POST, request, Map.class);
        return response.getBody();
    }

    /**
     * Upload a video file for analysis (used by sync flow with MultipartFile).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeVideo(MultipartFile file, Integer frameSkip) throws IOException {
        Path tempDir = Files.createTempDirectory("traffic_");
        Path tempFile = tempDir.resolve(file.getOriginalFilename());
        file.transferTo(tempFile.toFile());

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFile.toFile()));
            body.add("frame_skip", frameSkip != null ? frameSkip : 3);


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServiceUrl + PATH_UPLOAD, HttpMethod.POST, request, Map.class);
            return response.getBody();
        } finally {
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempDir);
        }
    }

    /**
     * Single frame image detection.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeFrame(MultipartFile file) throws IOException {
        Path tempDir = Files.createTempDirectory("frame_");
        Path tempFile = tempDir.resolve(file.getOriginalFilename());
        file.transferTo(tempFile.toFile());

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFile.toFile()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServiceUrl + PATH_FRAME, HttpMethod.POST, request, Map.class);
            return response.getBody();
        } finally {
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempDir);
        }
    }

    /**
     * Single frame detection with ByteArrayResource (no temp file).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeFrameInline(MultipartFile file) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                aiServiceUrl + PATH_FRAME, HttpMethod.POST, request, Map.class);
        return response.getBody();
    }

    /**
     * Query analysis result from AI service.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getResult(String taskId) {
        String url = aiServiceUrl + PATH_RESULT + taskId;
        log.debug("Querying task result: {}", url);
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return response.getBody();
    }

    /**
     * Query result with retry for transient network errors.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getResultWithRetry(String taskId) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                return getResult(taskId);
            } catch (ResourceAccessException e) {
                if (i == maxRetries - 1) throw e;
                log.warn("getResult retry {}/{} for taskId={}", i + 1, maxRetries, taskId);
                try {
                    Thread.sleep((long) Math.pow(2, i) * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        return null;
    }

    /**
     * Get HTTP headers for a video result (HEAD request).
     */
    public HttpHeaders headForVideo(String taskId) {
        String url = aiServiceUrl + PATH_VIDEO + taskId;
        return restTemplate.headForHeaders(url);
    }

    /**
     * Get video URL for streaming/download.
     */
    public String getVideoUrl(String taskId) {
        return aiServiceUrl + PATH_VIDEO + taskId;
    }

    /**
     * Notify AI service to cancel a task (best-effort).
     */
    public void cancelTask(String taskId) {
        try {
            restTemplate.postForEntity(aiServiceUrl + PATH_CANCEL + taskId, null, String.class);
        } catch (Exception e) {
            log.warn("Failed to notify Python of cancellation for taskId={}: {}", taskId, e.getMessage());
        }
    }
}
