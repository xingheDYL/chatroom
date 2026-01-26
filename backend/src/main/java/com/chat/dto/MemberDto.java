package com.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 房间成员DTO
 *
 * 返回房间成员的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {

    private Long id;
    private String username;
    private String avatar;
    private String role;        // admin, moderator, member
    private String status;      // online, offline, away, busy
    private Long joinedAt;      // 加入时间（时间戳）
}
