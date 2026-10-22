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
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        class="sidebar-menu"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
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
            <el-icon class="header-icon" :size="18"><Search /></el-icon>
          </el-tooltip>
          <el-tooltip content="通知" placement="bottom">
            <el-badge :value="5" :max="99" class="header-icon">
              <el-icon :size="18"><Bell /></el-icon>
            </el-badge>
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
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
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
  SwitchButton
} from '@element-plus/icons-vue'
import AiChat from '@/components/AiChat.vue'
import logoMark from '@/assets/logo-mark.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)

const menuItems = [
  { path: '/home', title: '首页', icon: 'HomeFilled' },
  { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
  { path: '/employee', title: '员工管理', icon: 'User' },
  { path: '/department', title: '部门管理', icon: 'OfficeBuilding' },
  { path: '/attendance', title: '考勤管理', icon: 'Calendar' },
  { path: '/salary', title: '薪资管理', icon: 'Money' },
  { path: '/performance', title: '绩效管理', icon: 'TrendCharts' },
  { path: '/analytics', title: '智能分析', icon: 'DataAnalysis' },
  { path: '/business', title: '营业分析', icon: 'Histogram' },
  { path: '/order', title: '销售订单', icon: 'Tickets' },
  { path: '/product', title: '产品管理', icon: 'Goods' },
  { path: '/warehouse', title: '仓库库存', icon: 'Box' },
  { path: '/customer', title: '境外客户', icon: 'UserFilled' },
  { path: '/supplier', title: '供应商', icon: 'Van' },
  { path: '/shipment', title: '物流跟踪', icon: 'Ship' },
  { path: '/payment', title: '收付款', icon: 'Wallet' }
]

const activeMenu = computed(() => route.path)
const currentPageTitle = computed(() => route.meta?.title || '')

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

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}

.logo {
  height: 60px;
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

.sidebar-menu {
  border-right: none;
  height: calc(100vh - 60px);
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
  overflow-y: auto;
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
