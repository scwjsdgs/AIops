<template>
  <div>
    <el-card>
      <el-table :data="tools" style="width: 100%">
        <el-table-column prop="name" label="工具名称" />
        <el-table-column prop="description" label="描述" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTools } from '@/api/tool'

const tools = ref([])

onMounted(async () => {
  try {
    const res = await getTools()
    tools.value = res.data || []
  } catch (e) {
    console.error(e)
  }
})
</script>