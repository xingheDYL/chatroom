<template>
  <div class="room-list-page">
    <div class="page-header">
      <h1>聊天室</h1>
      <div class="header-actions">
        <div class="user-info">
          <el-avatar :src="currentUser.avatar">{{ currentUser.username ? currentUser.username[0] : '?' }}</el-avatar>
          <span class="username">{{ currentUser.username }}</span>
        </div>
        <el-button @click="showJoinDialog = true" type="success">
          <i class="el-icon-plus"></i>
          加入房间
        </el-button>
        <el-button @click="showCreateDialog = true" type="primary">
          <i class="el-icon-plus"></i>
          创建房间
        </el-button>
        <el-button @click="handleLogout" type="danger" plain>退出登录</el-button>
      </div>
    </div>

    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabChange">
      <el-tab-pane label="我的房间" name="my">
        <div class="rooms-grid">
          <div
            v-for="room in rooms"
            :key="room.id"
            class="room-card"
            @click="enterRoom(room)"
          >
            <div class="room-icon">
              <i class="el-icon-chat-dot-round" style="font-size: 40px;"></i>
            </div>
            <div class="room-info">
              <h3>{{ room.name }}</h3>
              <p v-if="room.description">{{ room.description }}</p>
              <div class="room-meta">
                <span class="room-type">{{ room.type === 'group' ? '群聊' : '私聊' }}</span>
                <span class="member-count">
                  <i class="el-icon-user"></i>
                  {{ room.memberCount }}/{{ room.maxMembers }}
                </span>
              </div>
            </div>
          </div>

          <div v-if="rooms.length === 0" class="empty-state">
            <el-empty description="您还没有加入任何聊天室">
              <el-button type="primary" @click="activeTab = 'discover'">
                去发现房间
              </el-button>
            </el-empty>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="发现房间" name="discover">
        <div class="rooms-grid">
          <div
            v-for="room in allRoomsFiltered"
            :key="room.id"
            class="room-card"
          >
            <div class="room-icon">
              <i class="el-icon-chat-dot-round" style="font-size: 40px;"></i>
            </div>
            <div class="room-info">
              <h3>{{ room.name }}</h3>
              <p v-if="room.description">{{ room.description }}</p>
              <div class="room-meta">
                <span class="room-type">{{ room.type === 'group' ? '群聊' : '私聊' }}</span>
                <span class="member-count">
                  <i class="el-icon-user"></i>
                  {{ room.memberCount }}/{{ room.maxMembers }}
                </span>
              </div>
              <div class="room-actions">
                <el-button
                  v-if="isJoined(room.id)"
                  type="success"
                  size="small"
                  @click.stop="enterRoom(room)"
                >
                  进入
                </el-button>
                <el-button
                  v-else
                  type="primary"
                  size="small"
                  @click.stop="handleJoinRoom(room)"
                  :loading="joiningRoom === room.id"
                >
                  加入
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="allRoomsFiltered.length === 0" class="empty-state">
            <el-empty description="暂无房间" />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Create Room Dialog -->
    <el-dialog :visible.sync="showCreateDialog" title="创建新聊天室" width="500px">
      <el-form :model="newRoomForm" label-width="100px">
        <el-form-item label="房间名称" required>
          <el-input
            v-model="newRoomForm.name"
            placeholder="请输入房间名称"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="房间描述">
          <el-input
            v-model="newRoomForm.description"
            type="textarea"
            :rows="3"
            placeholder="可选描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="房间类型">
          <el-radio-group v-model="newRoomForm.type">
            <el-radio label="group">群聊</el-radio>
            <el-radio label="private">私聊（1对1）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="最大人数">
          <el-input-number
            v-model="newRoomForm.maxMembers"
            :min="2"
            :max="500"
          />
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateRoom">
          创建房间
        </el-button>
      </span>
    </el-dialog>

    <!-- Join Room Dialog -->
    <el-dialog :visible.sync="showJoinDialog" title="通过ID加入房间" width="400px">
      <el-form :model="joinRoomForm" label-width="80px">
        <el-form-item label="房间ID" required>
          <el-input
            v-model.number="joinRoomForm.roomId"
            placeholder="请输入房间ID"
            type="number"
          />
          <div class="room-id-hint">
            提示：房间创建成功后会在URL中显示，例如 /chat?roomId=123
          </div>
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="showJoinDialog = false">取消</el-button>
        <el-button type="primary" :loading="joiningByid" @click="handleJoinById">
          加入房间
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'

