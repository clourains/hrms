<template>
  <el-drawer
    v-model="appStore.settingsVisible"
    title="系统设置"
    size="300px"
    :append-to-body="true"
  >
    <div class="settings-block">
      <div class="settings-label">主题模式</div>
      <div class="settings-value">
        <el-switch
          :model-value="appStore.isDark"
          inline-prompt
          active-text="暗黑"
          inactive-text="浅色"
          @change="(v) => appStore.setDark(v)"
        />
      </div>
    </div>

    <div class="settings-block">
      <div class="settings-label">主题色</div>
      <div class="color-grid">
        <div
          v-for="c in PRIMARY_PRESETS"
          :key="c"
          class="color-swatch"
          :class="{ active: appStore.primaryColor.toLowerCase() === c.toLowerCase() }"
          :style="{ background: c }"
          @click="appStore.setPrimary(c)"
        >
          <el-icon v-if="appStore.primaryColor.toLowerCase() === c.toLowerCase()" color="#fff"><Check /></el-icon>
        </div>
      </div>
    </div>

    <div class="settings-block">
      <el-button size="small" @click="appStore.setPrimary('#409EFF')">恢复默认</el-button>
    </div>

    <div class="settings-tip">
      设置会自动保存到本地，刷新页面后仍然生效。
    </div>
  </el-drawer>
</template>

<script setup>
import { Check } from '@element-plus/icons-vue'
import { useAppStore, PRIMARY_PRESETS } from '@/store/app'

const appStore = useAppStore()
</script>

<style scoped lang="scss">
.settings-block {
  margin-bottom: 24px;
}

.settings-label {
  font-size: 13px;
  color: var(--el-text-primary, var(--el-text-color-primary));
  font-weight: 500;
  margin-bottom: 12px;
}

.settings-value {
  display: flex;
}

.color-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.color-swatch {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid transparent;
  transition: transform 0.15s, border-color 0.15s;

  &:hover {
    transform: scale(1.08);
  }

  &.active {
    border-color: var(--el-text-color-primary);
  }
}

.settings-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 14px;
}
</style>
