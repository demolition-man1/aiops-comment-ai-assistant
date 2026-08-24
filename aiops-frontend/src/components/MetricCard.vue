<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{
  title: string
  value: string | number
  hint?: string
  tone?: 'blue' | 'green' | 'amber' | 'red'
}>()

const { t } = useI18n()
</script>

<template>
  <section class="metric-card" :class="`metric-${tone || 'blue'}`">
    <div class="metric-head">
      <span>{{ title }}</span>
      <slot />
    </div>
    <strong>{{ value }}</strong>
    <p>{{ hint || t('dashboard.noDailyChange') }}</p>
  </section>
</template>
