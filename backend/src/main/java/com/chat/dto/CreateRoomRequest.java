package com.chat.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建聊天室请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {

    @NotBlank(message = "房间名称不能为空")
    @Size(min = 3, max = 100, message = "房间名称长度必须在3-100个字符之间")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    @Builder.Default
    private String type = "group";  // group: 群聊, private: 私聊

    @Builder.Default
    private Integer maxMembers = 100;
}
