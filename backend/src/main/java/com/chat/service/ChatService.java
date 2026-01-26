package com.chat.service;

import com.chat.dto.*;
import com.chat.entity.ChatRoom;
import com.chat.entity.Message;
import com.chat.entity.RoomMember;
import com.chat.entity.User;
import com.chat.repository.ChatRoomRepository;
import com.chat.repository.MessageRepository;
import com.chat.repository.RoomMemberRepository;
import com.chat.repository.UserRepository;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天服务类
 * 处理聊天室和消息操作
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;

    /**
     * 获取当前用户的所有聊天室
     */
    public List<RoomDto> getUserRooms() {
        Long userId = getCurrentUserId();
        List<ChatRoom> rooms = chatRoomRepository.findRoomsByUserId(userId);

        return rooms.stream()
                .map(room -> {
                    Long memberCount = roomMemberRepository.countByRoomId(room.getId());
                    String creatorName = userRepository.findById(room.getCreatedBy())
                            .map(user -> user.getUsername())
                            .orElse("未知");
                    return RoomDto.fromEntity(room, memberCount, creatorName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取所有公开的聊天室（用于发现和加入）
     */
    public List<RoomDto> getAllPublicRooms() {
        List<ChatRoom> rooms = chatRoomRepository.findAll();

        return rooms.stream()
                .map(room -> {
                    Long memberCount = roomMemberRepository.countByRoomId(room.getId());
                    String creatorName = userRepository.findById(room.getCreatedBy())
                            .map(user -> user.getUsername())
                            .orElse("未知");
                    return RoomDto.fromEntity(room, memberCount, creatorName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取房间详情
     */
    public RoomDto getRoomById(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        Long memberCount = roomMemberRepository.countByRoomId(roomId);
        String creatorName = userRepository.findById(room.getCreatedBy())
                .map(user -> user.getUsername())
                .orElse("未知");

        return RoomDto.fromEntity(room, memberCount, creatorName);
    }

    /**
     * 创建新聊天室
     */
    @Transactional
    public RoomDto createRoom(CreateRoomRequest request) {
        Long userId = getCurrentUserId();

        ChatRoom room = ChatRoom.builder()
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .maxMembers(request.getMaxMembers())
                .createdBy(userId)
                .build();

        room = chatRoomRepository.save(room);

        // 将创建者添加为成员
        RoomMember member = RoomMember.builder()
                .roomId(room.getId())
                .userId(userId)
                .role("admin")
                .build();
        roomMemberRepository.save(member);

        Long memberCount = roomMemberRepository.countByRoomId(room.getId());
        String creatorName = userRepository.findById(userId)
                .map(user -> user.getUsername())
                .orElse("未知");

        return RoomDto.fromEntity(room, memberCount, creatorName);
    }

    /**
     * 创建或获取与指定用户的私聊房间
     */
    @Transactional
    public RoomDto createOrGetPrivateChat(Long targetUserId) {
        Long currentUserId = getCurrentUserId();

        if (targetUserId.equals(currentUserId)) {
            throw new RuntimeException("不能与自己私聊");
        }

        // 检查是否已经存在两个用户之间的私聊房间
        List<ChatRoom> existingRooms = chatRoomRepository.findPrivateChatBetweenUsers(currentUserId, targetUserId);

        if (!existingRooms.isEmpty()) {
            // 返回已存在的私聊房间
            ChatRoom room = existingRooms.get(0);
            Long memberCount = roomMemberRepository.countByRoomId(room.getId());
            String creatorName = userRepository.findById(room.getCreatedBy())
                    .map(user -> user.getUsername())
                    .orElse("未知");
            return RoomDto.fromEntity(room, memberCount, creatorName);
        }

        // 创建新的私聊房间
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("目标用户不存在"));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("当前用户不存在"));

        String roomName = currentUser.getUsername() + " & " + targetUser.getUsername();

        ChatRoom room = ChatRoom.builder()
                .name(roomName)
                .type("private")
                .description("私聊")
                .maxMembers(2)
                .createdBy(currentUserId)
                .build();

        room = chatRoomRepository.save(room);

        // 将两个用户都添加为成员
        RoomMember member1 = RoomMember.builder()
                .roomId(room.getId())
                .userId(currentUserId)
                .role("member")
                .build();
        roomMemberRepository.save(member1);

        RoomMember member2 = RoomMember.builder()
                .roomId(room.getId())
                .userId(targetUserId)
                .role("member")
                .build();
        roomMemberRepository.save(member2);

        Long memberCount = 2L;
        String creatorName = currentUser.getUsername();

        return RoomDto.fromEntity(room, memberCount, creatorName);
    }

    /**
     * 加入聊天室
     */
    @Transactional
    public void joinRoom(Long roomId) {
        Long userId = getCurrentUserId();

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        // 检查用户是否已是成员
        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new RuntimeException("您已经是该房间成员");
        }

        // 检查房间是否已满
        long memberCount = roomMemberRepository.countByRoomId(roomId);
        if (memberCount >= room.getMaxMembers()) {
            throw new RuntimeException("房间已满");
        }

        // 将用户添加到房间
        RoomMember member = RoomMember.builder()
                .roomId(roomId)
                .userId(userId)
                .role("member")
                .build();
        roomMemberRepository.save(member);
    }

    /**
     * 离开聊天室
     */
    @Transactional
    public void leaveRoom(Long roomId) {
        Long userId = getCurrentUserId();

        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new RuntimeException("您不是该房间成员");
        }

        roomMemberRepository.deleteByRoomIdAndUserId(roomId, userId);
    }

    /**
     * 获取房间消息
     */
    public List<MessageDto> getRoomMessages(Long roomId) {
        Long userId = getCurrentUserId();

        // 检查用户是否是成员
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new RuntimeException("您不是该房间成员");
        }

        List<Message> messages = messageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);

        return messages.stream()
                .map(message -> {
                    String senderName = userRepository.findById(message.getSenderId())
                            .map(user -> user.getUsername())
                            .orElse("未知");
                    String senderAvatar = userRepository.findById(message.getSenderId())
                            .map(user -> user.getAvatar())
                            .orElse(null);
                    return MessageDto.fromEntity(message, senderName, senderAvatar);
                })
                .collect(Collectors.toList());
    }

    /**
     * 保存消息到数据库
     */
    @Transactional
    public MessageDto saveMessage(Long roomId, String content, String messageType, Long senderId) {
        Message message = Message.builder()
                .senderId(senderId)
                .roomId(roomId)
                .content(content)
                .messageType(messageType)
                .build();

        message = messageRepository.save(message);

        String senderName = userRepository.findById(senderId)
                .map(user -> user.getUsername())
                .orElse("未知");
        String senderAvatar = userRepository.findById(senderId)
                .map(user -> user.getAvatar())
                .orElse(null);

        return MessageDto.fromEntity(message, senderName, senderAvatar);
    }

    /**
     * 获取房间成员列表（包含用户信息）
     */
    public List<com.chat.dto.MemberDto> getRoomMembers(Long roomId) {
        List<RoomMember> members = roomMemberRepository.findByRoomId(roomId);

        return members.stream()
                .map(member -> {
                    User user = userRepository.findById(member.getUserId())
                            .orElse(null);
                    if (user != null) {
                        return com.chat.dto.MemberDto.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .avatar(user.getAvatar())
                                .role(member.getRole())
                                .status(user.getStatus())
                                .joinedAt(member.getJoinedAt() != null
                                        ? member.getJoinedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                        : null)
                                .build();
                    }
                    return null;
                })
                .filter(member -> member != null)
                .collect(Collectors.toList());
    }

    /**
     * 从安全上下文获取当前用户ID
     */
    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        throw new RuntimeException("用户未认证");
    }
}
