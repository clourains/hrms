<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <span class="logo-chip">
          <img :src="logoMark" alt="星跃出海" />
        </span>
        <span v-show="!isCollapse" class="logo-text">星跃出海国际贸易有限公司</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :default-openeds="defaultOpeneds"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        class="sidebar-menu"
      >
        <template v-for="group in visibleMenuGroups" :key="group.title">
          <!-- 顶级菜单项 -->
          <el-menu-item v-if="group.type === 'item'" :index="group.path">
            <el-icon><component :is="group.icon" /></el-icon>
            <template #title>{{ group.title }}</template>
          </el-menu-item>
          <!-- 分组菜单（children 过滤后为空的组不再渲染） -->
          <el-sub-menu v-else-if="group.children.length > 0" :index="group.title">
            <template #title>
              <el-icon><component :is="group.icon" /></el-icon>
              <span>{{ group.title }}</span>
            </template>
            <el-menu-item
              v-for="child in group.children"
              :key="child.path"
              :index="child.path"
            >
              <el-icon><component :is="child.icon" /></el-icon>
              <template #title>{{ child.title }}</template>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <!-- 头部 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="toggleCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tooltip content="搜索" placement="bottom">
            <el-icon class="header-icon" :size="18" @click="openSearch"><Search /></el-icon>
          </el-tooltip>
          <!-- 站内消息（P2-1） -->
          <el-popover
            v-model:visible="msgVisible"
            placement="bottom-end"
            :width="380"
            trigger="click"
            @show="loadRecentMessages"
          >
            <template #reference>
              <el-badge :value="msgUnread" :max="99" :hidden="msgUnread === 0" class="header-icon">
                <el-icon :size="18"><ChatDotRound /></el-icon>
              </el-badge>
            </template>
            <div class="notice-panel">
              <div class="notice-head">
                <span>站内消息</span>
                <div>
                  <el-button link type="primary" @click="msgReadAll">全部已读</el-button>
                  <el-button link type="primary" @click="goMessageCenter">查看全部</el-button>
                </div>
              </div>
              <el-empty v-if="msgList.length === 0" description="暂无消息" :image-size="56" />
              <div v-else class="notice-list">
                <div
                  v-for="m in msgList"
                  :key="m.id"
                  class="notice-item"
                  :class="{ 'msg-unread': m.isRead === 0 }"
                  @click="openMessage(m)"
                >
                  <div class="notice-title">
                    <el-tag
                      :type="m.msgType === '审批' ? 'warning' : (m.msgType === '提醒' ? 'danger' : 'primary')"
                      size="small"
                      effect="plain"
                    >
                      {{ m.msgType }}
                    </el-tag>
                    <span class="msg-pop-title">{{ m.title }}</span>
                  </div>
                  <div class="msg-pop-content">{{ m.content }}</div>
                  <div class="msg-pop-time">{{ formatMsgTime(m.createTime) }}</div>
                </div>
                <div v-if="msgUnread > 0" class="notice-more" @click="msgReadAll">
                  {{ msgUnread }} 条未读，点击全部标为已读
                </div>
              </div>
            </div>
          </el-popover>
          <el-popover
            v-model:visible="noticeVisible"
            placement="bottom-end"
            :width="360"
            trigger="click"
            @show="loadWarnings"
          >
            <template #reference>
              <el-badge :value="warningCount" :max="99" :hidden="warningCount === 0" class="header-icon">
                <el-icon :size="18"><Bell /></el-icon>
              </el-badge>
            </template>
            <div class="notice-panel">
              <div class="notice-head">
                <span>库存预警</span>
                <el-button link type="primary" @click="goWarning">查看全部</el-button>
              </div>
              <el-empty v-if="warnings.length === 0" description="暂无预警" :image-size="56" />
              <div v-else class="notice-list">
                <div v-for="w in warnings.slice(0, 6)" :key="w.id" class="notice-item" @click="goWarning">
                  <div class="notice-title">
                    {{ w.productName || '未知产品' }}
                    <span class="notice-sku">{{ w.sku }}</span>
                  </div>
                  <div class="notice-sub">
                    {{ w.warehouseName }} · 可用 {{ w.availableQuantity }} / 预警值 {{ w.warningQuantity }}
                  </div>
                </div>
                <div v-if="warnings.length > 6" class="notice-more" @click="goWarning">
                  还有 {{ warnings.length - 6 }} 条，点击查看全部
                </div>
              </div>
            </div>
          </el-popover>
          <el-tooltip content="全屏" placement="bottom">
            <el-icon class="header-icon" :size="18" @click="toggleFullscreen">
              <FullScreen />
            </el-icon>
          </el-tooltip>

          <el-dropdown @command="handleCommand" class="user-dropdown">
            <div class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ userStore.username ? userStore.username.charAt(0).toUpperCase() : 'U' }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo.realName || userStore.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <AiChat />

    <!-- 全局搜索（P022） -->
    <el-dialog v-model="searchVisible" title="搜索" width="520px" append-to-body>
      <el-input
        ref="searchInputRef"
        v-model="searchKeyword"
        placeholder="输入页面名称，如 库存 / 物流 / 订单"
        prefix-icon="Search"
        clearable
      />
      <div class="search-list">
        <div
          v-for="m in filteredMenus"
          :key="m.path"
          class="search-item"
          @click="goMenu(m.path)"
        >
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </div>
        <el-empty v-if="filteredMenus.length === 0" description="没有匹配的页面" :image-size="56" />
      </div>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getInventoryWarningApi, getInventoryWarningCountApi } from '@/api/trade'
