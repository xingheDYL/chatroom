package com.chat.dto;

import com.chat.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天室DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {

    private Long id;              // 房间ID
    private String name;          // 房间名称
    private String type;          // 房间类型
    private String description;   // 房间描述
    private Integer maxMembers;   // 最大成员数
    private Long memberCount;     // 当前成员数
    private Long createdBy;       // 创建者ID
    private LocalDateTime createdAt;  // 创建时间
    private String creatorName;   // 创建者名称

    /**
     * 从实体创建DTO
     */
    public static RoomDto fromEntity(ChatRoom room, Long memberCount, String creatorName) {
        return RoomDto.builder()
                .id(room.getId())
                .name(room.getName())
                .type(room.getType())
                .description(room.getDescription())
                .maxMembers(room.getMaxMembers())
                .memberCount(memberCount)
                .createdBy(room.getCreatedBy())
                .createdAt(room.getCreatedAt())
                .creatorName(creatorName)
                .build();
    }
}
