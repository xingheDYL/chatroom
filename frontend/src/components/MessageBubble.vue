<template>
  <div :class="['message-bubble', { own: isOwn }]">
    <div v-if="!isOwn" class="message-avatar">
      <el-avatar :size="36" :src="message.senderAvatar">
        {{ message.senderName?.[0] || '?' }}
      </el-avatar>
    </div>

    <div class="bubble-content">
      <div v-if="!isOwn" class="sender-name">{{ message.senderName }}</div>

      <div :class="['bubble', { own: isOwn }]">
        <template v-if="message.messageType === 'text'">
          <div class="text-content">{{ message.content }}</div>
        </template>

        <template v-else-if="message.messageType === 'image'">
          <el-image
            :src="message.content"
            fit="cover"
            class="image-content"
            :preview-src-list="[message.content]"
          />
        </template>

        <template v-else-if="message.messageType === 'file'">
          <div class="file-content">
            <el-icon><Document /></el-icon>
            <span>{{ getFileName(message.content) }}</span>
            <el-button link type="primary" size="small">下载</el-button>
          </div>
        </template>

        <template v-else>
          <div class="system-content">{{ message.content }}</div>
        </template>

        <div class="message-time">{{ formatTime(message.createdAt) }}</div>
      </div>
    </div>

    <div v-if="isOwn" class="message-avatar">
      <el-avatar :size="36" :src="message.senderAvatar">
        {{ message.senderName?.[0] || '?' }}
      </el-avatar>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/store/auth'
import { Document } from '@element-plus/icons-vue'
import { formatTimestamp } from '@/utils/encryption'

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
})

const authStore = useAuthStore()

const isOwn = computed(() => {
  return props.message.senderId === authStore.user?.id
})

function formatTime(timestamp) {
  return formatTimestamp(new Date(timestamp).getTime())
}

function getFileName(content) {
  try {
    const data = JSON.parse(content)
    return data.name || '文件'
  } catch {
    return '文件'
  }
}
</script>

<style scoped>
.message-bubble {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  max-width: 70%;
}

.message-bubble.own {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.bubble-content {
  flex: 1;
  min-width: 0;
}

.sender-name {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
  margin-left: 4px;
}

.bubble {
  padding: 10px 14px;
  border-radius: 16px;
  background: #f0f0f0;
  position: relative;
}

.bubble.own {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.bubble.own .sender-name {
  display: none;
}

.text-content {
  word-break: break-word;
  white-space: pre-wrap;
}

.image-content {
  max-width: 200px;
  border-radius: 8px;
}

.file-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.system-content {
  font-style: italic;
  opacity: 0.8;
}

.message-time {
  font-size: 11px;
  opacity: 0.7;
  margin-top: 4px;
}

.bubble.own .message-time {
  color: rgba(255, 255, 255, 0.8);
}
</style>
