<template>
  <div>
    <el-card>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <h3>实时监控</h3>
        <div>
          <el-input v-model="taskId" placeholder="输入任务ID" style="width: 200px; margin-right: 10px;" />
          <el-button type="primary" @click="connect" :disabled="connected">连接</el-button>
          <el-button type="danger" @click="disconnect" :disabled="!connected">断开</el-button>
        </div>
      </div>
      <el-divider />
      <div class="message-container" ref="messageContainer">
        <div v-for="(msg, idx) in messages" :key="idx" class="message-item">
          <span class="time">{{ msg.timestamp || '' }}</span>
          <span class="step">{{ msg.stepName || '' }}</span>
          <span class="content">{{ msg.content }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useWebSocketStore } from '@/store/websocket'

const wsStore = useWebSocketStore()
const taskId = ref('')
const connected = ref(false)
const messages = ref([])
const messageContainer = ref(null)

watch(() => wsStore.connected, (val) => {
  connected.value = val
})

watch(() => wsStore.messages, (newMsgs) => {
  messages.value = newMsgs
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}, { deep: true })

const connect = () => {
  if (taskId.value.trim()) {
    wsStore.connect(taskId.value.trim())
  }
}

const disconnect = () => {
  wsStore.disconnect()
}
</script>

<style scoped>
.message-container {
  height: 400px;
  overflow-y: auto;
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
}
.message-item {
  padding: 8px 0;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  gap: 15px;
}
.time {
  color: #909399;
  font-size: 12px;
  width: 120px;
}
.step {
  color: #409eff;
  font-weight: bold;
  width: 120px;
}
.content {
  color: #303133;
}
</style>