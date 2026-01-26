package com.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应DTO
 *
 * 包含成功认证后的JWT令牌和用户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;      // 访问令牌
    private String refreshToken;      // 刷新令牌
    private String tokenType = "Bearer";
    private Long expiresIn;           // 过期时间（毫秒）
    private UserDto user;             // 用户信息

    /**
     * 用户数据传输对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long id;              // 用户ID
        private String username;      // 用户名
        private String email;         // 邮箱
        private String avatar;        // 头像URL
        private String status;        // 在线状态
    }
}
