<template>
  <el-container class="app-layout">
    <el-aside width="200px">
      <div class="logo">AI Expense</div>
      <el-menu
        :default-active="currentRoute"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/bills">
          <span>账单管理</span>
        </el-menu-item>
        <el-menu-item index="/categories">
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/statistics">
          <span>月度统计</span>
        </el-menu-item>
        <el-menu-item index="/trends">
          <span>趋势分析</span>
        </el-menu-item>
        <el-menu-item index="/budget">
          <span>预算管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <span class="nickname">{{ userStore.user?.nickname || userStore.user?.email }}</span>
        <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
      </el-header>
      <el-main class="app-main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const currentRoute = computed(() => route.path)

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
}
.el-aside {
  background-color: #304156;
}
.logo {
  color: #fff;
  font-size: 18px;
  text-align: center;
  padding: 16px 0;
}
.app-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border-bottom: 1px solid #e6e6e6;
  gap: 12px;
}
.app-main {
  background-color: #f0f2f5;
}
</style>
