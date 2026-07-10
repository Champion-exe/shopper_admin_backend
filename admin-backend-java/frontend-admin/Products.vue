<template>
  <div class="admin-page">
    <h2 class="admin-page-title">商品管理</h2>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索商品名称" clearable style="width:240px" @keyup.enter="fetchData" />
      <el-button type="danger" @click="fetchData">搜索</el-button>
      <el-button type="primary" @click="showDialog()">新增商品</el-button>
    </div>

    <el-table :data="list" stripe v-loading="loading" style="margin-top:16px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="brand" label="品牌" width="100" />
      <el-table-column label="最低价" width="100">
        <template #default="{ row }">&yen;{{ row.minPrice ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="总库存" width="80">
        <template #default="{ row }">{{ row.totalStock ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="sales" label="销量" width="80" />
      <el-table-column prop="rating" label="评分" width="70" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button size="small" @click="showDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page" :page-size="size" :total="total"
      layout="total, prev, pager, next" style="margin-top:16px;justify-content:center"
      @current-change="fetchData"
    />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="品牌"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="分类ID"><el-input-number v-model="form.categoryId" :min="1" /></el-form-item>
        <el-form-item label="商家ID"><el-input-number v-model="form.sellerId" :min="3001" /></el-form-item>
        <el-form-item label="评分"><el-input-number v-model="form.rating" :min="0" :max="5" :precision="0.1" /></el-form-item>
        <el-form-item label="新品"><el-switch v-model="form.isNew" /></el-form-item>
        <el-form-item label="热门"><el-switch v-model="form.isHot" /></el-form-item>
        <el-form-item label="秒杀"><el-switch v-model="form.isSeckill" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getProductList, saveProduct, deleteProduct } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const form = ref<any>({ name: '', description: '', brand: '', categoryId: 111, sellerId: 3001, rating: 4.5, isNew: false, isHot: false, isSeckill: false })

async function fetchData() {
  loading.value = true
  try {
    const res = await getProductList({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    if (res.code === 200) {
      list.value = res.data.list
      total.value = res.data.total
    }
  } finally { loading.value = false }
}

function showDialog(row?: any) {
  form.value = row ? { ...row, isNew: !!row.isNew, isHot: !!row.isHot, isSeckill: !!row.isSeckill } : { name: '', description: '', brand: '', categoryId: 111, sellerId: 3001, rating: 4.5, isNew: false, isHot: false, isSeckill: false }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveProduct(form.value)
    if (res.code === 200) {
      ElMessage.success(form.value.id ? '商品已更新' : '商品已新增')
      dialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该商品？同时会删除关联的 SKU。', '确认删除', { type: 'warning' })
    const res = await deleteProduct(id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      fetchData()
    }
  } catch {}
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.admin-page { padding: 20px; }
.admin-page-title { font-size: 1.3rem; font-weight: 700; margin-bottom: 20px; }
.toolbar { display: flex; gap: 10px; align-items: center; }
</style>
