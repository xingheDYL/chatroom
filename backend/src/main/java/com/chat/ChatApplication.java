package com.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 聊天室应用主入口
 *
 * 该应用提供使用WebRTC的端到端加密聊天室。
 * 功能包括：
 * - 用户认证（用户名密码和邮箱验证码）
 * - WebSocket实时通信
 * - WebRTC点对点连接，使用DTLS加密
 * - 聊天室管理
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class ChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}