import {
  getMessageInboxApi,
  getUnreadCountApi,
  markMessageReadApi,
  readAllMessagesApi
} from '@/api/message'
import {
  User,
  OfficeBuilding,
  Calendar,
  Money,
  TrendCharts,
  DataAnalysis,
  Fold,
  Expand,
  Search,
  Bell,
  FullScreen,
  ArrowDown,
  Lock,
  SwitchButton,
  ChatDotRound
} from '@element-plus/icons-vue'
import AiChat from '@/components/AiChat.vue'
import logoMark from '@/assets/logo-mark.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)

// 企业级菜单规划：首页/仪表盘置顶，人事、业务各归其组，可视化分析统一放到底部
// adminOnly: true 的菜单仅管理员可见（P045 角色权限）
const menuGroups = [
  { type: 'item', path: '/home', title: '首页', icon: 'HomeFilled' },
  { type: 'item', path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
  {
    type: 'group',
    title: '人事管理',
    icon: 'Avatar',
    children: [
      { path: '/employee', title: '员工管理', icon: 'User', adminOnly: true },
      { path: '/department', title: '部门管理', icon: 'OfficeBuilding', adminOnly: true },
      { path: '/attendance', title: '考勤管理', icon: 'Calendar' },
      { path: '/salary', title: '薪资管理', icon: 'Money' },
      { path: '/performance', title: '绩效管理', icon: 'TrendCharts' },
      { path: '/message', title: '消息中心', icon: 'ChatDotRound' },
      { path: '/reminder', title: '到期提醒', icon: 'Bell', adminOnly: true },
      { path: '/audit', title: '审计日志', icon: 'Document', adminOnly: true }
    ]
  },
  {
    type: 'group',
    title: '业务管理',
    icon: 'Suitcase',
    adminOnly: true,
    children: [
      { path: '/order', title: '销售订单', icon: 'Tickets' },
      { path: '/product', title: '产品管理', icon: 'Goods' },
      { path: '/warehouse', title: '仓库库存', icon: 'Box' },
      { path: '/customer', title: '境外客户', icon: 'UserFilled' },
      { path: '/supplier', title: '供应商', icon: 'Van' },
      { path: '/shipment', title: '物流跟踪', icon: 'Ship' },
      { path: '/payment', title: '收付款', icon: 'Wallet' }
    ]
  }
]

// 按当前登录角色过滤菜单（P045）
const visibleMenuGroups = computed(() => {
  const isAdmin = userStore.role === 'admin'
  const groups = []
  for (const group of menuGroups) {
    if (group.adminOnly && !isAdmin) continue
    if (group.type === 'group') {
      const children = group.children.filter(child => !child.adminOnly || isAdmin)
      groups.push({ ...group, children })
    } else {
      groups.push(group)
    }
  }
  return groups
})

// 全局搜索用的扁平菜单列表（原代码引用了未定义的 menuItems，这里补上并按角色过滤）
const menuItems = computed(() => {
  const items = []
  for (const group of visibleMenuGroups.value) {
    if (group.type === 'item') {
      items.push({ title: group.title, path: group.path, icon: group.icon })
    } else {
      for (const child of group.children) {
        items.push({ title: child.title, path: child.path, icon: child.icon })
      }
    }
  }
  return items
})

// 进入页面时自动展开当前路由所在的分组
const defaultOpeneds = computed(() => {
  const opened = []
  for (const group of menuGroups) {
    if (group.type === 'group' && group.children.some(child => child.path === route.path)) {
      opened.push(group.title)
    }
  }
  return opened
})

const activeMenu = computed(() => route.path)
const currentPageTitle = computed(() => route.meta?.title || '')

// ========== 顶部搜索（P022） ==========
const searchVisible = ref(false)
const searchKeyword = ref('')
const searchInputRef = ref(null)

const filteredMenus = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return menuItems.value
  return menuItems.value.filter(m => m.title.toLowerCase().includes(kw) || m.path.toLowerCase().includes(kw))
})

const openSearch = () => {
  searchKeyword.value = ''
  searchVisible.value = true
  nextTick(() => searchInputRef.value?.focus())
}

const goMenu = (path) => {
  searchVisible.value = false
  if (route.path === path) return
  router.push(path)
}

// ========== 顶部通知：库存预警（P022） ==========
const noticeVisible = ref(false)
const warningCount = ref(0)
const warnings = ref([])

