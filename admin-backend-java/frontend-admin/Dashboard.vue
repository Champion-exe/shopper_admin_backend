<template>
  <div class="admin-dashboard">
    <h2 class="admin-page-title">数据看板</h2>

    <!-- 统计卡片 — 点击跳转 -->
    <div class="stat-cards">
      <div class="stat-card clickable" @click="$router.push('/admin/products')">
        <div class="stat-card-icon" style="background:#FFF0E6;color:#E1251B">
          <el-icon :size="28"><Goods /></el-icon>
        </div>
        <div class="stat-card-info">
          <span class="stat-card-value">{{ stats.productCount }}</span>
          <span class="stat-card-label">商品总数 →</span>
        </div>
      </div>
      <div class="stat-card clickable" @click="$router.push('/admin/orders')">
        <div class="stat-card-icon" style="background:#FFF0E6;color:#FF6A00">
          <el-icon :size="28"><List /></el-icon>
        </div>
        <div class="stat-card-info">
          <span class="stat-card-value">{{ stats.todayOrders }}</span>
          <span class="stat-card-label">今日订单 →</span>
        </div>
      </div>
      <div class="stat-card clickable" @click="$router.push('/admin/users')">
        <div class="stat-card-icon" style="background:#FFF0E6;color:#00B578">
          <el-icon :size="28"><User /></el-icon>
        </div>
        <div class="stat-card-info">
          <span class="stat-card-value">{{ stats.userCount }}</span>
          <span class="stat-card-label">用户总数 →</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card-icon" style="background:#FFF0E6;color:#2575FC">
          <el-icon :size="28"><Coin /></el-icon>
        </div>
        <div class="stat-card-info">
          <span class="stat-card-value">&yen;{{ stats.todayRevenue }}</span>
          <span class="stat-card-label">今日收入</span>
        </div>
      </div>
    </div>

    <!-- 订单概览 + 最近订单 -->
    <div class="dashboard-grid">
      <div class="panel">
        <h3 class="panel-title">订单概览</h3>
        <div class="order-summary">
          <div class="order-item" v-for="s in orderSummary" :key="s.label">
            <span class="order-label">{{ s.label }}</span>
            <span class="order-count" :style="{color:s.color}">{{ s.count }}</span>
          </div>
        </div>
      </div>
      <div class="panel">
        <h3 class="panel-title" style="cursor:pointer" @click="$router.push('/admin/orders')">最近订单 →</h3>
        <el-table :data="recentOrders" size="small" stripe>
          <el-table-column prop="orderNo" label="订单号" width="160" />
          <el-table-column prop="consignee" label="客户" width="70" />
          <el-table-column label="金额" width="90">
            <template #default="{row}">&yen;{{ row.payAmount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{row}">
              <el-tag :type="row.status==='completed'?'success':row.status==='cancelled'?'info':''" size="small">
                {{ statusMap[row.status] || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="150" />
        </el-table>
      </div>
    </div>

    <el-alert title="Spring Boot 3 管理后台 端口9090" type="success" :closable="false" show-icon style="margin-top:20px" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboard, getOrderList } from '@/api/admin'
import { Goods, List, User, Coin } from '@element-plus/icons-vue'

const stats = ref({ productCount: '-', todayOrders: '-', userCount: '-', todayRevenue: '0' })
const recentOrders = ref<any[]>([])
const orderSummary = ref<any[]>([])
const statusMap: Record<string,string> = {
  pending_payment:'待支付',pending_ship:'待发货',pending_receipt:'待收货',completed:'已完成',cancelled:'已取消'
}

onMounted(async () => {
  try {
    const res = await getDashboard()
    if (res.code === 200) stats.value = res.data
  } catch (e) { console.error(e) }

  // 获取所有订单统计
  try {
    const res = await getOrderList({ page: 1, size: 100 })
    if (res.code === 200) {
      const orders = res.data.list
      recentOrders.value = orders.slice(0, 5)
      const counts: Record<string,number> = {}
      orders.forEach((o:any) => {
        counts[o.status] = (counts[o.status] || 0) + 1
      })
      orderSummary.value = [
        { label:'待支付', count:counts['pending_payment']||0, color:'#E6A23C' },
        { label:'待发货', count:counts['pending_ship']||0, color:'#409EFF' },
        { label:'待收货', count:counts['pending_receipt']||0, color:'#909399' },
        { label:'已完成', count:counts['completed']||0, color:'#67C23A' },
        { label:'已取消', count:counts['cancelled']||0, color:'#F56C6C' },
      ]
    }
  } catch (e) { console.error(e) }
})
</script>

<style scoped lang="scss">
.admin-dashboard { padding: 20px; }
.admin-page-title { font-size: 1.3rem; font-weight: 700; margin-bottom: 24px; }

.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { display: flex; align-items: center; gap: 16px; background: white; border-radius: 12px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.stat-card.clickable { cursor: pointer; transition: box-shadow 0.2s; }
.stat-card.clickable:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
.stat-card-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.stat-card-info { display: flex; flex-direction: column; }
.stat-card-value { font-size: 1.5rem; font-weight: 700; }
.stat-card-label { font-size: 0.85rem; color: var(--color-text-secondary); }

.dashboard-grid { display: grid; grid-template-columns: 1fr 2fr; gap: 20px; }
.panel { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.panel-title { font-size: 1rem; font-weight: 700; margin: 0 0 16px 0; }

.order-summary { display: flex; flex-direction: column; gap: 14px; }
.order-item { display: flex; justify-content: space-between; align-items: center; }
.order-label { color: var(--color-text-secondary); }
.order-count { font-size: 1.3rem; font-weight: 700; }
</style>
