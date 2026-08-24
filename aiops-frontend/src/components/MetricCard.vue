<script setup lang="ts">
defineProps<{
  title: string
  value: string | number
  hint?: string
  tone?: 'blue' | 'green' | 'amber' | 'red'
}>()
</script>

<template>
  <section class="metric-card" :class="`metric-${tone || 'blue'}`">
    <div class="metric-head">
      <span>{{ title }}</span>
      <slot />
    </div>
    <strong>{{ value }}</strong>
    <p>{{ hint || '较昨日暂无变化' }}</p>
  </section>
</template>
