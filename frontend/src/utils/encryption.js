/**
 * 加密工具类 - WebRTC
 *
 * 注意：WebRTC 为所有数据通道提供内置的 DTLS 加密。
 * 此模块提供额外的消息格式化和验证工具。
 *
 * DTLS（数据报传输层安全）在 WebRTC 中是强制性的，提供：
 * - 所有发送数据的加密
 * - 对等方的身份验证
 * - 防篡改的完整性保护
 *
 * 通过 WebRTC 数据通道发送的消息无需额外加密。
 */

/**
 * 加密聊天的消息类型
 */
export const MessageType = {
  TEXT: 'text',
  IMAGE: 'image',
  FILE: 'file',
  SYSTEM: 'system',
  TYPING: 'typing',
  STOP_TYPING: 'stop-typing'
}

/**
 * 创建聊天消息对象
 */
export function createMessage(type, content, metadata = {}) {
  return {
    type,
    content,
    metadata,
    timestamp: Date.now(),
    id: generateMessageId()
  }
}

/**
 * 生成唯一的消息ID
 */
function generateMessageId() {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
}

/**
 * 验证消息结构
 */
export function validateMessage(message) {
  return (
    message &&
    typeof message === 'object' &&
    typeof message.type === 'string' &&
    message.hasOwnProperty('content') &&
    typeof message.timestamp === 'number'
  )
}

/**
 * 序列化消息用于通过 WebRTC 数据通道发送
 */
export function serializeMessage(message) {
  try {
    return JSON.stringify(message)
  } catch (error) {
    console.error('消息序列化失败:', error)
    return null
  }
}

/**
 * 反序列化从 WebRTC 数据通道接收的消息
 */
export function deserializeMessage(data) {
  try {
    return JSON.parse(data)
  } catch (error) {
    console.error('消息反序列化失败:', error)
    return null
  }
}

/**
 * 创建正在输入指示器消息
 */
export function createTypingIndicator() {
  return createMessage(MessageType.TYPING, null)
}

/**
 * 创建停止输入指示器消息
 */
export function createStopTypingIndicator() {
  return createMessage(MessageType.STOP_TYPING, null)
}

/**
 * 检查消息是否为正在输入指示器
 */
export function isTypingIndicator(message) {
  return message.type === MessageType.TYPING
}

/**
 * 检查消息是否为停止输入指示器
 */
export function isStopTypingIndicator(message) {
  return message.type === MessageType.STOP_TYPING
}

/**
 * 格式化时间戳用于显示
 */
export function formatTimestamp(timestamp) {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  // 少于1分钟
  if (diff < 60000) {
    return '刚刚'
  }

  // 少于1小时
  if (diff < 3600000) {
    const minutes = Math.floor(diff / 60000)
    return `${minutes}分钟前`
  }

  // 少于1天
  if (diff < 86400000) {
    const hours = Math.floor(diff / 3600000)
    return `${hours}小时前`
  }

  // 同一年
  if (date.getFullYear() === now.getFullYear()) {
    return date.toLocaleDateString('zh-CN', {
      month: '2-digit',
      day: '2-digit'
    }) + ' ' + date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  // 显示完整日期
  return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 截断文本用于预览
 */
export function truncateText(text, maxLength = 50) {
  if (text.length <= maxLength) {
    return text
  }
  return text.substring(0, maxLength) + '...'
}

/**
 * 检查 WebRTC 加密是否可用（DTLS 始终启用）
 */
export function isWebRTCEncryptionAvailable() {
  return (
    typeof RTCPeerConnection !== 'undefined' &&
    typeof RTCDataChannel !== 'undefined'
  )
}

/**
 * 获取 WebRTC 加密信息
 */
export function getWebRTCEncryptionInfo() {
  return {
    enabled: true,
    protocol: 'DTLS-SRTP',
    description:
      'WebRTC 使用 DTLS（数据报传输层安全）加密所有数据通道通信。这是强制性的，无法禁用。',
    keyExchange: 'Diffie-Hellman',
    cipherSuites: 'AES-128/256-GCM',
    note: '消息端到端加密，服务器或中间网络无法拦截。'
  }
}
