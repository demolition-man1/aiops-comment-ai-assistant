<script setup lang="ts">
import {
  BarChart3,
  Bell,
  Bot,
  ClipboardList,
  FileUp,
  GitCompareArrows,
  Home,
  LogOut,
  MessageSquareText,
  PenLine,
  RefreshCw,
  Settings,
  UserRound
} from 'lucide-vue-next'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const navItems = [
  { path: '/', label: '商家驾驶舱', icon: Home },
  { path: '/comments', label: '评论分析', icon: MessageSquareText },
  { path: '/import', label: '数据导入', icon: FileUp },
  { path: '/compare', label: '商品对比', icon: GitCompareArrows },
  { path: '/content', label: 'AI文案', icon: PenLine },
  { path: '/alerts', label: '告警中心', icon: Bell },
  { path: '/settings', label: '系统设置', icon: Settings }
]

const pageName = computed(() => navItems.find((item) => item.path === route.path)?.label || 'AI智能运营助手')

const logout = async () => {
  auth.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">AI</span>
        <span>AI智能运营助手</span>
      </div>

      <nav class="nav-list">
        <router-link v-for="item in navItems" :key="item.path" class="nav-link" :to="item.path">
          <component :is="item.icon" :size="19" />
          <span>{{ item.label }}</span>
        </router-link>

        <div style="flex: 1" />
      </nav>
    </aside>

    <main class="shell-main">
      <header class="topbar">
        <div>
          <h1 class="page-title">{{ pageName }}</h1>
          <p class="page-subtitle">评论驱动运营决策，让 AI 帮你看懂用户反馈</p>
        </div>
        <div class="toolbar-actions">
          <el-button :icon="RefreshCw">定时同步</el-button>
          <el-button :icon="ClipboardList">任务中心</el-button>
          <el-button :icon="BarChart3">数据报表</el-button>
          <el-dropdown>
            <el-button>
              <UserRound :size="16" />
              {{ auth.user?.nickname || auth.user?.username || '商家用户' }}
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <Bot :size="14" />
                  运营助手在线
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <LogOut :size="14" />
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <router-view />
    </main>
  </div>
</template>
