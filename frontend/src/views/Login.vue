<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="title">聊天室</h1>
      <p class="subtitle">端到端加密消息</p>

      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- Username/Password Login -->
        <el-tab-pane label="密码登录" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            class="login-form"
            @submit.native.prevent="handlePasswordLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="passwordForm.username"
                placeholder="请输入用户名"
                size="large"
                prefix-icon="el-icon-user"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="passwordForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                prefix-icon="el-icon-lock"
                show-password
                @keyup.native.enter="handlePasswordLogin"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="handlePasswordLogin"
                class="login-button"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Email Verification Code Login -->
        <el-tab-pane label="邮箱登录" name="email">
          <el-form
            ref="emailFormRef"
            :model="emailForm"
            :rules="emailRules"
            class="login-form"
          >
            <el-form-item prop="email">
              <el-input
                v-model="emailForm.email"
                placeholder="请输入邮箱"
                size="large"
                prefix-icon="el-icon-message"
              />
            </el-form-item>

            <el-form-item prop="code">
              <div class="code-input-group">
                <el-input
                  v-model="emailForm.code"
                  placeholder="请输入验证码"
                  size="large"
                  prefix-icon="el-icon-key"
                  maxlength="6"
                />
                <el-button
                  :disabled="countdown > 0"
                  :loading="sendingCode"
                  size="large"
                  @click="handleSendCode"
                  class="send-code-button"
                >
                  {{ countdown > 0 ? `${countdown}秒` : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="handleEmailLogin"
                class="login-button"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="footer-links">
        <router-link to="/register">注册账号</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { mapActions } from 'vuex'

export default {
  name: 'Login',
  data() {
    return {
      activeTab: 'password',
      loading: false,
      sendingCode: false,
      countdown: 0,
      passwordForm: {
        username: '',
        password: ''
      },
      emailForm: {
        email: '',
        code: ''
      },
      passwordRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      },
      emailRules: {
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
        ],
        code: [
          { required: true, message: '请输入验证码', trigger: 'blur' },
          { len: 6, message: '验证码必须是6位数字', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    ...mapActions('auth', ['login', 'sendVerificationCode', 'loginWithCode']),

    async handlePasswordLogin() {
      // 使用 try-catch 处理表单验证
      let valid = false
      try {
        valid = await this.$refs.passwordFormRef.validate()
      } catch {
        valid = false
      }
      if (!valid) return

      this.loading = true
      try {
        const result = await this.login({
          username: this.passwordForm.username,
          password: this.passwordForm.password
        })
        console.log('登录成功，返回数据:', result)
        console.log('当前用户:', this.$store.state.auth.user)
        console.log('isAuthenticated:', this.$store.getters['auth/isAuthenticated'])
        this.$message.success('登录成功')
        // 使用 nextTick 确保状态已更新
        await this.$nextTick()
        await this.$router.push('/chat')
      } catch (error) {
        console.error('登录失败:', error)
        this.$message.error(error.message || '登录失败')
      } finally {
        this.loading = false
      }
    },

    async handleSendCode() {
      // 手动验证邮箱字段
      if (!this.emailForm.email) {
        this.$message.warning('请输入邮箱')
        return
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.emailForm.email)) {
        this.$message.warning('邮箱格式不正确')
        return
      }

      this.sendingCode = true
      try {
        await this.sendVerificationCode({ email: this.emailForm.email })
        this.$message.success('验证码已发送至您的邮箱')

        // Start countdown
        this.countdown = 60
        const timer = setInterval(() => {
          this.countdown--
          if (this.countdown <= 0) {
            clearInterval(timer)
          }
        }, 1000)
      } catch (error) {
        this.$message.error(error.message || '发送验证码失败')
      } finally {
        this.sendingCode = false
      }
    },

    async handleEmailLogin() {
      // 使用 try-catch 处理表单验证
      let valid = false
      try {
        valid = await this.$refs.emailFormRef.validate()
      } catch {
        valid = false
      }
      if (!valid) return

      this.loading = true
      try {
        const result = await this.loginWithCode({
          email: this.emailForm.email,
          code: this.emailForm.code
        })
        console.log('邮箱登录成功，返回数据:', result)
        console.log('当前用户:', this.$store.state.auth.user)
        console.log('isAuthenticated:', this.$store.getters['auth/isAuthenticated'])
        this.$message.success('登录成功')
        // 使用 nextTick 确保状态已更新
        await this.$nextTick()
        await this.$router.push('/chat')
      } catch (error) {
        console.error('邮箱登录失败:', error)
        this.$message.error(error.message || '登录失败')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.title {
  font-size: 32px;
  font-weight: 700;
  text-align: center;
  color: #333;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 14px;
  text-align: center;
  color: #666;
  margin-bottom: 32px;
}

.login-tabs {
  margin-bottom: 24px;
}

.login-form {
  margin-top: 24px;
}

.code-input-group {
  display: flex;
  gap: 12px;
}

.code-input-group .el-input {
  flex: 1;
}

.send-code-button {
  min-width: 120px;
}

.login-button {
  width: 100%;
}

.footer-links {
  text-align: center;
  margin-top: 24px;
}

.footer-links a {
  color: #667eea;
  text-decoration: none;
  font-size: 14px;
}

.footer-links a:hover {
  text-decoration: underline;
}
</style>
