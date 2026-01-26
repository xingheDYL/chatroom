package com.chat.dto;

import com.chat.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    private Long id;              // 消息ID
    private Long senderId;        // 发送者ID
    private String senderName;    // 发送者名称
    private String senderAvatar;  // 发送者头像
    private Long roomId;          // 房间ID
    private String content;       // 消息内容
    private String messageType;   // 消息类型
    private LocalDateTime createdAt;  // 发送时间

    /**
     * 从实体创建DTO
     */
    public static MessageDto fromEntity(Message message, String senderName, String senderAvatar) {
        return MessageDto.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .senderAvatar(senderAvatar)
                .roomId(message.getRoomId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
