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
        @open="onGroupOpen"
        @close="onGroupClose"
      >
        <template v-for="group in visibleMenuGroups" :key="group.title">
          <!-- 顶级菜单项 -->
          <el-menu-item v-if="group.type === 'item'" :index="group.path">
            <el-icon><component :is="group.icon" /></el-icon>
            <template #title>
              <span class="menu-label">{{ group.title }}</span>
              <span v-if="menuBadges[group.path] > 0" class="menu-badge">{{ menuBadges[group.path] }}</span>
            </template>
          </el-menu-item>
          <!-- 分组菜单（children 过滤后为空的组不再渲染） -->
          <el-sub-menu v-else-if="group.children.length > 0" :index="group.title">
            <template #title>
              <el-icon class="group-icon">
                <component :is="group.icon" />
                <!-- 侧边栏整体折叠时：数字浮在图标右上角 -->
                <i v-if="isCollapse && groupTotal(group) > 0" class="menu-badge badge-float">
                  {{ groupTotal(group) }}
                </i>
              </el-icon>
              <span class="menu-label">{{ group.title }}</span>
              <!-- 侧边栏展开但这一组收起时：数字挂在组名右侧；组展开后不再显示，交给子项自己显示 -->
              <span
                v-if="!isCollapse && !openedGroups.includes(group.title) && groupTotal(group) > 0"
                class="menu-badge"
              >
                {{ groupTotal(group) }}
              </span>
            </template>
            <el-menu-item
              v-for="child in group.children"
              :key="child.path"
              :index="child.path"
            >
              <el-icon><component :is="child.icon" /></el-icon>
              <template #title>
                <span class="menu-label">{{ child.title }}</span>
                <span v-if="menuBadges[child.path] > 0" class="menu-badge">{{ menuBadges[child.path] }}</span>
              </template>
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
          <!--
            库存预警不再单独占一个顶部图标，
            改为在侧边栏「业务管理 / 仓库库存」上显示数字角标 + 仓库页内红字提示
          -->
          <el-tooltip content="主题模式" placement="bottom">
            <el-icon class="header-icon" :size="18" @click="appStore.toggleDark()">
              <Moon v-if="!appStore.isDark" />
              <Sunny v-else />
            </el-icon>
          </el-tooltip>
          <el-tooltip content="系统设置" placement="bottom">
            <el-icon class="header-icon" :size="18" @click="appStore.settingsVisible = true">
              <Setting />
            </el-icon>
          </el-tooltip>
          <el-tooltip content="锁屏" placement="bottom">
            <el-icon class="header-icon" :size="18" @click="appStore.lock()">
              <Lock />
            </el-icon>
          </el-tooltip>
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

      <!-- 多标签页导航 -->
      <RouteTabs />

      <!-- 主内容区 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" v-if="showView" :key="route.path" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <AiChat />

    <!-- 系统设置抽屉（主题模式/主题色） -->
    <SettingsDrawer />

    <!-- 锁屏遮罩 -->
    <LockScreen />

    <!-- 全局搜索（P022 菜单 + P7-1 员工/消息，Ctrl+K 唤起） -->
    <el-dialog v-model="searchVisible" title="全局搜索" width="560px" append-to-body>
      <el-input
        ref="searchInputRef"
        v-model="searchKeyword"
        placeholder="搜索页面 / 员工 / 消息（Ctrl+K）"
        prefix-icon="Search"
        clearable
      />
      <div class="search-list" v-loading="searchLoading" element-loading-text="搜索中...">
        <!-- 页面 -->
        <div v-if="filteredMenus.length > 0" class="search-group-title">页面</div>
        <div
          v-for="m in filteredMenus"
          :key="'menu-' + m.path"
          class="search-item"
          @click="goMenu(m.path)"
        >
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </div>

        <!-- 员工 -->
        <div v-if="empResults.length > 0" class="search-group-title">员工</div>
        <div
          v-for="e in empResults"
          :key="'emp-' + e.id"
          class="search-item"
          @click="goEmployee(e)"
        >
          <el-icon><User /></el-icon>
          <span>{{ e.name }}</span>
          <span class="search-item-sub">{{ e.empNo }}</span>
        </div>

        <!-- 消息 -->
        <div v-if="msgResults.length > 0" class="search-group-title">消息</div>
        <div
          v-for="msg in msgResults"
          :key="'msg-' + msg.id"
          class="search-item"
          @click="goMessageCenter"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span class="search-item-msg">{{ msg.title }}</span>
        </div>

        <el-empty
          v-if="searchKeyword && filteredMenus.length === 0 && empResults.length === 0 && msgResults.length === 0"
          description="没有匹配的结果"
          :image-size="56"
        />
      </div>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getInventoryWarningCountApi } from '@/api/trade'
