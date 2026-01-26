<template>
  <div class="profile-page">
    <div class="profile-card">
      <div class="avatar-section">
        <el-avatar :size="120" :src="currentUser.avatar">
          {{ currentUser.username ? currentUser.username[0] : '?' }}
        </el-avatar>
        <el-button type="primary" link>更换头像</el-button>
      </div>

      <div class="info-section">
        <h2>{{ currentUser.username }}</h2>
        <p class="email">{{ currentUser.email }}</p>
        <div class="status-badge">
          <span :class="['status-dot', currentUser.status]"></span>
          {{ formatStatus(currentUser.status) }}
        </div>
      </div>

      <el-divider />

      <div class="details-section">
        <div class="detail-item">
          <span class="label">用户ID</span>
          <span class="value">{{ currentUser.id }}</span>
        </div>
        <div class="detail-item">
          <span class="label">加入时间</span>
          <span class="value">{{ formatDate(currentUser.createdAt) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">上次登录</span>
          <span class="value">{{ formatDate(currentUser.lastLoginAt) }}</span>
        </div>
      </div>

      <el-divider />

      <div class="actions-section">
        <el-button type="danger" @click="handleLogout">
          <i class="el-icon-switch-button"></i>
          退出登录
        </el-button>
      </div>
    </div>

    <div class="settings-card">
      <h3>隐私与安全</h3>
      <p class="description">
        您的消息使用 WebRTC DTLS 进行端到端加密保护。这意味着只有您和与您交流的人才能阅读消息，
        中间的任何人，甚至是服务器，都无法访问。
      </p>

      <div class="encryption-info">
        <div class="info-item">
          <i class="el-icon-lock"></i>
          <span>DTLS-SRTP 加密</span>
        </div>
        <div class="info-item">
          <i class="el-icon-key"></i>
          <span>P2P 点对点连接</span>
        </div>
        <div class="info-item">
          <i class="el-icon-circle-check"></i>
          <span>服务器无法访问消息</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'

export default {
  name: 'Profile',
  computed: {
    ...mapState('auth', ['user']),
    currentUser() {
      return this.user || {}
    }
  },
  methods: {
    ...mapActions('auth', ['logout']),

    formatStatus(status) {
      const statusMap = {
        online: '在线',
        offline: '离线',
        away: '离开',
        busy: '忙碌'
      }
      return statusMap[status] || '未知'
    },

    formatDate(dateString) {
      if (!dateString) return '从未'
      return new Date(dateString).toLocaleDateString()
    },

    async handleLogout() {
      try {
        await this.$confirm('确定要退出登录吗？', '确认退出', {
          confirmButtonText: '退出',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await this.logout()
        this.$router.push('/login')
      } catch {
        // 用户取消
      }
    }
  }
}
</script>

<style scoped>
.profile-page {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-card,
.settings-card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.info-section {
  text-align: center;
  margin-bottom: 16px;
}

.info-section h2 {
  font-size: 24px;
  margin: 0 0 8px 0;
}

.email {
  color: #666;
  margin: 0 0 16px 0;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: #f0f0f0;
  border-radius: 20px;
  font-size: 14px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background: #67c23a;
}

.status-dot.offline {
  background: #909399;
}

.status-dot.away {
  background: #e6a23c;
}

.status-dot.busy {
  background: #f56c6c;
}

.details-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
}

.label {
  color: #666;
}

.value {
  font-weight: 500;
}

.actions-section {
  display: flex;
  justify-content: center;
}

.settings-card h3 {
  font-size: 18px;
  margin: 0 0 12px 0;
}

.description {
  color: #666;
  line-height: 1.6;
  margin-bottom: 24px;
}

.encryption-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  color: #667eea;
  font-weight: 500;
}
</style>
