package com.chat.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 消息实体类
 *
 * 表示在聊天室中发送的消息。
 * 内容在存储前已加密，用于实现端到端加密。
 */
@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String content;

    @Column(name = "message_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    @Builder.Default
    private String messageType = "text";

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        TEXT,    // 文本消息
        IMAGE,   // 图片消息
        FILE,    // 文件消息
        SYSTEM   // 系统消息
    }
}
