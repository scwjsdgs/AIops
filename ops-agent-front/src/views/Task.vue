<template>
  <div>
    <el-card>
      <h3>任务查询</h3>
      <el-form inline>
        <el-form-item label="任务ID">
          <el-input v-model="taskId" placeholder="请输入任务ID" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTask">查询</el-button>
        </el-form-item>
      </el-form>

      <div v-if="task" style="margin-top: 20px;">
        <el-descriptions title="任务详情" :column="2" border>
          <el-descriptions-item label="任务ID">{{ task.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="task.status === 'SUCCESS' ? 'success' : 'warning'">{{ task.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="类型">{{ task.type }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ task.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="输入">{{ task.input || '无' }}</el-descriptions-item>
          <el-descriptions-item label="输出">{{ task.output || '无' }}</el-descriptions-item>
          <el-descriptions-item label="Agent步骤" :span="2">
            <el-tag v-for="(step, idx) in task.agentSteps" :key="idx" size="small" style="margin: 2px;">{{ step }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-else-if="searched" description="未找到任务" />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getTask } from '@/api/task'
import { ElMessage } from 'element-plus'

const taskId = ref('')
const task = ref(null)
const searched = ref(false)

const loadTask = async () => {
  if (!taskId.value.trim()) {
    ElMessage.warning('请输入任务ID')
    return
  }
  try {
    const res = await getTask(taskId.value)
    task.value = res.data
    searched.value = true
  } catch (e) {
    task.value = null
    searched.value = true
    ElMessage.error('任务不存在或查询失败')
  }
}
</script>