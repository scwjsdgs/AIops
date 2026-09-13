<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6" v-for="stat in stats" :key="stat.label">
        <el-card class="stat-card">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top: 20px;">
      <h3>最近告警</h3>
      <el-table :data="recentAlerts" style="width: 100%">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="severity" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="row.severity === 'CRITICAL' ? 'danger' : 'warning'">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="timestamp" label="时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAlerts } from '@/api/alert'

const stats = ref([
  { label: '总告警', value: 0 },
  { label: '待处理', value: 0 },
  { label: '已处理', value: 0 },
  { label: '在线工具', value: 4 }
])
const recentAlerts = ref([])

onMounted(async () => {
  try {
    const res = await getAlerts({ page: 1, size: 5 })
    // 如果后端有数据则使用，否则保留默认
    stats.value[0].value = res.data?.total || 0
    recentAlerts.value = res.data?.records?.slice(0, 5) || []
  } catch (e) {
    // 模拟数据
    recentAlerts.value = [
      { title: '服务响应延迟', severity: 'WARNING', timestamp: '2026-06-28 10:00' },
      { title: '数据库连接超时', severity: 'CRITICAL', timestamp: '2026-06-28 09:30' }
    ]
  }
})
</script>