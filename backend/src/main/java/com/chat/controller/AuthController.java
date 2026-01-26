package com.chat.controller;

import com.chat.dto.*;
import com.chat.service.AuthService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * 用户认证的REST API端点
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * 使用用户名、邮箱和密码注册新用户
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(ApiResponse.success("注册成功", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("注册失败", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/login
     * 使用用户名和密码登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("登录成功", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("登录失败", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/send-code
     * 发送邮箱验证码
     */
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(
            @Valid @RequestBody SendCodeRequest request) {
        try {
            authService.sendVerificationCode(request);
            return ResponseEntity.ok(ApiResponse.success("验证码发送成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("发送验证码失败", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/login-with-code
     * 使用邮箱验证码登录
     */
    @PostMapping("/login-with-code")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithCode(
            @Valid @RequestBody LoginWithCodeRequest request) {
        try {
            AuthResponse response = authService.loginWithCode(request);
            return ResponseEntity.ok(ApiResponse.success("登录成功", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("登录失败", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/logout
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        try {
            authService.logout(getCurrentUserId());
            return ResponseEntity.ok(ApiResponse.success("登出成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("登出失败", e.getMessage()));
        }
    }

    /**
     * 从安全上下文获取当前用户ID
     */
    private Long getCurrentUserId() {
        Object principal = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        throw new RuntimeException("用户未认证");
    }
}
