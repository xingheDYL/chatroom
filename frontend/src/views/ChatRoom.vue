<template>
    <div class="chat-room">
        <div class="sidebar">
            <div class="sidebar-header">
                <h2>聊天室</h2>
                <div class="sidebar-buttons">
                    <el-button type="success" @click="showJoinRoomDialog = true" size="small">
                        加入房间
                    </el-button>
                    <el-button type="primary" @click="showCreateRoomDialog = true" size="small">
                        新建房间
                    </el-button>
                </div>
            </div>

            <div class="room-list">
                <div
                        v-for="room in rooms"
                        :key="room.id"
                        :class="['room-item', { active: currentRoomId === room.id }]"
                        @click="selectAndJoinRoom(room)"
                >
                    <div class="room-name">{{ room.name }}</div>
                    <div class="room-type">{{ room.type === 'group' ? '群聊' : '私聊' }}</div>
                </div>
            </div>
        </div>

        <div class="main-chat">
            <div v-if="!currentRoomId" class="empty-state">
                <i class="el-icon-chat-dot-round" style="font-size: 80px;"></i>
                <p>选择一个聊天室开始聊天</p>
            </div>

            <template v-else>
                <div class="chat-header">
                    <h3>{{ currentRoom ? currentRoom.name : '' }}</h3>
                    <div class="header-right">
                        <div class="user-info" @click="goToProfile">
                            <el-avatar :src="currentUser.avatar">{{
                                currentUser.username ? currentUser.username[0] : '?'
                                }}
                            </el-avatar>
                            <span class="username">{{ currentUser.username }}</span>
                        </div>
                        <div class="connection-status">
                            <div class="status-item">
                                <span :class="['status-dot', { connected: isSignalingConnected }]"></span>
                                <span>信令: {{ isSignalingConnected ? '已连接' : '连接中...' }}</span>
                            </div>
                            <div class="status-item">
                                <span :class="['status-dot', { connected: isWebRTCConnected }]"></span>
                                <span>P2P: {{ isWebRTCConnected ? '已加密' : '等待其他用户...' }}</span>
                            </div>
                        </div>
                        <el-button @click="handleLogout" type="danger" plain>退出登录</el-button>
                    </div>
                </div>

                <div ref="messagesContainer" class="messages-container">
                    <div
                            v-for="message in displayMessages"
                            :key="message.id"
                            :class="['message', { own: message.senderId === currentUserId }]"
                    >
                        <div class="message-avatar" @click="startPrivateChat(message.senderId, message.senderName)"
                             :class="{ clickable: message.senderId !== currentUserId }">
                            <el-avatar>{{ message.senderName ? message.senderName[0] : '?' }}</el-avatar>
                        </div>
                        <div class="message-content">
                            <div class="message-header">
                                <span class="sender-name">{{ message.senderName }}</span>
                                <span class="message-time">{{ formatTime(message.createdAt) }}</span>
                            </div>
                            <div class="message-text">{{ message.content }}<small v-if="!message.content"
                                                                                  style="color:red">(空)</small></div>
                        </div>
                    </div>
                </div>

                <div class="input-area">
                    <el-input
                            v-model="messageInput"
                            type="textarea"
                            :rows="3"
                            placeholder="输入消息... (端到端加密)"
                            @keydown.native.ctrl.enter="sendMessage"
                    />
                    <el-button
                            type="primary"
                            @click="sendMessage"
                            :disabled="!messageInput.trim()"
                    >
                        发送 (Ctrl+Enter)
                    </el-button>
                </div>
            </template>
        </div>

        <div class="users-panel">
            <div class="panel-header">
                <h3>在线用户</h3>
                <span class="user-count">{{ members.length }}</span>
            </div>
            <div class="users-list">
                <div v-for="member in members" :key="member.id" class="user-item"
                     @click="startPrivateChat(member.id, member.username)"
                     :class="{ clickable: member.id !== currentUserId }">
                    <el-avatar :src="member.avatar">{{ member.username ? member.username[0] : '?' }}</el-avatar>
                    <span class="user-name">{{ member.username }}</span>
                </div>
                <div v-if="members.length === 0" class="no-users">
                    暂无在线用户
                </div>
            </div>
        </div>

        <!-- Create Room Dialog -->
        <el-dialog :visible.sync="showCreateRoomDialog" title="创建新聊天室" width="400px">
            <el-form :model="newRoomForm">
                <el-form-item label="房间名称">
                    <el-input v-model="newRoomForm.name" placeholder="请输入房间名称"/>
                </el-form-item>
                <el-form-item label="房间描述">
                    <el-input
                            v-model="newRoomForm.description"
                            type="textarea"
                            placeholder="可选描述"
                    />
                </el-form-item>
            </el-form>
            <span slot="footer" class="dialog-footer">
        <el-button @click="showCreateRoomDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCreateRoomForm">创建</el-button>
      </span>
        </el-dialog>

        <!-- Join Room Dialog -->
        <el-dialog :visible.sync="showJoinRoomDialog" title="通过ID加入房间" width="400px">
            <el-form :model="joinRoomForm">
                <el-form-item label="房间ID">
                    <el-input
                            v-model.number="joinRoomForm.roomId"
                            placeholder="请输入房间ID"
                            type="number"
                    />
                    <div class="room-id-hint">
                        提示：房间ID在URL中显示，例如 /chat?roomId=123
                    </div>
                </el-form-item>
            </el-form>
            <span slot="footer" class="dialog-footer">
        <el-button @click="showJoinRoomDialog = false">取消</el-button>
        <el-button type="primary" @click="joinRoomById">加入</el-button>
      </span>
        </el-dialog>
    </div>
