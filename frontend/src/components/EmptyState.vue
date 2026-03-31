<template>
  <div class="empty-state">
    <div class="empty-icon">
      <el-icon :size="iconSize"><component :is="icon" /></el-icon>
    </div>
    <div class="empty-title">{{ title }}</div>
    <div v-if="description" class="empty-desc">{{ description }}</div>
    <div v-if="action" class="empty-action">
      <el-button type="primary" @click="$emit('action')">{{ action }}</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  title?: string
  description?: string
  action?: string
  icon?: string
  size?: 'small' | 'medium' | 'large'
}>(), {
  title: '暂无数据',
  icon: 'Tickets',
  size: 'medium',
})

defineEmits<{ action: [] }>()

const iconSize = computed(() => ({
  small: 32,
  medium: 48,
  large: 64,
}[props.size]))
</script>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;

  .empty-icon {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: #f5f7fa;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #c0c4cc;
    margin-bottom: 16px;
  }

  .empty-title {
    font-size: 15px;
    font-weight: 500;
    color: #606266;
    margin-bottom: 6px;
  }

  .empty-desc {
    font-size: 13px;
    color: #909399;
    margin-bottom: 16px;
    max-width: 320px;
  }
}
</style>
