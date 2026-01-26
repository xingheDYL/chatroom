<template>
  <div class="user-list">
    <div class="list-header">
      <h3>成员</h3>
      <span class="count">{{ users.length }}</span>
    </div>

    <div class="users">
      <div
        v-for="user in users"
        :key="user.id"
        :class="['user-item', { online: isOnline(user.id) }]"
      >
        <el-badge :is-dot="isOnline(user.id)" class="user-badge">
          <el-avatar :size="40" :src="user.avatar">
            {{ user.username?.[0] || '?' }}
          </el-avatar>
        </el-badge>

        <div class="user-info">
          <div class="user-name">{{ user.username }}</div>
          <div class="user-status">{{ getStatusText(user.id) }}</div>
        </div>

        <div v-if="user.id !== currentUserId" class="user-actions">
          <el-button
            v-if="canVideoCall"
            type="primary"
            link
            size="small"
            @click="$emit('video-call', user)"
          >
            <el-icon><VideoCamera /></el-icon>
          </el-button>
          <el-button
            type="primary"
            link
            size="small"
            @click="$emit('message', user)"
          >
            <el-icon><ChatDotRound /></el-icon>
          </el-button>
        </div>
      </div>

      <div v-if="users.length === 0" class="empty-state">
        <el-empty description="暂无成员" :image-size="60" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/store/auth'
import { VideoCamera, ChatDotRound } from '@element-plus/icons-vue'

const props = defineProps({
  users: {
    type: Array,
    default: () => []
  },
  onlineUserIds: {
    type: Array,
    default: () => []
  },
  canVideoCall: {
    type: Boolean,
    default: false
  }
})

defineEmits(['message', 'video-call'])

const authStore = useAuthStore()
const currentUserId = computed(() => authStore.user?.id)

function isOnline(userId) {
  return props.onlineUserIds.includes(userId)
}

function getStatusText(userId) {
  if (userId === currentUserId.value) {
    return '我'
  }
  return isOnline(userId) ? '在线' : '离线'
}
</script>

<style scoped>
.user-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: white;
}

.list-header {
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-header h3 {
  font-size: 16px;
  margin: 0;
}

.count {
  background: #667eea;
  color: white;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.users {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-item:hover {
  background: #f5f5f5;
}

.user-item.online {
  background: rgba(103, 126, 234, 0.05);
}

.user-badge {
  position: relative;
}

.user-badge :deep(.el-badge__content) {
  background: #67c23a;
  border: 2px solid white;
  right: 0;
  top: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 500;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-status {
  font-size: 12px;
  color: #999;
}

.user-item.online .user-status {
  color: #67c23a;
}

.user-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.user-item:hover .user-actions {
  opacity: 1;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}
</style>
