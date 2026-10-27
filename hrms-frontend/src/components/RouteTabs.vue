<template>
  <div class="route-tabs" @contextmenu="onContextMenu">
    <el-tabs
      v-model="activeTab"
      type="card"
      class="tabs-inner"
      @tab-click="handleClick"
      @tab-remove="handleRemove"
    >
      <el-tab-pane
        v-for="t in tabs"
        :key="t.path"
        :name="t.path"
        :label="t.title"
        :closable="!t.affix"
      />
    </el-tabs>

    <el-tooltip content="刷新当前页" placement="top">
      <el-icon class="tabs-refresh" :size="14" @click="refreshCurrent"><RefreshRight /></el-icon>
    </el-tooltip>

    <!-- 页签右键菜单 -->
    <teleport to="body">
      <div
        v-if="ctxVisible"
        class="tab-ctx-menu"
        :style="{ left: ctxX + 'px', top: ctxY + 'px' }"
      >
        <div class="ctx-item" @click="doRefresh">刷新当前页</div>
        <div class="ctx-item" @click="closeOthers">关闭其他</div>
        <div class="ctx-item ctx-danger" @click="closeAll">全部关闭</div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const activeTab = ref(route.path)
const tabs = ref([{ path: '/home', title: '首页', affix: true }])

// 路由变化：登记页签 + 同步选中项
watch(() => route.path, (p) => {
  if (route.meta?.title && !tabs.value.some(t => t.path === p)) {
    tabs.value.push({ path: p, title: route.meta.title, affix: p === '/home' })
  }
  activeTab.value = p
}, { immediate: true })

const handleClick = (pane) => {
  const path = pane.props.name
  if (path && path !== route.path) router.push(path)
}

const handleRemove = (name) => {
  const idx = tabs.value.findIndex(t => t.path === name)
  if (idx < 0 || tabs.value[idx].affix) return
  tabs.value.splice(idx, 1)
  // 关闭的是当前页：跳到相邻页签
  if (name === route.path) {
    const next = tabs.value[idx] || tabs.value[idx - 1]
    router.push(next ? next.path : '/home')
  }
}

// ========== 右键菜单 ==========
const ctxVisible = ref(false)
const ctxX = ref(0)
const ctxY = ref(0)

const onContextMenu = (e) => {
  const item = e.target.closest('.el-tabs__item')
  if (!item) return
  e.preventDefault()
  const name = (item.id || '').replace(/^tab-/, '')
  if (!name) return
  // 右键的页签设为当前页
  if (name !== route.path) router.push(name)
  ctxX.value = Math.min(e.clientX, window.innerWidth - 130)
  ctxY.value = e.clientY + 6
  ctxVisible.value = true
}

const closeCtx = () => {
  ctxVisible.value = false
}

const doRefresh = () => {
  closeCtx()
  appStore.reloadView()
}

const closeOthers = () => {
  closeCtx()
  tabs.value = tabs.value.filter(t => t.affix || t.path === route.path)
}

const closeAll = () => {
  closeCtx()
  tabs.value = tabs.value.filter(t => t.affix)
  if (route.path !== '/home') router.push('/home')
}

const refreshCurrent = () => {
  appStore.reloadView()
}

onMounted(() => document.addEventListener('click', closeCtx))
onUnmounted(() => document.removeEventListener('click', closeCtx))
</script>

<style scoped lang="scss">
.route-tabs {
  display: flex;
  align-items: center;
  flex: none;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
  padding: 0 12px;
  height: 38px;
  user-select: none;
}

.tabs-inner {
  flex: 1;
  min-width: 0;

  :deep(.el-tabs__header) {
    margin: 0;
    border-bottom: none;
  }

  :deep(.el-tabs__nav) {
    border: none;
  }

  :deep(.el-tabs__item) {
    height: 28px;
    line-height: 28px;
    margin-top: 5px;
    margin-bottom: 4px;
    border: 1px solid transparent !important;
    border-radius: 4px;
    font-size: 13px;
    color: var(--el-text-color-regular);
    transition: color 0.15s, background-color 0.15s;

    &:hover {
      color: var(--el-color-primary);
    }

    &.is-active {
      background: var(--el-color-primary);
      color: #fff;
      border-color: var(--el-color-primary) !important;
      font-weight: 500;

      /* 蓝底上的关闭按钮同步白色 */
      .is-icon-close {
        color: rgba(255, 255, 255, 0.85);

        &:hover {
          color: #fff;
          background: rgba(255, 255, 255, 0.2);
        }
      }
    }

    .is-icon-close {
      margin-top: -1px;
    }
  }
}

.tabs-refresh {
  flex: none;
  margin-left: 8px;
  padding: 5px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--el-text-color-regular);
  transition: color 0.15s, background-color 0.15s;

  &:hover {
    color: var(--el-color-primary);
    background: var(--el-fill-color-light);
  }
}
</style>

<style lang="scss">
/* 右键菜单渲染在 body 下，不能 scoped */
.tab-ctx-menu {
  position: fixed;
  z-index: 3000;
  min-width: 120px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  padding: 4px;

  .ctx-item {
    padding: 7px 14px;
    font-size: 13px;
    color: var(--el-text-color-primary);
    border-radius: 4px;
    cursor: pointer;

    &:hover {
      background: var(--el-fill-color-light);
      color: var(--el-color-primary);
    }

    &.ctx-danger:hover {
      color: var(--el-color-danger);
    }
  }
}
</style>
