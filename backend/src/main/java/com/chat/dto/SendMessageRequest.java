package com.chat.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送消息请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Builder.Default
    private String messageType = "text";  // text: 文本, image: 图片, file: 文件, system: 系统
}
