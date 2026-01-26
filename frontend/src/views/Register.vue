<template>
  <div class="register-container">
    <div class="register-card">
      <h1 class="title">注册账号</h1>
      <p class="subtitle">加入我们的加密聊天室</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="register-form"
        @submit.native.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            prefix-icon="el-icon-user"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="邮箱"
            size="large"
            prefix-icon="el-icon-message"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            prefix-icon="el-icon-lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
            size="large"
            prefix-icon="el-icon-lock"
            show-password
            @keyup.native.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleRegister"
            class="register-button"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer-links">
        <router-link to="/login">已有账号？立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { mapActions } from 'vuex'

export default {
  name: 'Register',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.form.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }

    return {
      loading: false,
      form: {
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 100, message: '密码长度为6-100个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    ...mapActions('auth', ['register']),

    async handleRegister() {
      const valid = await this.$refs.formRef.validate().catch(() => false)
      if (!valid) return

      this.loading = true
      try {
        const result = await this.register({
          username: this.form.username,
          email: this.form.email,
          password: this.form.password
        })
        console.log('注册成功，返回数据:', result)
        console.log('当前用户:', this.$store.state.auth.user)
        console.log('isAuthenticated:', this.$store.getters['auth/isAuthenticated'])
        this.$message.success('注册成功')
        await this.$nextTick()
        await this.$router.push('/chat')
      } catch (error) {
        console.error('注册失败:', error)
        this.$message.error(error.message || '注册失败')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
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

.register-form {
  margin-top: 24px;
}

.register-button {
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
