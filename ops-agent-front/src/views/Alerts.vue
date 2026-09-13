<template>
  <div>
    <el-card>
      <div style="margin-bottom: 20px;">
        <el-button type="primary" @click="sendTestAlert">发送测试告警</el-button>
      </div>
      <el-table :data="alerts" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="severity" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="row.severity === 'CRITICAL' ? 'danger' : 'warning'">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'RESOLVED' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="timestamp" label="时间" width="180" />
      </el-table>
      <el-pagination layout="prev, pager, next" :total="total" @current-change="loadAlerts" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAlerts, sendAlert } from '@/api/alert'
import { ElMessage } from 'element-plus'

const alerts = ref([])
const total = ref(0)
const page = ref(1)
const size = 10

const loadAlerts = async (p = page.value) => {
  try {
    // 后端暂未提供分页接口，此处使用模拟数据
    // 实际开发时替换为真实接口
    const res = await getAlerts({ page: p, size })
    alerts.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    // 若接口不存在，使用模拟数据
    alerts.value = []
    total.value = 0
  }
}

const sendTestAlert = async () => {
  try {
    await sendAlert({
      source: '前端测试',
      severity: 'WARNING',
      title: '测试告警',
      description: '这是一个来自前端的测试告警',
      serviceName: 'frontend'
    })
    ElMessage.success('告警发送成功')
    loadAlerts()
  } catch (e) {
    ElMessage.error('发送失败')
  }
}

onMounted(() => loadAlerts())
</script>