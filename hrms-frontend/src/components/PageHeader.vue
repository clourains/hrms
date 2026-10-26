<template>
  <div class="page-header-card">
    <div class="page-header-left">
      <div v-if="icon" class="page-header-icon">
        <el-icon :size="22"><component :is="icon" /></el-icon>
      </div>
      <div class="page-header-text">
        <div class="page-header-title-row">
          <h2 class="page-header-title">{{ title }}</h2>
          <slot name="badge" />
        </div>
        <div v-if="description || $slots.description" class="page-header-desc">
          <slot name="description">{{ description }}</slot>
        </div>
      </div>
    </div>
    <div class="page-header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
// 页面统一页头：左侧图标+标题+描述，右侧主操作按钮区（art-design-pro 风格）
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  // 支持传图标组件或全局注册的图标名（字符串）
  icon: { type: [String, Object, Function], default: null }
})
</script>
