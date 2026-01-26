import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '@/store'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/ChatRoom.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/rooms',
    name: 'RoomList',
    component: () => import('@/views/RoomList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true }
  }
]

const router = new VueRouter({
  mode: 'history',
  routes
})

// Navigation guard for authentication
router.beforeEach(async (to, from, next) => {
  // 检查是否需要初始化（仅当状态为空且localStorage有数据时）
  const hasTokenInStorage = !!localStorage.getItem('accessToken')
  if (!store.state.auth.user && hasTokenInStorage) {
    await store.dispatch('auth/initFromStorage')
  }

  const requiresAuth = to.matched.some(record => record.meta.requiresAuth !== false)
  const isAuthenticated = store.getters['auth/isAuthenticated']

  // 需要认证但未登录 -> 跳转到登录页
  if (requiresAuth && !isAuthenticated) {
    // 避免循环重定向：如果目标已经是登录页，直接放行
    if (to.path === '/login' || to.path === '/register') {
      next()
    } else {
      next('/login')
    }
  }
  // 已登录用户访问登录/注册页 -> 跳转到聊天页
  else if ((to.path === '/login' || to.path === '/register') && isAuthenticated) {
    next('/chat')
  }
  // 其他情况正常放行
  else {
    next()
  }
})

export default router
