/**
 * WebRTC Manager
 *
 * Manages peer-to-peer connections using WebRTC with DTLS encryption.
 * Handles SDP offer/answer exchange and ICE candidate gathering.
 */
export class WebRTCManager {
  constructor(config = {}) {
    this.peerConnections = new Map() // peerId -> RTCPeerConnection
    this.dataChannels = new Map() // peerId -> RTCDataChannel
    this.localUserId = config.localUserId || null
    this.roomId = config.roomId || null
    this.iceServers = config.iceServers || [
      { urls: 'stun:stun.l.google.com:19302' }
    ]

    // Callbacks
    this.onDataChannelMessage = config.onDataChannelMessage || (() => {})
    this.onDataChannelOpen = config.onDataChannelOpen || (() => {})
    this.onDataChannelClose = config.onDataChannelClose || (() => {})
    this.onIceCandidate = config.onIceCandidate || (() => {})
    this.onConnectionStateChange = config.onConnectionStateChange || (() => {})
  }

  /**
   * Create a new RTCPeerConnection for a peer
   */
  createPeerConnection(peerId) {
    const config = {
      iceServers: this.iceServers
    }

    // Create RTCPeerConnection with DTLS enabled by default
    const pc = new RTCPeerConnection(config)

    // Store the peer connection
    this.peerConnections.set(peerId, pc)

    // Handle ICE candidates
    pc.onicecandidate = (event) => {
      if (event.candidate) {
        console.log(`=== ICE 候选者生成 ===`)
        console.log(`对等方: ${peerId}`)
        console.log(`候选者:`, event.candidate.candidate)
        this.onIceCandidate(peerId, event.candidate)
      } else {
        console.log(`=== ICE 候选者收集完成 ===`)
        console.log(`对等方: ${peerId}`)
      }
    }

    // Handle connection state changes
    pc.onconnectionstatechange = () => {
      console.log(`=== 连接状态变化 ===`)
      console.log(`对等方: ${peerId}`)
      console.log(`新状态: ${pc.connectionState}`)
      this.onConnectionStateChange(peerId, pc.connectionState)

      if (pc.connectionState === 'disconnected' || pc.connectionState === 'failed') {
        this.closePeerConnection(peerId)
      } else if (pc.connectionState === 'connected') {
        console.log(`✅ P2P 连接成功建立！`)
      }
    }

    // Handle ICE connection state changes
    pc.oniceconnectionstatechange = () => {
      console.log(`=== ICE 连接状态变化 ===`)
      console.log(`对等方: ${peerId}`)
      console.log(`ICE 状态: ${pc.iceConnectionState}`)
    }

    // Handle incoming data channels (for answering peer)
    pc.ondatachannel = (event) => {
      const dataChannel = event.channel
      this.setupDataChannel(peerId, dataChannel)
    }

    return pc
  }

  /**
   * Set up a data channel for a peer
   */
  setupDataChannel(peerId, dataChannel) {
    // Store the data channel
    this.dataChannels.set(peerId, dataChannel)

    console.log(`=== 设置数据通道 ===`)
    console.log(`对等方: ${peerId}`)
    console.log(`数据通道标签: ${dataChannel.label}`)
    console.log(`当前状态: ${dataChannel.readyState}`)

    // Set up event handlers
    dataChannel.onopen = () => {
      console.log(`=== 数据通道已打开 ===`)
      console.log(`对等方: ${peerId}`)
      this.onDataChannelOpen(peerId)
    }

    dataChannel.onclose = () => {
      console.log(`=== 数据通道已关闭 ===`)
      console.log(`对等方: ${peerId}`)
      this.onDataChannelClose(peerId)
      this.dataChannels.delete(peerId)
    }

    dataChannel.onmessage = (event) => {
      this.onDataChannelMessage(peerId, event.data)
    }

    dataChannel.onerror = (error) => {
      console.error(`=== 数据通道错误 ===`)
      console.error(`对等方: ${peerId}`)
      console.error(`错误:`, error)
    }
  }

