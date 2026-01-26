package com.chat.controller;

import com.chat.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * WebRTC控制器
 *
 * 为WebRTC连接提供TURN服务器凭证
 */
@RestController
@RequestMapping("/api/webrtc")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class WebRTCController {

    @Value("${webrtc.stun-url:}")
    private String stunUrlString;

    @Value("${webrtc.turn-url:}")
    private String turnUrl;

    @Value("${webrtc.turn-username:}")
    private String turnUsername;

    @Value("${webrtc.turn-credential:}")
    private String turnCredential;

    /**
     * 获取STUN URL数组
     */
    private String[] getStunUrls() {
        if (stunUrlString == null || stunUrlString.isEmpty()) {
            return new String[]{"stun:stun.l.google.com:19302"};
        }
        return stunUrlString.split(",");
    }

    /**
     * GET /api/webrtc/ice-servers
     * 获取ICE服务器配置（STUN/TURN）
     */
    @GetMapping("/ice-servers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIceServers() {
        Map<String, Object> response = new HashMap<>();

        // 添加STUN服务器
        response.put("stunServers", getStunUrls());

        // 如果配置了TURN服务器则添加
        if (turnUrl != null && !turnUrl.isEmpty()) {
            Map<String, String> turnServer = new HashMap<>();
            turnServer.put("urls", turnUrl);
            if (turnUsername != null && !turnUsername.isEmpty()) {
                turnServer.put("username", turnUsername);
            }
            if (turnCredential != null && !turnCredential.isEmpty()) {
                turnServer.put("credential", turnCredential);
            }
            response.put("turnServer", turnServer);
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/webrtc/turn-credentials
     * 生成临时TURN凭证（用于生产环境的时间限制凭证）
     */
    @PostMapping("/turn-credentials")
    public ResponseEntity<ApiResponse<Map<String, String>>> getTurnCredentials(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 在生产环境中，你应该：
        // 1. 从authHeader验证JWT
        // 2. 使用TURN服务器API生成时间限制的TURN凭证
        // 3. 返回凭证

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", turnUsername);
        credentials.put("credential", turnCredential);
        credentials.put("ttl", "86400"); // 24小时

        return ResponseEntity.ok(ApiResponse.success(credentials));
    }
}
