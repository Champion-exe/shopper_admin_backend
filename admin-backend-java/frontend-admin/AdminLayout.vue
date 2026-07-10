<template>
  <div class="admin-layout">
    <el-container>
      <el-aside width="220px" class="admin-sidebar">
        <div class="admin-logo">
          <span class="logo-icon">S</span>
          <span class="logo-text">Shopper 管理</span>
        </div>
        <el-menu
          :default-active="route.path"
          router
          class="admin-menu"
          background-color="#2c2c2c"
          text-color="#ccc"
          active-text-color="#E1251B"
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据看板</span>
          </el-menu-item>
          <el-menu-item index="/admin/products">
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/orders">
            <el-icon><List /></el-icon>
            <span>订单管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu>
        <div class="admin-logout">
          <el-button type="danger" plain size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </el-aside>
      <el-container>
        <el-header class="admin-header">
          <span class="admin-title">后台管理系统</span>
          <div class="admin-user">
            <el-icon><User /></el-icon>
            <span>{{ adminUser?.nickname || '管理员' }}</span>
          </div>
        </el-header>
        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const adminUser = ref<any>(null)

const stored = localStorage.getItem('adminUser')
if (stored) {
  try {
    const data = JSON.parse(stored)
    adminUser.value = data.user || data
  } catch (e) {}
} else if (userStore.user) {
  adminUser.value = userStore.user
}

function handleLogout() {
  userStore.logout()
  localStorage.removeItem('adminUser')
  router.push('/admin/login')
}
</script>

<style scoped lang="scss">
.admin-layout {
  height: 100vh;

  .admin-sidebar {
    background: #2c2c2c;
    display: flex;
    flex-direction: column;
  }

  .admin-logo {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 20px;
    border-bottom: 1px solid #444;

    .logo-icon {
      width: 32px;
      height: 32px;
      background: var(--color-promotion-gradient);
      color: white;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 900;
    }

    .logo-text {
      color: white;
      font-weight: 700;
      font-size: 1.1rem;
    }
  }

  .admin-menu {
    border-right: none;
    flex: 1;
  }

  .admin-logout {
    padding: 16px;
    text-align: center;
    border-top: 1px solid #444;
  }

  .admin-header {
    background: white;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.08);

    .admin-title {
      font-weight: 700;
      font-size: 1.1rem;
    }

    .admin-user {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--color-text-secondary);
    }
  }

  .admin-main {
    background: #f5f5f5;
    min-height: calc(100vh - 60px);
  }
}
</style>
