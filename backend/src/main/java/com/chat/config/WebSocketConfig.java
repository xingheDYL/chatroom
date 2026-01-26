package com.chat.config;

import com.chat.webrtc.SignalingHandler;
import com.chat.websocket.ChatHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 *
 * 配置WebRTC信令和聊天的WebSocket端点
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SignalingHandler signalingHandler;
    private final ChatHandler chatHandler;

    public WebSocketConfig(SignalingHandler signalingHandler, ChatHandler chatHandler) {
        this.signalingHandler = signalingHandler;
        this.chatHandler = chatHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册WebRTC信令处理器
        registry.addHandler(signalingHandler, "/ws/signaling/{roomId}")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://101.34.200.158",
                        "http://101.34.200.158:8901",
                        "http://chatroom.wqdyl.cloud"
                );

        // 注册聊天处理器（用于实时消息传递）
        registry.addHandler(chatHandler, "/ws/chat/{roomId}")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://101.34.200.158",
                        "http://101.34.200.158:8901",
                        "http://chatroom.wqdyl.cloud"
                );
    }
}
