import { authApi } from '@/api/auth'

// 辅助函数：检查 token 是否过期
function isTokenExpired(expiresAt) {
  if (!expiresAt) return true
  return new Date().getTime() > new Date(expiresAt).getTime()
}

const state = {
  user: null,
  accessToken: localStorage.getItem('accessToken') || null,
  refreshToken: localStorage.getItem('refreshToken') || null,
  tokenExpiresAt: localStorage.getItem('tokenExpiresAt') || null
}

const getters = {
  isAuthenticated: (state) => {
    // 检查 token、user 是否存在，以及 token 是否过期
    return !!state.accessToken && !!state.user && !isTokenExpired(state.tokenExpiresAt)
  }
}

const mutations = {
  SET_USER(state, user) {
    state.user = user
    if (user) {
      localStorage.setItem('user', JSON.stringify(user))
    } else {
      localStorage.removeItem('user')
    }
  },
  SET_ACCESS_TOKEN(state, token) {
    state.accessToken = token
    if (token) {
      localStorage.setItem('accessToken', token)
    } else {
      localStorage.removeItem('accessToken')
    }
  },
  SET_REFRESH_TOKEN(state, token) {
    state.refreshToken = token
    if (token) {
      localStorage.setItem('refreshToken', token)
    } else {
      localStorage.removeItem('refreshToken')
    }
  },
  SET_TOKEN_EXPIRES_AT(state, expiresAt) {
    state.tokenExpiresAt = expiresAt
    if (expiresAt) {
      localStorage.setItem('tokenExpiresAt', expiresAt)
    } else {
      localStorage.removeItem('tokenExpiresAt')
    }
  },
  CLEAR_AUTH(state) {
    state.user = null
    state.accessToken = null
    state.refreshToken = null
    state.tokenExpiresAt = null
    localStorage.removeItem('user')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('tokenExpiresAt')
  }
}

const actions = {
  initFromStorage({ commit }) {
    const storedToken = localStorage.getItem('accessToken')
    const storedRefreshToken = localStorage.getItem('refreshToken')
    const storedUser = localStorage.getItem('user')
    const storedExpiresAt = localStorage.getItem('tokenExpiresAt')

    if (storedToken) {
      commit('SET_ACCESS_TOKEN', storedToken)
    }
    if (storedRefreshToken) {
      commit('SET_REFRESH_TOKEN', storedRefreshToken)
    }
    if (storedUser) {
      try {
        commit('SET_USER', JSON.parse(storedUser))
      } catch (e) {
        console.error('解析用户信息失败:', e)
        commit('CLEAR_AUTH')
        return
      }
    }
    if (storedExpiresAt) {
      commit('SET_TOKEN_EXPIRES_AT', storedExpiresAt)
    }

    // 检查 token 是否过期，如果过期则清除认证信息
    if (storedExpiresAt && isTokenExpired(storedExpiresAt)) {
      console.log('Token 已过期，清除认证信息')
      commit('CLEAR_AUTH')
    }
  },

  async register({ commit }, { username, email, password }) {
    const data = await authApi.register({ username, email, password })
    // 后端返回 ApiResponse 包装的数据，需要访问 .data
    const { user, accessToken, refreshToken, expiresIn } = data.data
    commit('SET_USER', user)
    commit('SET_ACCESS_TOKEN', accessToken)
    commit('SET_REFRESH_TOKEN', refreshToken)
    // 计算 token 过期时间（当前时间 + 有效期）
    if (expiresIn) {
      const expiresAt = new Date(Date.now() + expiresIn).toISOString()
      commit('SET_TOKEN_EXPIRES_AT', expiresAt)
    }
    return { data: data.data }
  },

  async login({ commit }, { username, password }) {
    const data = await authApi.login({ username, password })
    // 后端返回 ApiResponse 包装的数据，需要访问 .data
    const { user, accessToken, refreshToken, expiresIn } = data.data
    commit('SET_USER', user)
    commit('SET_ACCESS_TOKEN', accessToken)
    commit('SET_REFRESH_TOKEN', refreshToken)
    // 计算 token 过期时间（当前时间 + 有效期）
    if (expiresIn) {
      const expiresAt = new Date(Date.now() + expiresIn).toISOString()
      commit('SET_TOKEN_EXPIRES_AT', expiresAt)
    }
    return { data: data.data }
  },

  async sendVerificationCode(_, { email }) {
    const data = await authApi.sendCode({ email })
    return { data: data.data }
  },

  async loginWithCode({ commit }, { email, code }) {
    const data = await authApi.loginWithCode({ email, code })
    // 后端返回 ApiResponse 包装的数据，需要访问 .data
    const { user, accessToken, refreshToken, expiresIn } = data.data
    commit('SET_USER', user)
    commit('SET_ACCESS_TOKEN', accessToken)
    commit('SET_REFRESH_TOKEN', refreshToken)
    // 计算 token 过期时间（当前时间 + 有效期）
    if (expiresIn) {
      const expiresAt = new Date(Date.now() + expiresIn).toISOString()
      commit('SET_TOKEN_EXPIRES_AT', expiresAt)
    }
    return { data: data.data }
  },

  async logout({ commit }) {
    try {
      await authApi.logout()
    } catch (error) {
      console.error('登出API调用失败:', error)
    } finally {
      commit('CLEAR_AUTH')
    }
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
