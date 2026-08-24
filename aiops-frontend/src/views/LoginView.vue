<script setup lang="ts">
import { LockKeyhole, UserRound } from 'lucide-vue-next'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
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
        <span>AI智能运营助手</span>
      </div>
      <h1 class="login-title">商家运营后台</h1>
      <p class="login-hint">默认账号：admin。密码以本地初始化脚本或部署环境配置为准。</p>

      <el-form label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" size="large" placeholder="请输入用户名">
            <template #prefix>
              <UserRound :size="16" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码">
            <template #prefix>
              <LockKeyhole :size="16" />
            </template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          登录系统
        </el-button>
      </el-form>
    </section>
  </main>
</template>