</template>

<script>
import {mapState, mapGetters, mapActions} from 'vuex'
import {WebRTCManager} from '@/webrtc/WebRTCManager'
import {SignalingClient} from '@/webrtc/signaling'
import {createMessage, serializeMessage, MessageType, formatTimestamp} from '@/utils/encryption'

export default {
    name: 'ChatRoom',
    data() {
        return {
            currentRoomId: null,
            messageInput: '',
            showCreateRoomDialog: false,
            showJoinRoomDialog: false,
            isWebRTCConnected: false,
            isSignalingConnected: false,
            newRoomForm: {
                name: '',
                description: '',
                type: 'group',
                maxMembers: 100
            },
            joinRoomForm: {
                roomId: null
            },
            // WebRTC instances
            webRTCManager: null,
            signalingClient: null,
            chatWebSocket: null,
            memberRefreshTimer: null
        }
    },
    computed: {
        ...mapState('auth', ['user']),
        ...mapState('chat', ['rooms', 'currentRoom', 'messages', 'members']),
        ...mapGetters('chat', ['sortedMessages', 'getMemberById', 'getMemberName']),
        currentUser() {
            return this.user || {}
        },
        currentUserId() {
            return this.user ? this.user.id : null
        },
        displayMessages() {
            return this.sortedMessages
        }
    },
    mounted() {
        this.$store.dispatch('auth/initFromStorage')
        if (!this.$store.getters['auth/isAuthenticated']) {
            this.$router.push('/login')
            return
        }
        this.loadRooms()

        // 检查URL中是否有roomId参数
        const roomIdFromUrl = this.$route.query.roomId
        if (roomIdFromUrl) {
            this.handleUrlRoomId(roomIdFromUrl)
        }
    },
    beforeDestroy() {
        this.disconnect()
    },
    methods: {
        ...mapActions('auth', ['logout']),
        ...mapActions('chat', {
            fetchRoomsAction: 'fetchRooms',
            createRoomAction: 'createRoom',
            joinRoomAction: 'joinRoom',
            getRoomAction: 'getRoom',
            fetchRoomMessages: 'fetchRoomMessages',
            fetchRoomMembers: 'fetchRoomMembers',
            addMessage: 'addMessage',
            setCurrentRoom: 'setCurrentRoom',
            setOnlineUsers: 'setOnlineUsers',
            createOrGetPrivateChat: 'createOrGetPrivateChat'
        }),

        formatTime(timestamp) {
            return formatTimestamp(new Date(timestamp).getTime())
        },

        getStatusText(status) {
            const statusMap = {
                'online': '在线',
                'offline': '离线',
                'away': '离开',
                'busy': '忙碌'
            }
            return statusMap[status] || status
        },

        async loadRooms() {
            try {
                await this.fetchRoomsAction()
            } catch (error) {
                this.$message.error('加载聊天室失败')
            }
        },

        async submitCreateRoomForm() {
            if (!this.newRoomForm.name.trim()) {
                this.$message.warning('请输入房间名称')
                return
            }

            try {
                const response = await this.createRoomAction({
                    name: this.newRoomForm.name,
                    description: this.newRoomForm.description,
                    type: this.newRoomForm.type,
                    maxMembers: this.newRoomForm.maxMembers
                })
                this.$message.success('创建成功')
                this.showCreateRoomDialog = false
                this.newRoomForm = {name: '', description: '', type: 'group', maxMembers: 100}
                await this.loadRooms()
                await this.selectAndJoinRoom(response.data)
            } catch (error) {
                this.$message.error(error.message || '创建失败')
            }
        },

        async selectAndJoinRoom(room) {
            if (this.currentRoomId === room.id) {
                return
            }

            this.currentRoomId = room.id
            this.setCurrentRoom(room)

            // 更新URL
            this.$router.push({name: 'Chat', query: {roomId: room.id}})

            try {
                await this.fetchRoomMessages(room.id)
                await this.fetchRoomMembers(room.id)
                this.disconnect()
                this.setupWebRTC(room.id)
                this.setupChatWebSocket(room.id)
                this.startMemberRefresh(room.id)
                this.$nextTick(() => this.scrollToBottom())
            } catch (error) {
                this.$message.error(error.message || '加载聊天室失败')
            }
        },

        async joinRoomById() {
            if (!this.joinRoomForm.roomId || this.joinRoomForm.roomId <= 0) {
                this.$message.warning('请输入有效的房间ID')
                return
            }

            try {
                const roomResponse = await this.getRoomAction(this.joinRoomForm.roomId)
                const room = roomResponse.data
                // 通过 store action 调用 API 加入房间
                try {
                    await this.$store.dispatch('chat/joinRoom', this.joinRoomForm.roomId)
                    this.$message.success('加入成功')
                } catch (joinError) {
                    // 如果已经是成员，静默处理（不显示错误）
                    if (!joinError.message || !joinError.message.includes('已经是该房间成员')) {
                        throw joinError
                    }
                    // 已经是成员，直接进入房间
                }
                await this.loadRooms()
                this.showJoinRoomDialog = false
                this.joinRoomForm.roomId = null
                await this.selectAndJoinRoom(room)
            } catch (error) {
                this.$message.error(error.message || '加入失败，房间不存在或已满员')
            }
        },

        startMemberRefresh(roomId) {
            this.stopMemberRefresh()
            this.fetchRoomMembers(roomId)
            this.memberRefreshTimer = setInterval(() => {
                this.fetchRoomMembers(roomId)
            }, 30000)
        },

        stopMemberRefresh() {
            if (this.memberRefreshTimer) {
                clearInterval(this.memberRefreshTimer)
                this.memberRefreshTimer = null
            }
        },

        async handleLogout() {
            try {
                await this.logout()
                this.$message.success('已退出登录')
                this.$router.push('/login')
            } catch (error) {
                this.$message.error('退出登录失败')
            }
        },

        goToProfile() {
            this.$router.push('/profile')
        },

        async startPrivateChat(userId, username) {
            if (userId === this.currentUserId) {
                this.$message.warning('不能与自己私聊')
                return
            }

            try {
                const response = await this.createOrGetPrivateChat(userId)
                const room = response.data
                await this.selectAndJoinRoom(room)
                this.$message.success(`已进入与 ${username} 的私聊`)
            } catch (error) {
                this.$message.error(error.message || '进入私聊失败')
            }
        },

        async handleUrlRoomId(roomIdFromUrl) {
            const room = this.rooms.find(r => r.id === Number(roomIdFromUrl))
            if (room) {
                // 房间已在列表中，直接进入
                await this.selectAndJoinRoom(room)
            } else {
                try {
                    // 房间不在列表中，先获取房间信息，然后加入，最后进入
                    const roomResponse = await this.getRoomAction(roomIdFromUrl)
                    const roomData = roomResponse.data
                    // 通过 store action 调用 API 加入房间
                    try {
                        await this.$store.dispatch('chat/joinRoom', roomIdFromUrl)
                        this.$message.success(`已加入房间：${roomData.name}`)
                    } catch (joinError) {
                        // 如果已经是成员，静默处理（不显示错误）
                        if (!joinError.message || !joinError.message.includes('已经是该房间成员')) {
                            throw joinError
                        }
                        // 已经是成员，直接继续
                    }
                    await this.loadRooms()
                    await this.selectAndJoinRoom(roomData)
                } catch (error) {
                    console.error('加入/进入房间失败:', error)
                    this.$message.error(error.message || '房间不存在或已满员')
                    this.$router.push({name: 'Chat'})
                }
            }
        },

        setupWebRTC(roomId) {
            const self = this
            this.signalingClient = new SignalingClient({
                onUserJoin: async (userId) => {
                    console.log(`=== 用户加入事件 ===`)
                    console.log(`新用户 ${userId} 加入了房间 ${roomId}`)
                    await self.fetchRoomMembers(roomId)
                    console.log(`我的用户ID: ${self.currentUserId}`)
                    console.log(`这是其他用户，正在建立WebRTC连接`)
                    const offer = await self.webRTCManager.createOffer(userId)
                    self.signalingClient.sendOffer(userId, offer)
                },
                onUserLeave: (userId) => {
                    console.log(`用户 ${userId} 离开了`)
                    self.webRTCManager?.closePeerConnection(userId)
                    self.fetchRoomMembers(roomId)
                },
                onOffer: async (senderId, offerData) => {
                    console.log(`=== 收到连接请求 ===`)
                    console.log(`来自用户: ${senderId}`)
                    console.log(`我的用户ID: ${self.currentUserId}`)
                    const answer = await self.webRTCManager.createAnswer(senderId, offerData)
                    self.signalingClient.sendAnswer(senderId, answer)
                },
                onAnswer: async (senderId, answerData) => {
                    console.log(`=== 收到连接响应 ===`)
                    console.log(`来自用户: ${senderId}`)
                    await self.webRTCManager.setRemoteDescription(senderId, answerData)
                },
                onIceCandidate: async (senderId, candidateData) => {
                    console.log(`=== 收到 ICE 候选者 ===`)
                    console.log(`来自用户: ${senderId}`)
                    console.log(`候选者:`, candidateData)
                    await self.webRTCManager.addIceCandidate(senderId, candidateData)
                },
                onOpen: () => {
                    console.log('=== 信令服务器已连接 ===')
                    self.isSignalingConnected = true
                },
                onClose: () => {
                    console.log('信令服务器已断开')
                    self.isSignalingConnected = false
                    self.isWebRTCConnected = false
                },
                onError: (error) => {
                    console.error('信令服务器错误:', error)
                }
            })

            this.webRTCManager = new WebRTCManager({
                localUserId: self.currentUserId,
                roomId: roomId,
                onDataChannelMessage: (peerId, data) => {
                    const message = JSON.parse(data)
                    if (message && message.type !== 'typing') {
                        let senderName = message.metadata?.senderName
                        if (!senderName) {
                            const member = self.getMemberById(peerId)
                            senderName = member ? member.username : `用户 ${peerId}`
                        }

                        self.addMessage({
                            id: message.id || `p2p-${Date.now()}`,
                            senderId: peerId,
                            senderName: senderName,
                            content: message.content || '',
                            messageType: message.type || 'text',
                            createdAt: new Date(message.timestamp || Date.now()).toISOString()
                        })
                        self.$nextTick(() => self.scrollToBottom())
                    }
                },
                onDataChannelOpen: () => {
                    self.isWebRTCConnected = true
                },
                onDataChannelClose: () => {
                    self.isWebRTCConnected = false
                },
                onIceCandidate: (peerId, candidate) => {
                    console.log(`=== WebRTC ICE 候选者回调 ===`)
                    console.log(`对等方: ${peerId}`)
                    console.log(`候选者:`, candidate.candidate)
                    self.signalingClient.sendIceCandidate(peerId, candidate)
                },
                onConnectionStateChange: (peerId, state) => {
                    console.log(`与用户 ${peerId} 的连接状态: ${state}`)
                }
            })

            this.signalingClient.connect(roomId, this.currentUserId)
        },

        setupChatWebSocket(roomId) {
            const self = this
            // 生产环境使用环境变量配置的地址，开发环境使用当前页面主机名
            let wsBaseUrl
            if (process.env.NODE_ENV === 'production' && process.env.VUE_APP_WS_BASE_URL) {
                wsBaseUrl = process.env.VUE_APP_WS_BASE_URL
            } else if (process.env.NODE_ENV === 'production') {
                // 生产环境但未配置环境变量：使用当前页面的协议和域名
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

            const wsUrl = `${wsBaseUrl}/ws/chat/${roomId}?userId=${this.currentUserId}`

            console.log('聊天WebSocket连接URL:', wsUrl)

            const ws = new WebSocket(wsUrl)

            ws.onopen = () => {
                console.log('聊天WebSocket已连接')
            }

            ws.onmessage = (event) => {
                const data = JSON.parse(event.data)

                // 处理心跳消息
                if (data.type === 'ping') {
                    ws.send(JSON.stringify({type: 'pong'}))
                    return
                }
                if (data.type === 'pong') {
                    // 心跳响应，连接正常
                    return
                }

                if (data.type === 'message' && data.data) {
                    const msg = data.data
                    const exists = self.messages.some(m => m.id === msg.id)
                    if (!exists && msg.senderId !== self.currentUserId) {
                        self.addMessage({
                            id: msg.id,
                            senderId: msg.senderId,
                            senderName: msg.senderName,
                            content: msg.content,
                            messageType: msg.messageType,
                            createdAt: msg.createdAt
                        })
                        self.$nextTick(() => self.scrollToBottom())
                    }
                } else if (data.type === 'online-users') {
                    self.setOnlineUsers(data.count)
                }
            }

            ws.onerror = (error) => {
                console.error('聊天WebSocket错误:', error)
            }

            ws.onclose = () => {
                console.log('聊天WebSocket已断开')
            }

            this.chatWebSocket = ws
        },

        disconnect() {
            this.stopMemberRefresh()
            this.webRTCManager?.closeAll()
            this.signalingClient?.disconnect()
            this.chatWebSocket?.close()

            this.webRTCManager = null
            this.signalingClient = null
            this.chatWebSocket = null
            this.isWebRTCConnected = false
            this.isSignalingConnected = false
        },

        sendMessage() {
            if (!this.messageInput.trim()) return

            const content = this.messageInput.trim()
            this.messageInput = ''

            const message = createMessage(MessageType.TEXT, content, {
                senderId: this.currentUserId,
                senderName: this.currentUser.username || '我'
            })

            // Send via WebRTC
            if (this.isWebRTCConnected) {
                this.webRTCManager.broadcast(serializeMessage(message))
            }

            // Send via WebSocket for storage
            if (this.chatWebSocket?.readyState === WebSocket.OPEN) {
                this.chatWebSocket.send(JSON.stringify({
                    type: 'message',
                    content: content,
                    messageType: 'text'
                }))
            }

            // Add to local display
            this.addMessage({
                id: message.id,
                senderId: this.currentUserId,
                senderName: this.currentUser.username || '我',
                content: content,
                messageType: 'text',
                createdAt: new Date().toISOString()
            })

            this.$nextTick(() => this.scrollToBottom())
        },

        scrollToBottom() {
            if (this.$refs.messagesContainer) {
                this.$refs.messagesContainer.scrollTop = this.$refs.messagesContainer.scrollHeight
            }
        }
    }
}
</script>

<style scoped>
.chat-room {
    display: flex;
    height: 100vh;
    background: #f5f5f5;
}

.sidebar {
    width: 280px;
    background: white;
    border-right: 1px solid #e0e0e0;
    display: flex;
    flex-direction: column;
}

.sidebar-header {
    padding: 20px;
    border-bottom: 1px solid #e0e0e0;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.sidebar-header h2 {
    font-size: 18px;
    margin: 0;
}

.room-list {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
}

.room-item {
    padding: 12px 16px;
    border-radius: 8px;
    cursor: pointer;
    margin-bottom: 8px;
    transition: background 0.2s;
}

.room-item:hover {
    background: #f5f5f5;
}

.room-item.active {
    background: #667eea;
    color: white;
}

.room-name {
    font-weight: 500;
}

.room-type {
    font-size: 12px;
    opacity: 0.7;
    margin-top: 4px;
}

.main-chat {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: white;
}

.empty-state {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #999;
}

.chat-header {
    padding: 16px 24px;
    border-bottom: 1px solid #e0e0e0;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.chat-header h3 {
    margin: 0;
}

.connection-status {
    display: flex;
    align-items: center;
    gap: 16px;
    font-size: 14px;
    color: #666;
}

.status-item {
    display: flex;
    align-items: center;
    gap: 6px;
}

.status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #f56c6c;
}

