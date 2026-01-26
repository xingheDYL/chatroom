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
 * 聊天室成员实体类
 *
 * 表示用户与聊天室之间的多对多关系。
 * 记录用户何时加入聊天室。
 */
@Entity
@Table(name = "room_members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime joinedAt;

    /**
     * 成员角色枚举
     */
    @Column(length = 20)
    @Builder.Default
    private String role = "member";

    /**
     * 成员角色枚举
     */
    public enum Role {
        ADMIN,      // 管理员
        MODERATOR,  // 版主
        MEMBER      // 普通成员
    }
}
