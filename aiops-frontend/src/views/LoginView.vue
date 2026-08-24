<script setup lang="ts">
import { LockKeyhole, UserRound } from 'lucide-vue-next'
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const { t } = useI18n()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: ''
})

const submit = async () => {
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    await router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <div class="brand" style="height: auto; padding: 0">
        <span class="brand-mark">AI</span>
        <span>{{ t('common.appName') }}</span>
      </div>
      <h1 class="login-title">{{ t('login.title') }}</h1>
      <p class="login-hint">{{ t('login.hint') }}</p>

      <el-form label-position="top" @keyup.enter="submit">
        <el-form-item :label="t('login.username')">
          <el-input v-model="form.username" size="large" :placeholder="t('login.usernamePlaceholder')">
            <template #prefix>
              <UserRound :size="16" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('login.password')">
          <el-input v-model="form.password" size="large" type="password" show-password :placeholder="t('login.passwordPlaceholder')">
            <template #prefix>
              <LockKeyhole :size="16" />
            </template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          {{ t('login.submit') }}
        </el-button>
      </el-form>
    </section>
  </main>
</template>