const loadWarnings = async () => {
  try {
    const res = await getInventoryWarningApi()
    warnings.value = res.data || []
    warningCount.value = warnings.value.length
  } catch (e) {
    warnings.value = []
    warningCount.value = 0
  }
}

const goWarning = () => {
  noticeVisible.value = false
  // 库存预警页已合并到"库存总览"，这里直接带筛选条件过去
  router.push({ path: '/warehouse', query: { tab: 'inventory', lowStock: '1' } })
}

const refreshWarningCount = async () => {
  try {
    const res = await getInventoryWarningCountApi()
    warningCount.value = res.data || 0
  } catch (e) {
    // 未登录或接口异常时不显示红点即可
  }
}

onMounted(refreshWarningCount)

// 切换页面时刷新红点，避免库存变动后数字不更新
watch(() => route.path, () => {
  refreshWarningCount()
  refreshMsgCount()
})

// ========== 顶部通知：站内消息（P2-1） ==========
const msgVisible = ref(false)
const msgUnread = ref(0)
const msgList = ref([])

const formatMsgTime = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

const refreshMsgCount = async () => {
  try {
    const res = await getUnreadCountApi()
    msgUnread.value = Number(res.data || 0)
  } catch (e) {
    // 未登录或接口异常时不显示红点即可
  }
}

const loadRecentMessages = async () => {
  try {
    const res = await getMessageInboxApi({})
    msgList.value = (res.data || []).slice(0, 8)
  } catch (e) {
    msgList.value = []
  }
}

const openMessage = async (m) => {
  if (m.isRead === 0) {
    try {
      await markMessageReadApi(m.id)
      m.isRead = 1
      await refreshMsgCount()
    } catch (e) {
      // 忽略
    }
  }
  msgVisible.value = false
  router.push('/message')
}

const msgReadAll = async () => {
  try {
    await readAllMessagesApi()
    ElMessage.success('已全部标记为已读')
    msgList.value = msgList.value.map(m => ({ ...m, isRead: 1 }))
    msgUnread.value = 0
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

const goMessageCenter = () => {
  msgVisible.value = false
  router.push('/message')
}

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      ElMessage.info('个人中心功能开发中')
      break
    case 'password':
      ElMessage.info('修改密码功能开发中')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await userStore.logout()
        ElMessage.success('退出成功')
        router.push('/login')
      } catch (error) {
        // 用户取消
      }
      break
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

/*
 * 右侧主区必须能"收缩"
 * flex 子项默认 min-height:auto，会被内容撑到实际高度；
 * 外层又有 overflow:hidden，超出 100vh 的部分直接被裁掉，
 * 而且 el-main 自身高度等于内容高度，overflow-y:auto 永远不触发 → 滚轮失效。
 * 加 min-height:0 后 el-main 才被限制在视口内，内部滚动条才出现。
 */
.main-container {
  min-height: 0;
  overflow: hidden;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 8px;
  color: #fff;
  background-color: #2b3648;
  border-bottom: 1px solid #1f2d3d;
  overflow: hidden;
}

.logo-chip {
  width: 32px;
  height: 32px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 8px;

  img {
    width: 24px;
    height: 24px;
    object-fit: contain;
  }
}

.logo-text {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

/*
 * 菜单自己滚动
 * 原来写死 height: calc(100vh - 60px) 且外层 overflow:hidden，
 * 菜单项一多就整块被裁掉、也没有滚动条，所以下面的供应商/物流追踪看不到。
 */
.sidebar-menu {
  border-right: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;

  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.18);
    border-radius: 3px;
  }
  &::-webkit-scrollbar-thumb:hover {
    background: rgba(255, 255, 255, 0.32);
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
  transition: color 0.3s;

  &:hover {
    color: #409EFF;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-icon {
  cursor: pointer;
  color: #606266;
  transition: color 0.3s;

  &:hover {
    color: #409EFF;
  }
}

.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 10px;
  height: 60px;

  &:hover {
    background-color: #f5f7fa;
  }
}

.user-avatar {
  background-color: #409EFF;
  color: #fff;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  min-height: 0;
  overflow-y: auto;
}

/* 顶部通知面板 */
.notice-panel { max-height: 420px; overflow-y: auto; }
.notice-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.notice-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover { background-color: #f5f7fa; }
}
.notice-title { font-size: 13px; color: #303133; font-weight: 500; }
.notice-sku { color: #909399; font-weight: 400; margin-left: 6px; font-size: 12px; }
.notice-sub { font-size: 12px; color: #F56C6C; margin-top: 2px; }
.notice-more { text-align: center; font-size: 12px; color: #409EFF; padding: 8px 0; cursor: pointer; }

/* 站内消息弹层（P2-1） */
.msg-unread { background-color: #f0f7ff; }
.msg-pop-title { font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.msg-pop-content {
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.msg-pop-time { font-size: 12px; color: #c0c4cc; margin-top: 4px; }

/* 全局搜索列表 */
.search-list { margin-top: 12px; max-height: 360px; overflow-y: auto; }
.search-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #303133;

  &:hover { background-color: #f5f7fa; color: #409EFF; }
}

/* 路由过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
