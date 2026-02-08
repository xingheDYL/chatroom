/**
 * WebRTC Signaling Client
 *
 * Handles WebSocket signaling for WebRTC peer connections.
 * Manages SDP offer/answer exchange and ICE candidate exchange.
 */
export class SignalingClient {
  constructor(config = {}) {
    this.ws = null
    this.roomId = config.roomId || null
    this.userId = config.userId || null
    this.serverUrl = config.serverUrl || null

    // Callbacks
    this.onOpen = config.onOpen || (() => {})
    this.onClose = config.onClose || (() => {})
    this.onError = config.onError || (() => {})
    this.onMessage = config.onMessage || (() => {})
    this.onUserJoin = config.onUserJoin || (() => {})
    this.onUserLeave = config.onUserLeave || (() => {})
    this.onUserList = config.onUserList || (() => {})
    this.onOffer = config.onOffer || (() => {})
    this.onAnswer = config.onAnswer || (() => {})
    this.onIceCandidate = config.onIceCandidate || (() => {})
  }

  /**
   * Get WebSocket URL based on environment
   */
  getWebSocketUrl(path) {
    // 生产环境使用环境变量配置的地址，开发环境使用当前页面主机名
    let wsBaseUrl
    if (process.env.NODE_ENV === 'production' && process.env.VUE_APP_WS_BASE_URL) {
      wsBaseUrl = process.env.VUE_APP_WS_BASE_URL
    } else if (process.env.NODE_ENV === 'production') {
      // 生产环境但未配置环境变量：使用当前页面的协议和域名
      // 这样会通过 ws://域名/ws 的方式连接（如果开放了 8902 端口）
      // 或者通过 Nginx 代理 /ws/ 到后端 8902 端口
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const hostname = window.location.hostname
      const port = window.location.port || (window.location.protocol === 'https:' ? '443' : '80')
      // 如果是标准端口（80或443），不包含端口号
      if ((window.location.protocol === 'http:' && port === '80') ||
          (window.location.protocol === 'https:' && port === '443')) {
        wsBaseUrl = `${protocol}//${hostname}`
      } else {
        wsBaseUrl = `${protocol}//${hostname}:${port}`
      }
    } else {
      // 开发环境：使用当前页面的主机名和端口 8902
      const protocol = 'ws'
      const hostname = window.location.hostname
      wsBaseUrl = `${protocol}://${hostname}:8902`
    }

    return `${wsBaseUrl}/${path}`
  }

  /**
   * Connect to the signaling server
   */
  connect(roomId, userId) {
    this.roomId = roomId
    this.userId = userId

    // Build WebSocket URL with query parameters
    const url = this.getWebSocketUrl(`ws/signaling/${roomId}?userId=${userId}`)

    console.log('=== 正在连接信令服务器 ===')
    console.log('房间ID:', roomId)
    console.log('用户ID:', userId)
    console.log('完整URL:', url)
    console.log('用户ID类型:', typeof userId)

    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      console.log('Signaling WebSocket connected')
      this.onOpen()
    }

    this.ws.onclose = (event) => {
      console.log('Signaling WebSocket disconnected:', event.code, event.reason)
      this.onClose(event)
    }

    this.ws.onerror = (error) => {
      console.error('Signaling WebSocket error:', error)
      this.onError(error)
    }

    this.ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        this.handleMessage(message)
      } catch (error) {
        console.error('Failed to parse signaling message:', error)
      }
    }
  }

  /**
   * Handle incoming signaling messages
   */
  handleMessage(message) {
    console.log('Received signaling message:', message.type, message)

    switch (message.type) {
      case 'ping':
        // 响应心跳消息
        this.send({ type: 'pong', timestamp: Date.now() })
        break

      case 'pong':
        // 心跳响应，连接正常
        console.debug('收到心跳响应')
        break

      case 'join':
        this.onUserJoin(message.senderId)
        break

      case 'leave':
        this.onUserLeave(message.data)
        break

      case 'user-list':
        this.onUserList(message.data)
        break

      case 'offer':
        this.onOffer(message.senderId, message.data)
        break

      case 'answer':
        this.onAnswer(message.senderId, message.data)
        break

      case 'ice-candidate':
        this.onIceCandidate(message.senderId, message.data)
        break

      case 'error':
        console.error('Signaling server error:', message.data)
        this.onError(new Error(message.data))
        break

      default:
        console.warn('Unknown signaling message type:', message.type)
    }
  }

  /**
   * Send an offer to a peer (or broadcast to room)
   */
  sendOffer(targetId, offer) {
    const message = {
      type: 'offer',
      senderId: this.userId,
      targetId: targetId || null,
      roomId: this.roomId,
      data: {
        sdp: offer.sdp,
        type: offer.type
      },
      timestamp: Date.now()
    }
    this.send(message)
  }

  /**
   * Send an answer to a peer
   */
  sendAnswer(targetId, answer) {
    const message = {
      type: 'answer',
      senderId: this.userId,
      targetId: targetId,
      roomId: this.roomId,
      data: {
        sdp: answer.sdp,
        type: answer.type
      },
      timestamp: Date.now()
    }
    this.send(message)
  }

  /**
   * Send an ICE candidate to a peer
   */
  sendIceCandidate(targetId, candidate) {
    console.log('=== 发送 ICE 候选者 ===')
    console.log('目标用户:', targetId)
    console.log('当前用户ID (this.userId):', this.userId)
    console.log('房间ID:', this.roomId)
    console.log('WebSocket 状态:', this.ws?.readyState)

    const message = {
      type: 'ice-candidate',
      senderId: this.userId,
      targetId: targetId,
      roomId: this.roomId,
      data: {
        candidate: candidate.candidate,
        sdpMid: candidate.sdpMid,
        sdpMLineIndex: candidate.sdpMLineIndex,
        usernameFragment: candidate.usernameFragment
      },
      timestamp: Date.now()
    }

    console.log('发送的消息:', message)
    this.send(message)
  }

  /**
   * Send a leave message
   */
  sendLeave() {
    const message = {
      type: 'leave',
      senderId: this.userId,
      roomId: this.roomId,
      timestamp: Date.now()
    }
    this.send(message)
  }

  /**
   * Send a message through the WebSocket
   */
  send(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const json = JSON.stringify(message)
      console.log('>>> 通过 WebSocket 发送消息:', message.type)
      this.ws.send(json)
    } else {
      console.error('WebSocket 未连接，无法发送消息')
    }
  }

  /**
   * Disconnect from the signaling server
   */
  disconnect() {
    if (this.ws) {
      this.sendLeave()
      this.ws.close()
      this.ws = null
    }
  }

  /**
   * Check if connected
   */
  isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }
}
