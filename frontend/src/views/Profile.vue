<template>
  <div class="profile-page">
    <el-button class="back-button" @click="goBack" icon="el-icon-arrow-left">返回聊天室</el-button>
    <div class="profile-card">
      <div class="avatar-section">
        <el-avatar :size="120" :src="currentUser.avatar">
          {{ currentUser.username ? currentUser.username[0] : '?' }}
        </el-avatar>
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
      </div>

      <el-divider />

      <div class="actions-section">
        <el-button @click="showPasswordDialog = true">
          <i class="el-icon-edit"></i>
          修改密码
        </el-button>
        <el-button type="danger" @click="handleLogout">
          <i class="el-icon-switch-button"></i>
          退出登录
        </el-button>
      </div>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog title="修改密码" :visible.sync="showPasswordDialog" width="400px">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordForm" label-width="100px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="text" placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（至少6位）" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancelPasswordChange">取 消</el-button>
        <el-button type="primary" @click="handlePasswordChange" :loading="passwordLoading">确 定</el-button>
      </div>
    </el-dialog>

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
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请再次输入新密码'))
      } else if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }

    const validateNewPassword = (rule, value, callback) => {
      if (value === this.passwordForm.oldPassword) {
        callback(new Error('新密码不能与旧密码相同'))
      } else {
        callback()
      }
    }

    return {
      showPasswordDialog: false,
      passwordLoading: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入旧密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '新密码长度不能少于6位', trigger: 'blur' },
          { validator: validateNewPassword, trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    ...mapState('auth', ['user']),
    currentUser() {
      return this.user || {}
    }
  },
  methods: {
    ...mapActions('auth', ['logout', 'updatePassword']),

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

    goBack() {
      this.$router.push('/chat')
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
    },

    async handlePasswordChange() {
      try {
        await this.$refs.passwordForm.validate()
        this.passwordLoading = true
        await this.updatePassword({
          oldPassword: this.passwordForm.oldPassword,
          newPassword: this.passwordForm.newPassword
        })
        this.$message.success('密码修改成功，请重新登录')
        await this.logout()
        this.$router.push('/login')
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || '密码修改失败')
        }
      } finally {
        this.passwordLoading = false
      }
    },

    cancelPasswordChange() {
      this.showPasswordDialog = false
      this.passwordForm = {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
      this.$refs.passwordForm?.clearValidate()
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

.back-button {
  align-self: flex-start;
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
