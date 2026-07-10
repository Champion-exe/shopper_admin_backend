<template>
  <div class="admin-page">
    <h2 class="admin-page-title">订单管理</h2>

    <div class="toolbar">
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width:160px" @change="fetchData">
        <el-option label="待支付" value="pending_payment" />
        <el-option label="待发货" value="pending_ship" />
        <el-option label="待收货" value="pending_receipt" />
        <el-option label="已完成" value="completed" />
        <el-option label="已取消" value="cancelled" />
      </el-select>
    </div>

    <el-table :data="list" stripe v-loading="loading" style="margin-top:16px">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="consignee" label="收货人" width="80" />
      <el-table-column label="金额" width="100">
        <template #default="{ row }">&yen;{{ row.payAmount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="商品" min-width="200">
        <template #default="{ row }">
          <span v-for="(item, i) in row.items" :key="i">
            {{ item.productName }} x{{ item.quantity }}<span v-if="i < row.items.length - 1">, </span>
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="160" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending_ship'" size="small" type="primary" @click="handleShip(row.id)">发货</el-button>
          <el-button v-if="row.status !== 'completed' && row.status !== 'cancelled'" size="small" type="danger" @click="handleCancel(row.id)">取消</el-button>
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
import { getOrderList, shipOrder, cancelOrder } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref('')

const statusMap: Record<string, string> = {
  pending_payment: '待支付', pending_ship: '待发货',
  pending_receipt: '待收货', completed: '已完成', cancelled: '已取消',
}
function statusText(s: string) { return statusMap[s] || s }
function statusType(s: string) {
  return s === 'completed' ? 'success' : s === 'cancelled' ? 'info' : s === 'pending_payment' ? 'warning' : 'primary'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getOrderList({ page: page.value, size: size.value, status: statusFilter.value || undefined })
    if (res.code === 200) {
      list.value = res.data.list
      total.value = res.data.total
    }
  } finally { loading.value = false }
}

async function handleShip(id: number) {
  try {
    await ElMessageBox.confirm('确认发货？', '发货', { type: 'info' })
    const res = await shipOrder(id)
    if (res.code === 200) { ElMessage.success('已发货'); fetchData() }
  } catch {}
}

async function handleCancel(id: number) {
  try {
    await ElMessageBox.confirm('确定取消该订单？', '取消订单', { type: 'warning' })
    const res = await cancelOrder(id)
    if (res.code === 200) { ElMessage.success('已取消'); fetchData() }
  } catch {}
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.admin-page { padding: 20px; }
.admin-page-title { font-size: 1.3rem; font-weight: 700; margin-bottom: 20px; }
.toolbar { display: flex; gap: 10px; align-items: center; }
</style>
