package com.chat.controller;

import com.chat.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件控制器
 *
 * 文件上传和下载的REST API端点
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class FileController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * POST /api/files/upload
     * 上传文件
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文件不能为空", null));
            }

            // 验证文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文件大小不能超过10MB", null));
            }

            // 创建上传目录
            File uploadDirectory = new File(uploadDir);
            if (!uploadDirectory.exists()) {
                uploadDirectory.mkdirs();
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = Paths.get(uploadDir, uniqueFilename);
            Files.copy(file.getInputStream(), filePath);

            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("filename", uniqueFilename);
            result.put("originalFilename", originalFilename);
            result.put("size", file.getSize());
            result.put("url", "/files/" + uniqueFilename);

            return ResponseEntity.ok(ApiResponse.success("文件上传成功", result));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文件上传失败", e.getMessage()));
        }
    }

    /**
     * GET /api/files/{filename}
     * 获取文件
     */
    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir, filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(filePath);

            // 根据文件扩展名设置Content-Type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                    .header("Content-Type", contentType)
                    .body(fileContent);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
