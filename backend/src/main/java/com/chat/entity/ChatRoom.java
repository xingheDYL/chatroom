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
 * 聊天室实体类
 *
 * 表示系统中的聊天室。
 * 可以是群聊或私聊（1对1）类型。
 */
@Entity
@Table(name = "chat_rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String name;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String type = "group";

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(length = 500, columnDefinition = "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String description;

    @Column(name = "max_members")
    @Builder.Default
    private Integer maxMembers = 100;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * 房间类型枚举
     */
    public enum RoomType {
        GROUP,   // 群聊
        PRIVATE  // 私聊
    }
}