.status-dot.connected {
    background: #67c23a;
}

.header-right {
    display: flex;
    align-items: center;
    gap: 20px;
}

.user-info {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
    padding: 8px 12px;
    border-radius: 8px;
    transition: background 0.2s;
}

.user-info:hover {
    background: #f5f5f5;
}

.user-info .username {
    font-size: 14px;
    font-weight: 500;
    color: #333;
}

.messages-container {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.message {
    display: flex;
    gap: 12px;
    max-width: 70%;
}

.message.own {
    align-self: flex-end;
    flex-direction: row-reverse;
}

.message-content {
    flex: 1;
}

.message-header {
    display: flex;
    gap: 12px;
    margin-bottom: 4px;
}

.sender-name {
    font-weight: 500;
    font-size: 14px;
}

.message-time {
    font-size: 12px;
    color: #999;
}

.message-text {
    padding: 10px 16px;
    background: #f0f0f0;
    border-radius: 12px;
    word-break: break-word;
}

.message.own .message-text {
    background: #667eea;
    color: white;
}

.message-avatar.clickable {
    cursor: pointer;
    transition: transform 0.2s;
}

.message-avatar.clickable:hover {
    transform: scale(1.1);
}

.input-area {
    padding: 20px;
    border-top: 1px solid #e0e0e0;
    display: flex;
    gap: 12px;
}

.input-area .el-textarea {
    flex: 1;
}

.users-panel {
    width: 240px;
    background: white;
    border-left: 1px solid #e0e0e0;
    display: flex;
    flex-direction: column;
}

.panel-header {
    padding: 16px;
    border-bottom: 1px solid #e0e0e0;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.panel-header h3 {
    font-size: 16px;
    margin: 0;
}

.user-count {
    background: #667eea;
    color: white;
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 12px;
}

.users-list {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.user-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px;
    border-radius: 8px;
    background: #f9f9f9;
    transition: background 0.2s;
}

.user-item:hover {
    background: #f0f0f0;
}

.user-item.clickable {
    cursor: pointer;
}

.user-item.clickable:hover {
    background: #e0e0ff;
    transform: translateX(4px);
}

.user-name {
    flex: 1;
    font-size: 14px;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.user-status {
    font-size: 11px;
    padding: 2px 6px;
    border-radius: 4px;
    text-transform: capitalize;
}

.user-status.online {
    background: #67c23a;
    color: white;
}

.user-status.offline {
    background: #909399;
    color: white;
}

.user-status.away {
    background: #e6a23c;
    color: white;
}

.user-status.busy {
    background: #f56c6c;
    color: white;
}

.no-users {
    text-align: center;
    color: #999;
    font-size: 14px;
    padding: 20px 0;
}

.sidebar-buttons {
    display: flex;
    gap: 8px;
}

.room-id-hint {
    margin-top: 8px;
    font-size: 12px;
    color: #999;
    line-height: 1.5;
}
</style>