export default {
  name: 'RoomList',
  data() {
    return {
      activeTab: 'my',
      showCreateDialog: false,
      showJoinDialog: false,
      creating: false,
      joiningRoom: null,
      joiningByid: false,
      newRoomForm: {
        name: '',
        description: '',
        type: 'group',
        maxMembers: 100
      },
      joinRoomForm: {
        roomId: null
      }
    }
  },
  computed: {
    ...mapState('auth', ['user']),
    ...mapState('chat', ['rooms', 'allRooms']),
    currentUser() {
      return this.user || {}
    },
    // 过滤出未加入的房间用于"发现"标签页
    allRoomsFiltered() {
      const joinedRoomIds = this.rooms.map(r => r.id)
      return this.allRooms.filter(room => !joinedRoomIds.includes(room.id))
    }
  },
  mounted() {
    this.loadMyRooms()
  },
  methods: {
    ...mapActions('auth', ['logout']),
    ...mapActions('chat', ['fetchRooms', 'fetchAllRooms', 'createRoom', 'joinRoom', 'getRoom']),

    // 检查是否已加入房间
    isJoined(roomId) {
      return this.rooms.some(r => r.id === roomId)
    },

    async loadMyRooms() {
      try {
        await this.fetchRooms()
      } catch (error) {
        this.$message.error('加载我的房间失败')
      }
    },

    async loadAllRooms() {
      try {
        await Promise.all([
          this.fetchRooms(),    // 需要先加载已加入的房间
          this.fetchAllRooms()  // 再加载所有房间
        ])
      } catch (error) {
        this.$message.error('加载房间列表失败')
      }
    },

    handleTabChange(tab) {
      if (tab.name === 'discover') {
        this.loadAllRooms()
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

    async handleCreateRoom() {
      if (!this.newRoomForm.name.trim()) {
        this.$message.warning('请输入房间名称')
        return
      }

      if (this.newRoomForm.name.trim().length < 3) {
        this.$message.warning('房间名称至少需要3个字符')
        return
      }

      this.creating = true
      try {
        await this.createRoom({
          name: this.newRoomForm.name,
          description: this.newRoomForm.description,
          type: this.newRoomForm.type,
          maxMembers: this.newRoomForm.maxMembers
        })
        this.$message.success('创建成功')
        this.showCreateDialog = false
        this.newRoomForm = { name: '', description: '', type: 'group', maxMembers: 100 }
        await this.loadMyRooms()
      } catch (error) {
        this.$message.error(error.message || '创建失败')
      } finally {
        this.creating = false
      }
    },

    async handleJoinRoom(room) {
      this.joiningRoom = room.id
      try {
        await this.joinRoom(room.id)
        this.$message.success('加入成功')
        await this.loadMyRooms()
      } catch (error) {
        this.$message.error(error.message || '加入失败')
      } finally {
        this.joiningRoom = null
      }
    },

    async handleJoinById() {
      if (!this.joinRoomForm.roomId || this.joinRoomForm.roomId <= 0) {
        this.$message.warning('请输入有效的房间ID')
        return
      }

      this.joiningByid = true
      try {
        // 先获取房间信息验证是否存在
        await this.getRoom(this.joinRoomForm.roomId)
        // 加入房间
        await this.joinRoom(this.joinRoomForm.roomId)
        this.$message.success('加入成功')
        this.showJoinDialog = false
        this.joinRoomForm.roomId = null
        await this.loadMyRooms()
        // 切换到"我的房间"标签页
        this.activeTab = 'my'
      } catch (error) {
        this.$message.error(error.message || '加入失败，房间不存在或已满员')
      } finally {
        this.joiningByid = false
      }
    },

    enterRoom(room) {
      this.$router.push({ name: 'Chat', query: { roomId: room.id } })
    }
  }
}
</script>

<style scoped>
.room-list-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 32px;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info .username {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.rooms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.room-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #e0e0e0;
  display: flex;
  gap: 16px;
}

.room-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border-color: #667eea;
}

.room-icon {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.room-info {
  flex: 1;
  min-width: 0;
}

.room-info h3 {
  font-size: 18px;
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.room-info p {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.room-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #999;
}

.room-type {
  text-transform: capitalize;
  padding: 2px 8px;
  background: #f0f0f0;
  border-radius: 4px;
}

.member-count {
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 20px;
}

.room-id-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}
</style>
