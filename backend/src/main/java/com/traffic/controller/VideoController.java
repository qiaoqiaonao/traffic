package com.traffic.controller;

import com.traffic.vo.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
@Slf4j
public class VideoController {

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${storage.temp-dir:temp/videos}")
    private String tempVideoDir;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 获取本地视频路径
     */
    private Path getLocalVideoPath(String taskId) {
        return Paths.get(tempVideoDir, taskId + "_result.mp4");
    }

    /**
     * 流式播放视频 - 支持 GET 和 HEAD
     */
    @RequestMapping(value = "/result/{taskId}/stream", method = {RequestMethod.GET, RequestMethod.HEAD})
    public void streamVideo(
            @PathVariable String taskId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            handleHeadRequest(taskId, response);
            return;
        }

        try {
            Path localPath = getLocalVideoPath(taskId);
            File localFile = localPath.toFile();

            if (!localFile.exists()) {
                downloadToLocal(taskId, localPath);
            }

            streamLocalFile(localFile, request, response);

        } catch (ClientAbortException e) {
            // ✅ 正常情况：用户刷新/关闭/切换页面，静默忽略
            log.debug("客户端主动断开视频流: taskId={}", taskId);

        } catch (IOException e) {
            // ✅ 兼容中文报错和英文 ClientAbort
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("你的主机中的软件中止了一个已建立的连接")
                    || msg.contains("software caused connection abort")
                    || msg.contains("broken pipe")
                    || msg.contains("connection reset")) {
                log.debug("客户端断开视频流: taskId={}, 原因={}", taskId, e.getMessage());
                return;
            }

            log.error("视频流IO异常: taskId={}", taskId, e);
            // 只有 response 还没提交时，才能返回错误
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "视频加载失败");
            }

        } catch (Exception e) {
            log.error("视频流处理失败: taskId={}", taskId, e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "视频加载失败");
            }
        }
    }

    /**
     * 直接下载接口
     */
    @GetMapping("/result/{taskId}")
    public void downloadVideo(@PathVariable String taskId, HttpServletResponse response) {
        try {
            Path localPath = getLocalVideoPath(taskId);

            // 没有缓存就下载
            if (!localPath.toFile().exists()) {
                downloadToLocal(taskId, localPath);
            }

            File file = localPath.toFile();

            // 设置响应头
            response.setContentType("video/mp4");
            response.setContentLengthLong(file.length());
            response.setHeader("Content-Disposition",
                    ContentDisposition.inline().filename(taskId + "_result.mp4").build().toString());
            response.setHeader("Accept-Ranges", "bytes");

            // 流式输出
            try (InputStream is = Files.newInputStream(localPath);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                os.flush();
            }

        } catch (Exception e) {
            log.error("下载视频失败: {}", taskId, e);
            response.setStatus(500);
        }
    }

    // ============ 私有方法 ============

    /**
     * 处理 HEAD 请求
     */
    private void handleHeadRequest(String taskId, HttpServletResponse response) throws IOException {
        Path localPath = getLocalVideoPath(taskId);

        if (!localPath.toFile().exists()) {
            // 尝试从 Python 获取信息（或者返回404让前端知道需要加载）
            try {
                String pythonUrl = aiServiceUrl + "/api/analyze/video/" + taskId;
                HttpHeaders headers = restTemplate.headForHeaders(pythonUrl);

                response.setContentType(headers.getContentType() != null ?
                        headers.getContentType().toString() : "video/mp4");
                if (headers.getContentLength() > 0) {
                    response.setContentLengthLong(headers.getContentLength());
                }
                response.setHeader("Accept-Ranges", "bytes");
                response.setStatus(200);
                return;
            } catch (Exception e) {
                log.warn("HEAD 请求失败，文件不存在: {}", taskId);
                response.setStatus(404);
                return;
            }
        }

        long fileSize = localPath.toFile().length();
        response.setContentType("video/mp4");
        response.setContentLengthLong(fileSize);
        response.setHeader("Accept-Ranges", "bytes");
        response.setStatus(200);
    }

    /**
     * 从 Python 服务下载视频到本地
     */
    private void downloadToLocal(String taskId, Path localPath) throws IOException {
        Files.createDirectories(localPath.getParent());

        String url = aiServiceUrl + "/api/analyze/video/" + taskId;
        log.info("下载视频到本地: {} -> {}", url, localPath);

        byte[] data = restTemplate.getForObject(url, byte[].class);
        if (data == null || data.length == 0) {
            throw new IOException("下载的视频为空");
        }

        Files.write(localPath, data);
        log.info("视频下载完成: {} bytes", data.length);
    }

    /**
     * 流式传输本地文件，支持 Range
     */
    private void streamLocalFile(File file, HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {
        long fileSize = file.length();
        String rangeHeader = request.getHeader("Range");

        long start = 0;
        long end = fileSize - 1;
        boolean isPartial = false;

        // 解析 Range
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            try {
                String range = rangeHeader.substring(6);
                String[] parts = range.split("-");

                if (parts[0].length() > 0) start = Long.parseLong(parts[0]);
                if (parts.length > 1 && parts[1].length() > 0) {
                    end = Long.parseLong(parts[1]);
                }

                start = Math.max(0, start);
                end = Math.min(fileSize - 1, end);
                isPartial = true;

            } catch (Exception e) {
                log.warn("Range 解析失败，使用完整文件");
            }
        }

        long contentLength = end - start + 1;

        // 设置响应头
        response.setContentType("video/mp4");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Length", String.valueOf(contentLength));

        if (isPartial) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range",
                    String.format("bytes %d-%d/%d", start, end, fileSize));
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }

        // 流式输出
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             OutputStream out = response.getOutputStream()) {

            raf.seek(start);
            byte[] buffer = new byte[8192];
            long remaining = contentLength;

            while (remaining > 0) {
                int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) break;

                out.write(buffer, 0, read);
                out.flush();
                remaining -= read;
            }
        }

        log.debug("视频流传输完成: {} bytes", contentLength);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> data = new HashMap<>();
        log.debug("------------------后端运行正常--------------");
        data.put("status", "up");
        data.put("service", "traffic-analysis-backend");
        data.put("version", "2.0.0");
        return ApiResponse.success(data);
    }
}