  /**
   * Create an offer to initiate a connection
   */
  async createOffer(peerId) {
    console.log(`=== 创建 Offer ===`)
    console.log(`对等方: ${peerId}`)

    let pc = this.peerConnections.get(peerId)
    if (!pc) {
      pc = this.createPeerConnection(peerId)
    }

    // Create a data channel for sending messages
    const dataChannel = pc.createDataChannel('chat', {
      ordered: false // Use unordered messages for better performance
    })
    this.setupDataChannel(peerId, dataChannel)

    // Create offer
    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)

    console.log(`Offer 已创建并设置为本地描述`)
    return offer
  }

  /**
   * Create an answer to respond to an offer
   */
  async createAnswer(peerId, offer) {
    console.log(`=== 创建 Answer ===`)
    console.log(`对等方: ${peerId}`)

    let pc = this.peerConnections.get(peerId)
    if (!pc) {
      pc = this.createPeerConnection(peerId)
    }

    // Set remote description (offer)
    await pc.setRemoteDescription(new RTCSessionDescription(offer))
    console.log(`远程描述已设置`)

    // Create answer
    const answer = await pc.createAnswer()
    await pc.setLocalDescription(answer)
    console.log(`Answer 已创建并设置为本地描述`)

    return answer
  }

  /**
   * Set remote description (answer)
   */
  async setRemoteDescription(peerId, description) {
    const pc = this.peerConnections.get(peerId)
    if (!pc) {
      throw new Error(`No peer connection found for ${peerId}`)
    }

    await pc.setRemoteDescription(new RTCSessionDescription(description))
  }

  /**
   * Add an ICE candidate
   */
  async addIceCandidate(peerId, candidate) {
    const pc = this.peerConnections.get(peerId)
    if (!pc) {
      console.warn(`No peer connection found for ${peerId}, storing candidate for later`)
      // TODO: Store candidate for later
      return
    }

    await pc.addIceCandidate(new RTCIceCandidate(candidate))
  }

  /**
   * Send a message to a peer via the data channel
   */
  sendToPeer(peerId, data) {
    const dataChannel = this.dataChannels.get(peerId)
    if (!dataChannel || dataChannel.readyState !== 'open') {
      console.error(`Data channel not ready for peer ${peerId}`)
      return false
    }

    try {
      // data 已经是序列化后的字符串，直接发送
      dataChannel.send(data)
      return true
    } catch (error) {
      console.error(`Failed to send message to peer ${peerId}:`, error)
      return false
    }
  }

  /**
   * Broadcast a message to all connected peers
   */
  broadcast(data) {
    let successCount = 0
    for (const [peerId, dataChannel] of this.dataChannels) {
      if (dataChannel.readyState === 'open') {
        if (this.sendToPeer(peerId, data)) {
          successCount++
        }
      }
    }
    return successCount
  }

  /**
   * Close a peer connection
   */
  closePeerConnection(peerId) {
    const pc = this.peerConnections.get(peerId)
    if (pc) {
      pc.close()
      this.peerConnections.delete(peerId)
    }

    const dataChannel = this.dataChannels.get(peerId)
    if (dataChannel) {
      dataChannel.close()
      this.dataChannels.delete(peerId)
    }

    console.log(`Closed peer connection with ${peerId}`)
  }

  /**
   * Close all peer connections
   */
  closeAll() {
    for (const [peerId, pc] of this.peerConnections) {
      pc.close()
    }
    this.peerConnections.clear()

    for (const [peerId, dataChannel] of this.dataChannels) {
      dataChannel.close()
    }
    this.dataChannels.clear()

    console.log('Closed all peer connections')
  }

  /**
   * Get connection stats
   */
  getStats() {
    const stats = {
      totalPeers: this.peerConnections.size,
      connectedPeers: 0,
      dataChannelsOpen: 0
    }

    for (const [peerId, pc] of this.peerConnections) {
      if (pc.connectionState === 'connected') {
        stats.connectedPeers++
      }
    }

    for (const [peerId, dataChannel] of this.dataChannels) {
      if (dataChannel.readyState === 'open') {
        stats.dataChannelsOpen++
      }
    }

    return stats
  }
}