import { getProbationPageApi } from '@/api/recruit'
import { PROBATION_EXPIRE_DAYS } from '@/utils/constants'
import {
  getMessageInboxApi,
  getUnreadCountApi,
  markMessageReadApi,
  readAllMessagesApi
} from '@/api/message'
import { getEmployeeListApi } from '@/api/employee'
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
  ChatDotRound,
  Setting,
  Moon,
  Sunny
} from '@element-plus/icons-vue'
import AiChat from '@/components/AiChat.vue'
import RouteTabs from '@/components/RouteTabs.vue'
import SettingsDrawer from '@/components/SettingsDrawer.vue'
import LockScreen from '@/components/LockScreen.vue'
import logoMark from '@/assets/logo-mark.png'
import { useAppStore } from '@/store/app'
import { messageJumpTarget } from '@/utils/messageJump'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const isCollapse = ref(false)

// 页签右键"刷新"触发：先卸载再重挂载 router-view，实现不刷新浏览器的页面重载
const showView = ref(true)
watch(() => appStore.refreshKey, async () => {
  showView.value = false
  await nextTick()
  showView.value = true
})

// 企业级菜单规划：首页/仪表盘置顶，人事、业务各归其组，可视化分析统一放到底部
// adminOnly: true 的菜单仅管理员可见（P045 角色权限）；
// roles: 数组指定可访问角色白名单（P4-3，如 manager 可见员工管理）
const menuGroups = [
  { type: 'item', path: '/home', title: '首页', icon: 'HomeFilled' },
  { type: 'item', path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
  {
    type: 'group',
    title: '人事管理',
    icon: 'Avatar',
    children: [
      { path: '/employee', title: '员工管理', icon: 'User', roles: ['admin', 'manager'] },
      { path: '/department', title: '部门管理', icon: 'OfficeBuilding', adminOnly: true },
      { path: '/attendance', title: '考勤管理', icon: 'Calendar' },
      { path: '/salary', title: '薪资管理', icon: 'Money' },
      { path: '/performance', title: '绩效管理', icon: 'TrendCharts' },
      { path: '/approval', title: '审批中心', icon: 'DocumentChecked' },
      { path: '/message', title: '消息中心', icon: 'ChatDotRound' },
      { path: '/reminder', title: '到期提醒', icon: 'Bell', adminOnly: true }
    ]
  },
  {
    type: 'group',
    title: '系统管理',
    icon: 'Setting',
    adminOnly: true,
    children: [
      { path: '/user', title: '用户管理', icon: 'User' },
      { path: '/notice', title: '公告管理', icon: 'Notification' },
      { path: '/loginLog', title: '登录日志', icon: 'Key' },
      { path: '/config', title: '参数配置', icon: 'Tools' },
      { path: '/audit', title: '审计日志', icon: 'Document' }
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
  },
  {
    type: 'group',
    title: '招聘管理',
    icon: 'Promotion',
    adminOnly: true,
    children: [
      { path: '/recruit/requisition', title: '招聘需求', icon: 'Promotion' },
      { path: '/recruit/candidate', title: '候选人', icon: 'UserFilled' },
      { path: '/recruit/interview', title: '面试安排', icon: 'Calendar' },
      { path: '/recruit/probation', title: '转正管理', icon: 'CircleCheck' }
    ]
  }
]

// 按当前登录角色过滤菜单（P045 / P4-3）
const visibleMenuGroups = computed(() => {
  const role = userStore.role
  const isAdmin = role === 'admin'
  const canSee = (item) => {
    if (item.roles) return item.roles.includes(role)
    if (item.adminOnly) return isAdmin
    return true
  }
  const groups = []
  for (const group of menuGroups) {
    if (!canSee(group)) continue
    if (group.type === 'group') {
      const children = group.children.filter(canSee)
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

// ========== 全局搜索（P022 菜单 + P7-1 员工/消息） ==========
const searchVisible = ref(false)
const searchKeyword = ref('')
const searchInputRef = ref(null)
const searchLoading = ref(false)
const empResults = ref([])
const msgResults = ref([])
let empCache = null
let searchTimer = null

const canSeeEmployee = computed(() => ['admin', 'manager'].includes(userStore.role))

const filteredMenus = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return menuItems.value
  return menuItems.value.filter(m => m.title.toLowerCase().includes(kw) || m.path.toLowerCase().includes(kw))
})

const openSearch = () => {
  searchKeyword.value = ''
  empResults.value = []
  msgResults.value = []
  searchVisible.value = true
  nextTick(() => searchInputRef.value?.focus())
}

// 关键词变化时防抖搜索员工（客户端过滤缓存）和消息（服务端模糊）
watch(searchKeyword, (kw) => {
  clearTimeout(searchTimer)
  const k = kw.trim()
  if (!k) {
    empResults.value = []
    msgResults.value = []
    return
  }
  searchTimer = setTimeout(() => doGlobalSearch(k), 300)
})

const doGlobalSearch = async (kw) => {
  searchLoading.value = true
  try {
    // 员工：admin/manager 可见，首次拉全量后本地过滤
    if (canSeeEmployee.value) {
      if (!empCache) {
        const res = await getEmployeeListApi()
        empCache = res.code === 200 ? (res.data || []) : []
      }
      empResults.value = empCache
        .filter(e => (e.name || '').includes(kw) || (e.empNo || '').toLowerCase().includes(kw.toLowerCase()))
        .slice(0, 5)
    }
    // 消息：服务端按关键字模糊
    const msgRes = await getMessageInboxApi({ keyword: kw })
    msgResults.value = msgRes.code === 200 ? (msgRes.data || []).slice(0, 5) : []
  } catch {
    // 搜索失败静默，不影响菜单搜索
  } finally {
    searchLoading.value = false
  }
}

const goEmployee = (emp) => {
  searchVisible.value = false
  router.push({ path: '/employee', query: { name: emp.name } })
}

const goMenu = (path) => {
  searchVisible.value = false
  if (route.path === path) return
  router.push(path)
}

// ========== 侧边栏菜单角标（库存不足 / 转正到期）==========
// 数字沿菜单树向上汇总：组收起时挂在组名（折叠态挂在图标）上，组展开后只在对应子项显示
const menuBadges = reactive({
  '/warehouse': 0,           // 库存不足条数
  '/recruit/probation': 0    // 临近到期 + 已逾期的试用期人数
})

// Ctrl+K / Cmd+K 唤起全局搜索
const onGlobalKeydown = (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    openSearch()
  }
}

const groupTotal = (group) => (group.children || [])
  .reduce((sum, child) => sum + (menuBadges[child.path] || 0), 0)

// 记录当前展开的分组，用于"展开了就不再在组名上重复显示数字"
const openedGroups = ref([...defaultOpeneds.value])

const onGroupOpen = (index) => {
  if (!openedGroups.value.includes(index)) {
    openedGroups.value.push(index)
  }
}

const onGroupClose = (index) => {
  openedGroups.value = openedGroups.value.filter(item => item !== index)
}

// 20 秒节流：切换路由频繁，避免每次跳页都打两个接口
let badgeFetchedAt = 0

const refreshMenuBadges = async (force = false) => {
  const now = Date.now()
  if (!force && now - badgeFetchedAt < 20000) {
    return
  }
  badgeFetchedAt = now

  // 库存不足：低于预警值的库存条数
  try {
    const res = await getInventoryWarningCountApi()
    menuBadges['/warehouse'] = Number(res.data || 0)
  } catch (e) {
    menuBadges['/warehouse'] = 0
  }

  // 转正到期：试用中且临近结束日期（含已逾期），天数与转正管理页共用同一常量
  try {
    const res = await getProbationPageApi({
      pageNum: 1, pageSize: 1, status: 1, expireDays: PROBATION_EXPIRE_DAYS
    })
    menuBadges['/recruit/probation'] = Number(res.data?.total || 0)
  } catch (e) {
    // 非管理员访问会被拒，静默不显示
    menuBadges['/recruit/probation'] = 0
  }
}

onMounted(() => {
  refreshMenuBadges(true)
  refreshMsgCount()
})

// Ctrl+K 全局搜索快捷键注册/注销
window.addEventListener('keydown', onGlobalKeydown)
onUnmounted(() => window.removeEventListener('keydown', onGlobalKeydown))

// 切换页面时刷新角标，避免数据变动后数字不更新
watch(() => route.path, () => {
  refreshMenuBadges()
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
  // 快速通道：审批/薪资类消息直达处理页面，其余进消息中心
  router.push(messageJumpTarget(m) || '/message')
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
  searchVisible.value = false
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
      router.push('/profile')
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
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
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
  color: var(--el-text-color-regular);
  transition: color 0.3s;

  &:hover {
    color: var(--el-color-primary);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-icon {
  cursor: pointer;
  color: var(--el-text-color-regular);
  transition: color 0.3s;

  &:hover {
    color: var(--el-color-primary);
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
    background-color: var(--el-fill-color-light);
  }
}

.user-avatar {
  background-color: var(--el-color-primary);
  color: #fff;
}

.user-name {
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.main-content {
  background-color: var(--el-bg-color-page);
  padding: 20px;
  min-height: 0;
  overflow-y: auto;
}

/* 侧边栏菜单角标 */
.menu-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-badge {
  flex: none;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  margin-left: 6px;
  border-radius: 9px;
  background: #F56C6C;
  color: #fff;
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
}

/* 侧边栏折叠时，数字浮在分组图标右上角 */
.badge-float {
  position: absolute;
  top: -7px;
  right: -10px;
  margin-left: 0;
  transform: scale(0.85);
  transform-origin: right top;
}

.group-icon {
  position: relative;
}

/* 顶部通知面板 */
.notice-panel { max-height: 420px; overflow-y: auto; }
.notice-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}
.notice-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover { background-color: var(--el-fill-color-light); }
}
.notice-title { font-size: 13px; color: var(--el-text-color-primary); font-weight: 500; }
.notice-sku { color: var(--el-text-color-secondary); font-weight: 400; margin-left: 6px; font-size: 12px; }
.notice-sub { font-size: 12px; color: var(--el-color-danger); margin-top: 2px; }
.notice-more { text-align: center; font-size: 12px; color: var(--el-color-primary); padding: 8px 0; cursor: pointer; }

/* 站内消息弹层（P2-1） */
.msg-unread { background-color: var(--el-color-primary-light-9); }
.msg-pop-title { font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.msg-pop-content {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-top: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.msg-pop-time { font-size: 12px; color: var(--el-text-color-placeholder); margin-top: 4px; }

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
  color: var(--el-text-color-primary);

  &:hover { background-color: var(--el-fill-color-light); color: var(--el-color-primary); }
}

/* 搜索分组（P7-1） */
.search-group-title {
  padding: 8px 12px 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 600;
}
.search-item-sub { font-size: 12px; color: var(--el-text-color-placeholder); }
.search-item-msg { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

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
