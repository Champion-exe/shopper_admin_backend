<template>
  <div class="admin-page">
    <h2 class="admin-page-title">用户管理</h2>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable style="width:200px" @keyup.enter="fetchData" />
      <el-select v-model="roleFilter" placeholder="角色筛选" clearable style="width:130px" @change="fetchData">
        <el-option label="管理员" value="admin" />
        <el-option label="商家" value="seller" />
        <el-option label="用户" value="user" />
      </el-select>
      <el-button type="danger" @click="fetchData">查询</el-button>
    </div>

    <el-table :data="list" stripe v-loading="loading" style="margin-top:16px">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="角色" width="90">
        <template #default="{ row }">
          <el-tag :type="row.role==='admin'?'danger':row.role==='seller'?'warning':'info'" size="small">
            {{ row.role==='admin'?'管理员':row.role==='seller'?'商家':'用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170" />
      <el-table-column label="操作" min-width="180">
        <template #default="{ row }">
          <template v-if="row.role!=='admin'">
            <el-button v-if="row.role!=='seller'" size="small" @click="setRole(row, 'seller')">设为商家</el-button>
            <el-button v-if="row.role!=='user'" size="small" type="primary" @click="setRole(row, 'user')">解封/设为用户</el-button>
            <el-button v-if="row.role!=='' && row.id!==currentUserId" size="small" type="danger" @click="setRole(row, '')">封禁</el-button>
          </template>
          <span v-else style="color:#999;font-size:0.85rem">系统管理员</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page" :page-size="size" :total="total"
      layout="total, prev, pager, next" style="margin-top:16px;justify-content:center"
      @current-change="fetchData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserList, updateUserStatus } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')
const roleFilter = ref('')
const page = ref(1)
const size = ref(10)
const currentUserId = ref<number>(0)

// 获取当前登录用户
const stored = localStorage.getItem('adminUser')
if (stored) {
  try {
    const data = JSON.parse(stored)
    currentUserId.value = (data.user || data).id || 0
  } catch {}
}
const total = ref(0)

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserList({
      page: page.value, size: size.value,
      keyword: keyword.value || undefined,
      role: roleFilter.value || undefined,
    })
    if (res.code === 200) { list.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

async function setRole(row: any, role: string) {
  const labels: Record<string,string> = {'admin':'设为管理员','seller':'设为商家','user':'设为用户','':'封禁'}
  try {
    await ElMessageBox.confirm(`确定将 ${row.nickname || row.username} ${labels[role]}？`, '修改角色', { type: 'warning' })
    const res = await updateUserStatus(row.id, { role })
    if (res.code === 200) { ElMessage.success('已更新'); fetchData() }
  } catch {}
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.admin-page { padding: 20px; }
.admin-page-title { font-size: 1.3rem; font-weight: 700; margin-bottom: 20px; }
.toolbar { display: flex; gap: 10px; align-items: center; }
</style>
