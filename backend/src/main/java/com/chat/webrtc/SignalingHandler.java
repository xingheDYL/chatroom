package com.chat.webrtc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chat.webrtc.dto.SignalingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC信令处理器
 *
 * 处理WebRTC信令的WebSocket连接。
 * 管理对等连接、SDP交换和ICE候选者交换。
 */
@Slf4j
@Component
public class SignalingHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 按房间存储会话：roomId -> Map<userId, WebSocketSession>
    private final Map<String, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // 在会话中存储用户ID以便快速查找
    private final Map<WebSocketSession, String> sessionUserIds = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionRoomIds = new ConcurrentHashMap<>();

    // 心跳消息内容
    private static final String PING_MESSAGE = "{\"type\":\"ping\"}";
    private static final String PONG_MESSAGE = "{\"type\":\"pong\"}";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String uri = session.getUri().toString();
        log.info("=== 新的WebSocket连接 ===");
        log.info("连接URI: {}", uri);

        String roomId = getRoomIdFromSession(session);
        String userId = getUserIdFromSession(session);

        log.info("解析出的房间ID: {}", roomId);
        log.info("解析出的用户ID: {}", userId);

        if (roomId == null || userId == null) {
            log.warn("连接被拒绝：URI中缺少roomId或userId");
            session.close();
            return;
        }

        // 将会话添加到房间
        roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, session);
        sessionUserIds.put(session, userId);
        sessionRoomIds.put(session, roomId);

        log.info("用户 {} 成功加入房间 {}", userId, roomId);
        log.info("房间 {} 当前用户数: {}", roomId, roomSessions.get(roomId).size());

        // 通知房间内的其他用户
        broadcastToRoom(roomId, userId, SignalingMessage.builder()
                .type("join")
                .senderId(userId)
                .targetId(null)
                .roomId(roomId)
                .data(userId)
                .timestamp(System.currentTimeMillis())
                .build(), session);

        // 向新用户发送当前用户列表
        sendUserList(session, roomId, userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = sessionUserIds.get(session);
        String roomId = sessionRoomIds.get(session);

        if (userId == null || roomId == null) {
            log.warn("消息被拒绝：会话未正确初始化");
            return;
        }

        try {
            // 首先检查是否是心跳消息（JSON格式简单解析）
            String payload = message.getPayload();
            if (payload.contains("\"type\"") && payload.contains("ping")) {
                log.debug("收到来自用户 {} 的心跳 ping", userId);
                session.sendMessage(new TextMessage(PONG_MESSAGE));
                return;
            }
            if (payload.contains("\"type\"") && payload.contains("pong")) {
                log.debug("收到来自用户 {} 的心跳 pong", userId);
                return;
            }

            SignalingMessage signalingMessage = objectMapper.readValue(
                    payload, SignalingMessage.class);

            // 如果未提供则设置发送者信息
            if (signalingMessage.getSenderId() == null) {
                signalingMessage.setSenderId(userId);
            }
            if (signalingMessage.getRoomId() == null) {
                signalingMessage.setRoomId(roomId);
            }
            if (signalingMessage.getTimestamp() == null) {
                signalingMessage.setTimestamp(System.currentTimeMillis());
            }

            log.debug("收到信令消息: type={}, from={}, room={}",
                    signalingMessage.getType(), userId, roomId);

            switch (signalingMessage.getType()) {
                case "offer":
                case "answer":
                case "ice-candidate":
                    log.info("=== 转发信令消息 ===");
                    log.info("消息类型: {}", signalingMessage.getType());
                    log.info("发送者ID: {} (类型: {})", signalingMessage.getSenderId(),
                            signalingMessage.getSenderId() != null ? signalingMessage.getSenderId().getClass() : "null");
                    log.info("目标ID: {} (类型: {})", signalingMessage.getTargetId(),
                            signalingMessage.getTargetId() != null ? signalingMessage.getTargetId().getClass() : "null");
                    log.info("房间ID: {}", roomId);

                    // 确保ID是字符串类型
                    String senderId = signalingMessage.getSenderId() != null
                            ? String.valueOf(signalingMessage.getSenderId()) : userId;
                    String targetId = signalingMessage.getTargetId() != null
                            ? String.valueOf(signalingMessage.getTargetId()) : null;

                    // 如果指定了目标用户则转发给目标，否则广播
                    if (targetId != null) {
                        log.info("尝试发送给用户: {}", targetId);
                        sendToUser(roomId, targetId, signalingMessage);
                    } else {
                        log.info("广播到房间（排除发送者）");
                        broadcastToRoom(roomId, senderId, signalingMessage, session);
                    }
                    break;

                case "leave":
                    handleUserLeave(session);
                    break;

                default:
                    log.warn("未知的信令消息类型: {}", signalingMessage.getType());
            }

        } catch (Exception e) {
            log.error("处理信令消息错误: {}", e.getMessage(), e);
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
            log.debug("已向 {} 个活跃的信令WebSocket连接发送心跳", count);
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
        String userId = sessionUserIds.get(session);
        String roomId = sessionRoomIds.get(session);

        if (userId == null || roomId == null) {
            return;
        }

        // 从房间中移除会话
        Map<String, WebSocketSession> room = roomSessions.get(roomId);
        if (room != null) {
            room.remove(userId);
            if (room.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }

        sessionUserIds.remove(session);
        sessionRoomIds.remove(session);

        log.info("用户 {} 离开房间 {}", userId, roomId);

        // 通知其他用户
        broadcastToRoom(roomId, userId, SignalingMessage.builder()
                .type("leave")
                .senderId(userId)
                .roomId(roomId)
                .data(userId)
                .timestamp(System.currentTimeMillis())
                .build(), null);
    }

    /**
     * 向房间内的所有用户广播消息（除发送者外）
     */
    private void broadcastToRoom(String roomId, String excludeUserId, SignalingMessage message,
                                 WebSocketSession excludeSession) throws IOException {
        Map<String, WebSocketSession> room = roomSessions.get(roomId);
        if (room == null) {
            return;
        }

        String jsonMessage = objectMapper.writeValueAsString(message);

        for (Map.Entry<String, WebSocketSession> entry : room.entrySet()) {
            WebSocketSession targetSession = entry.getValue();
            if (targetSession.isOpen() && targetSession != excludeSession) {
                targetSession.sendMessage(new TextMessage(jsonMessage));
            }
        }
    }

    /**
     * 向房间内的特定用户发送消息
     */
    private void sendToUser(String roomId, String targetUserId, SignalingMessage message) throws IOException {
        Map<String, WebSocketSession> room = roomSessions.get(roomId);
        if (room == null) {
            log.warn("房间 {} 不存在", roomId);
            return;
        }

        log.info("房间 {} 当前用户列表: {}", roomId, room.keySet());

        WebSocketSession targetSession = room.get(targetUserId);
        if (targetSession != null && targetSession.isOpen()) {
            String jsonMessage = objectMapper.writeValueAsString(message);
            targetSession.sendMessage(new TextMessage(jsonMessage));
            log.info("成功发送消息给用户 {}", targetUserId);
        } else {
            log.warn("目标用户 {} 在房间 {} 中未找到或会话已关闭", targetUserId, roomId);
            log.warn("查找的用户ID类型: {}", targetUserId.getClass().getName());
        }
    }

    /**
     * 向刚加入的用户发送当前房间内的用户列表
     */
    private void sendUserList(WebSocketSession session, String roomId, String newUserId) throws IOException {
        Map<String, WebSocketSession> room = roomSessions.get(roomId);
        if (room == null) {
            return;
        }

        for (String userId : room.keySet()) {
            if (!userId.equals(newUserId)) {
                SignalingMessage message = SignalingMessage.builder()
                        .type("user-list")
                        .senderId(userId)
                        .roomId(roomId)
                        .data(userId)
                        .timestamp(System.currentTimeMillis())
                        .build();

                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            }
        }
    }

    /**
     * 向客户端发送错误消息
     */
    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        SignalingMessage message = SignalingMessage.builder()
                .type("error")
                .data(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        String jsonMessage = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(jsonMessage));
    }

    /**
     * 从会话URI中提取房间ID
     */
    private String getRoomIdFromSession(WebSocketSession session) {
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

            String roomId = parts[parts.length - 1];
            if (roomId != null && !roomId.trim().isEmpty()) {
                log.debug("成功解析房间ID: {}", roomId);
                return roomId;
            }

            log.warn("房间ID为空");
            return null;
        } catch (Exception e) {
            log.error("解析房间ID时发生错误", e);
            return null;
        }
    }

    /**
     * 从查询参数中提取用户ID
     */
    private String getUserIdFromSession(WebSocketSession session) {
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
                    String userId = keyValue[1];
                    if (userId != null && !userId.trim().isEmpty()) {
                        log.debug("成功解析用户ID: {}", userId);
                        return userId;
                    }
                }
            }

            log.warn("未找到userId参数");
            return null;
        } catch (Exception e) {
            log.error("解析用户ID时发生错误", e);
            return null;
        }
    }

    /**
     * 获取房间的在线用户数
     */
    public int getRoomUserCount(String roomId) {
        Map<String, WebSocketSession> room = roomSessions.get(roomId);
        return room == null ? 0 : room.size();
    }
}
