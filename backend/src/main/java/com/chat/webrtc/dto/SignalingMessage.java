package com.chat.webrtc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebRTC信令消息
 *
 * 用于在对等方之间交换SDP和ICE候选者
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalingMessage {

    private String type;  // offer, answer, ice-candidate, join, leave
    private String senderId;  // 发送者ID
    private String targetId;  // 可选：用于点对点信令
    private String roomId;  // 房间ID
    private Object data;  // SDP或ICE候选者数据
    private Long timestamp;  // 时间戳

    /**
     * SDP数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SdpData {
        private String sdp;  // 会话描述协议
        private String type;  // offer或answer
    }

    /**
     * ICE候选者数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IceCandidateData {
        private String candidate;  // 候选者字符串
        private String sdpMid;  // SDP媒体标识符
        private Integer sdpMLineIndex;  // SDP媒体行索引
        private String usernameFragment;  // 用户名片段
    }
}
