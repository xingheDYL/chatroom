package com.chat.websocket;

import com.chat.dto.MessageDto;
import com.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天处理器
 *
 * 处理实时聊天消息的WebSocket连接。
 * 管理在线用户并向房间成员广播消息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    // 按房间存储会话：roomId -> Map<userId, WebSocketSession>
    private final Map<Long, Map<Long, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // 存储用户和房间信息以便快速查找
    private final Map<WebSocketSession, Long> sessionUserIds = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, Long> sessionRoomIds = new ConcurrentHashMap<>();

    // 心跳消息内容
    private static final String PING_MESSAGE = "{\"type\":\"ping\"}";
    private static final String PONG_MESSAGE = "{\"type\":\"pong\"}";

    /**
     * JDK 8兼容的Map创建辅助方法
     */
    private Map<String, Object> createMap(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    private Map<String, Object> createMap(String key1, Object value1, String key2, Object value2,
                                           String key3, Object value3, String key4, Object value4) {
        Map<String, Object> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        return map;
    }

    private Map<String, Object> createMap(String key1, Object value1, String key2, Object value2,
                                           String key3, Object value3) {
        Map<String, Object> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return map;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("=== 新的聊天WebSocket连接 ===");
        log.info("连接URI: {}", session.getUri().toString());

        Long roomId = getRoomIdFromSession(session);
        Long userId = getUserIdFromSession(session);

        if (roomId == null || userId == null) {
            log.warn("聊天连接被拒绝：URI中缺少roomId或userId");
            session.close();
            return;
        }

        // 将会话添加到房间
        roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, session);
        sessionUserIds.put(session, userId);
        sessionRoomIds.put(session, roomId);

        log.info("用户 {} 成功加入聊天室 {} 通过WebSocket", userId, roomId);

        // 发送在线用户数
        broadcastOnlineUsers(roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = sessionUserIds.get(session);
        Long roomId = sessionRoomIds.get(session);

        if (userId == null || roomId == null) {
            log.warn("消息被拒绝：会话未正确初始化");
            return;
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            // 处理心跳消息
            if ("ping".equals(type)) {
                log.debug("收到来自用户 {} 的心跳 ping", userId);
                session.sendMessage(new TextMessage(PONG_MESSAGE));
                return;
            } else if ("pong".equals(type)) {
                // 客户端响应了我们的心跳，连接正常
                log.debug("收到来自用户 {} 的心跳 pong", userId);
                return;
            }

            if ("message".equals(type)) {
                String content = (String) payload.get("content");
                String messageType = (String) payload.getOrDefault("messageType", "text");

                // 将消息保存到数据库
                MessageDto messageDto = chatService.saveMessage(roomId, content, messageType, userId);

                // 不广播消息，因为：
                // 1. 发送者通过 WebRTC P2P 直接发送给其他用户
                // 2. 其他用户通过 WebRTC P2P 接收
                // 3. WebSocket 仅用于消息存储，不进行广播
                // broadcastToRoom(roomId, messageDto, session);
            } else if ("typing".equals(type)) {
                // 广播正在输入指示器
                broadcastToRoom(roomId, payload, session);
            } else if ("stop-typing".equals(type)) {
                // 广播停止输入指示器
                broadcastToRoom(roomId, payload, session);
            }

        } catch (Exception e) {
            log.error("处理聊天消息错误: {}", e.getMessage(), e);
            sendError(session, "消息格式无效");
        }
    }

    /**
     * 定时发送心跳消息到所有活跃的WebSocket连接
     * 每30秒发送一次，防止连接因空闲超时而被断开
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        int count = 0;
        for (WebSocketSession session : sessionUserIds.keySet()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(PING_MESSAGE));
                    count++;
                } catch (IOException e) {
                    log.warn("发送心跳失败: {}", e.getMessage());
                }
            }
        }
        if (count > 0) {
            log.debug("已向 {} 个活跃的聊天WebSocket连接发送心跳", count);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        handleUserLeave(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误，session: {}", session.getId(), exception);
        handleUserLeave(session);
    }

    /**
     * 处理用户离开房间
     */
    private void handleUserLeave(WebSocketSession session) throws IOException {
        Long userId = sessionUserIds.get(session);
        Long roomId = sessionRoomIds.get(session);

        if (userId == null || roomId == null) {
            return;
        }

        // 从房间中移除会话
        Map<Long, WebSocketSession> room = roomSessions.get(roomId);
        if (room != null) {
            room.remove(userId);
            if (room.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }

        sessionUserIds.remove(session);
        sessionRoomIds.remove(session);

        log.info("用户 {} 离开聊天室 {}", userId, roomId);

        // 发送更新的在线用户数
        broadcastOnlineUsers(roomId);
    }

    /**
     * 向房间内的所有用户广播消息
     */
    private void broadcastToRoom(Long roomId, MessageDto messageDto, WebSocketSession excludeSession) throws IOException {
        Map<Long, WebSocketSession> room = roomSessions.get(roomId);
        if (room == null) {
            return;
        }

        String jsonMessage = objectMapper.writeValueAsString(createMap(
                "type", "message",
                "data", messageDto
        ));

        for (WebSocketSession targetSession : room.values()) {
            if (targetSession.isOpen() && targetSession != excludeSession) {
                targetSession.sendMessage(new TextMessage(jsonMessage));
            }
        }
    }

    /**
     * 向房间广播输入指示器（除发送者外）
     */
    private void broadcastToRoom(Long roomId, Map<String, Object> payload, WebSocketSession excludeSession) throws IOException {
        Map<Long, WebSocketSession> room = roomSessions.get(roomId);
        if (room == null) {
            return;
        }

        String jsonMessage = objectMapper.writeValueAsString(payload);

        for (WebSocketSession targetSession : room.values()) {
            if (targetSession.isOpen() && targetSession != excludeSession) {
                targetSession.sendMessage(new TextMessage(jsonMessage));
            }
        }
    }

    /**
     * 向房间广播在线用户数
     */
    private void broadcastOnlineUsers(Long roomId) throws IOException {
        Map<Long, WebSocketSession> room = roomSessions.get(roomId);
        if (room == null) {
            return;
        }

        String jsonMessage = objectMapper.writeValueAsString(createMap(
                "type", "online-users",
                "count", room.size(),
                "roomId", roomId
        ));

        for (WebSocketSession targetSession : room.values()) {
            if (targetSession.isOpen()) {
                targetSession.sendMessage(new TextMessage(jsonMessage));
            }
        }
    }

    /**
     * 向客户端发送错误消息
     */
    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        String jsonMessage = objectMapper.writeValueAsString(createMap(
                "type", "error",
                "message", errorMessage
        ));
        session.sendMessage(new TextMessage(jsonMessage));
    }

    /**
     * 从会话URI中提取房间ID
     */
    private Long getRoomIdFromSession(WebSocketSession session) {
        try {
            String uri = session.getUri().toString();
            log.debug("解析URI获取房间ID: {}", uri);

            // 移除查询参数部分
            String path = uri.contains("?") ? uri.substring(0, uri.indexOf("?")) : uri;

            String[] parts = path.split("/");
            if (parts.length == 0) {
                log.warn("URI解析后路径为空");
                return null;
            }

            String roomIdStr = parts[parts.length - 1];
            if (roomIdStr != null && !roomIdStr.trim().isEmpty()) {
                Long roomId = Long.parseLong(roomIdStr);
                log.debug("成功解析房间ID: {}", roomId);
                return roomId;
            }

            log.warn("房间ID为空");
            return null;
        } catch (NumberFormatException e) {
            log.error("房间ID格式错误", e);
            return null;
        }
    }

    /**
     * 从查询参数中提取用户ID
     */
    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            String uri = session.getUri().toString();
            log.debug("解析URI获取用户ID: {}", uri);

            String query = uri.contains("?") ? uri.substring(uri.indexOf("?") + 1) : "";
            if (query.isEmpty()) {
                log.warn("查询参数为空");
                return null;
            }

            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length >= 2 && "userId".equals(keyValue[0])) {
                    String userIdStr = keyValue[1];
                    if (userIdStr != null && !userIdStr.trim().isEmpty()) {
                        Long userId = Long.parseLong(userIdStr);
                        log.debug("成功解析用户ID: {}", userId);
                        return userId;
                    }
                }
            }

            log.warn("未找到userId参数");
            return null;
        } catch (NumberFormatException e) {
            log.error("用户ID格式错误", e);
            return null;
        }
    }

    /**
     * 获取房间的在线用户数
     */
    public int getRoomUserCount(Long roomId) {
        Map<Long, WebSocketSession> room = roomSessions.get(roomId);
        return room == null ? 0 : room.size();
    }
}
