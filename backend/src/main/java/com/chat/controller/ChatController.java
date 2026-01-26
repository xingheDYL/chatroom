package com.chat.controller;

import com.chat.dto.*;
import com.chat.service.ChatService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天控制器
 * <p>
 * 聊天室管理的REST API端点
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ChatController {

    private final ChatService chatService;

    /**
     * GET /api/rooms
     * 获取当前用户的所有聊天室
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomDto>>> getUserRooms() {
        try {
            List<RoomDto> rooms = chatService.getUserRooms();
            return ResponseEntity.ok(ApiResponse.success(rooms));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取聊天室失败", e.getMessage()));
        }
    }

    /**
     * GET /api/rooms/all
     * 获取所有公开的聊天室（用于发现和加入）
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getAllPublicRooms() {
        try {
            List<RoomDto> rooms = chatService.getAllPublicRooms();
            return ResponseEntity.ok(ApiResponse.success(rooms));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取聊天室失败", e.getMessage()));
        }
    }

    /**
     * GET /api/rooms/{id}
     * 根据ID获取房间详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomDto>> getRoomById(@PathVariable Long id) {
        try {
            RoomDto room = chatService.getRoomById(id);
            return ResponseEntity.ok(ApiResponse.success(room));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取房间失败", e.getMessage()));
        }
    }

    /**
     * POST /api/rooms
     * 创建新聊天室
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RoomDto>> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        try {
            RoomDto room = chatService.createRoom(request);
            return ResponseEntity.ok(ApiResponse.success("创建聊天室成功", room));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("创建聊天室失败", e.getMessage()));
        }
    }

    /**
     * POST /api/rooms/{id}/join
     * 加入聊天室
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<Void>> joinRoom(@PathVariable Long id) {
        try {
            chatService.joinRoom(id);
            return ResponseEntity.ok(ApiResponse.success("加入聊天室成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("加入聊天室失败", e.getMessage()));
        }
    }

    /**
     * POST /api/rooms/{id}/leave
     * 离开聊天室
     */
    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(@PathVariable Long id) {
        try {
            chatService.leaveRoom(id);
            return ResponseEntity.ok(ApiResponse.success("离开聊天室成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("离开聊天室失败", e.getMessage()));
        }
    }

    /**
     * GET /api/rooms/{id}/messages
     * 获取聊天室消息
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getRoomMessages(@PathVariable Long id) {
        try {
            List<MessageDto> messages = chatService.getRoomMessages(id);
            return ResponseEntity.ok(ApiResponse.success(messages));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取消息失败", e.getMessage()));
        }
    }

    /**
     * POST /api/rooms/{id}/messages
     * 发送消息到聊天室（通过REST API，也可通过WebSocket）
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        try {
            request.setRoomId(id);
            // 注意：在实际实现中，你应该从安全上下文获取发送者ID
            // 这是一个简化版本
            MessageDto message = chatService.saveMessage(
                    id,
                    request.getContent(),
                    request.getMessageType(),
                    1L // 占位符 - 应从安全上下文获取
            );
            return ResponseEntity.ok(ApiResponse.success("发送消息成功", message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("发送消息失败", e.getMessage()));
        }
    }

    /**
     * GET /api/rooms/{id}/members
     * 获取聊天室成员列表
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<com.chat.dto.MemberDto>>> getRoomMembers(@PathVariable Long id) {
        try {
            List<com.chat.dto.MemberDto> members = chatService.getRoomMembers(id);
            return ResponseEntity.ok(ApiResponse.success(members));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取成员失败", e.getMessage()));
        }
    }

    /**
     * POST /api/rooms/private/{userId}
     * 创建或获取与指定用户的私聊房间
     */
    @PostMapping("/private/{userId}")
    public ResponseEntity<ApiResponse<RoomDto>> createOrGetPrivateChat(@PathVariable Long userId) {
        try {
            RoomDto room = chatService.createOrGetPrivateChat(userId);
            return ResponseEntity.ok(ApiResponse.success("获取私聊房间成功", room));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取私聊房间失败", e.getMessage()));
        }
    }
}
