<script setup lang="ts">
import {
  BarChart3,
  Bell,
  Bot,
  Boxes,
  ClipboardList,
  FileUp,
  GitCompareArrows,
  Globe2,
  Home,
  LogOut,
  MessageSquareText,
  PenLine,
  RefreshCw,
  Tags,
  Lightbulb,
  Settings,
  UserRound
} from 'lucide-vue-next'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'

import type { AppLocale } from '@/i18n'
import { useAuthStore } from '@/stores/auth'
import { useLocaleStore } from '@/stores/locale'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const localeStore = useLocaleStore()
const { t } = useI18n()

const navItems = [
  { path: '/', labelKey: 'layout.nav.dashboard', icon: Home },
  { path: '/comments', labelKey: 'layout.nav.comments', icon: MessageSquareText },
  { path: '/tags', labelKey: 'layout.nav.tags', icon: Tags },
  { path: '/solutions', labelKey: 'layout.nav.solutions', icon: Lightbulb },
  { path: '/prompts', labelKey: 'layout.nav.prompts', icon: Bot },
  { path: '/ai-logs', labelKey: 'layout.nav.aiLogs', icon: ClipboardList },
  { path: '/import', labelKey: 'layout.nav.import', icon: FileUp },
  { path: '/categories', labelKey: 'layout.nav.categories', icon: Boxes },
  { path: '/compare', labelKey: 'layout.nav.compare', icon: GitCompareArrows },
  { path: '/content', labelKey: 'layout.nav.content', icon: PenLine },
  { path: '/alerts', labelKey: 'layout.nav.alerts', icon: Bell },
  { path: '/settings', labelKey: 'layout.nav.settings', icon: Settings }
]

const pageItems = [
  ...navItems,
  { path: '/sync', labelKey: 'layout.actions.scheduledSync' },
  { path: '/tasks', labelKey: 'layout.actions.taskCenter' },
  { path: '/reports', labelKey: 'layout.actions.dataReports' }
]

const pageName = computed(() => {
  const item = pageItems.find((pageItem) => pageItem.path === route.path)
  return item ? t(item.labelKey) : t('common.appName')
})

const changeLocale = (value: string | number | boolean | object | undefined) => {
  if (typeof value === 'string') {
    localeStore.setLocale(value as AppLocale)
  }
}

const goScheduledSync = async () => {
  await router.push({ path: '/sync' })
}

const goTaskCenter = async () => {
  await router.push({ path: '/tasks' })
}

const goDataReports = async () => {
  await router.push({ path: '/reports' })
}

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
        <span>{{ t('common.appName') }}</span>
      </div>

      <nav class="nav-list">
        <router-link v-for="item in navItems" :key="item.path" class="nav-link" :to="item.path">
          <component :is="item.icon" :size="19" />
          <span>{{ t(item.labelKey) }}</span>
        </router-link>

        <div style="flex: 1" />
      </nav>
    </aside>

    <main class="shell-main">
      <header class="topbar">
        <div>
          <h1 class="page-title">{{ pageName }}</h1>
          <p class="page-subtitle">{{ t('layout.subtitle') }}</p>
        </div>
        <div class="toolbar-actions">
          <el-select
            :model-value="localeStore.locale"
            class="language-select"
            :aria-label="t('layout.language')"
            @change="changeLocale"
          >
            <template #prefix>
              <Globe2 :size="15" />
            </template>
            <el-option
              v-for="language in localeStore.languages"
              :key="language.code"
              :label="language.label"
              :value="language.code"
            />
          </el-select>
          <el-button :icon="RefreshCw" @click="goScheduledSync">{{ t('layout.actions.scheduledSync') }}</el-button>
          <el-button :icon="ClipboardList" @click="goTaskCenter">{{ t('layout.actions.taskCenter') }}</el-button>
          <el-button :icon="BarChart3" @click="goDataReports">{{ t('layout.actions.dataReports') }}</el-button>
          <el-dropdown>
            <el-button>
              <UserRound :size="16" />
              {{ auth.user?.nickname || auth.user?.username || t('layout.user.merchantUser') }}
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <Bot :size="14" />
                  {{ t('layout.user.assistantOnline') }}
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <LogOut :size="14" />
                  {{ t('layout.user.logout') }}
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
