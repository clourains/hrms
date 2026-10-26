<template>
  <div class="home">
    <FlowBackground />

    <div class="home-inner">
      <div class="home-brand">
        <div class="brand-chip">
          <img :src="logoMark" alt="星跃出海" />
        </div>
        <div class="brand-text">
          <h1>星跃出海国际贸易有限公司</h1>
          <p>人力资源管理系统 · 业务系统</p>
        </div>
      </div>

      <div class="welcome">
        <h2>{{ greeting }}，{{ displayName }}</h2>
        <p>欢迎使用星跃出海国际贸易有限公司人力资源与业务一体化管理平台</p>
      </div>

      <!-- 公告栏（P6-1）：有生效公告时展示，重要公告优先置顶 -->
      <div v-if="notices.length > 0" class="notice-board">
        <div
          v-for="n in notices"
          :key="n.id"
          class="notice-card"
          :class="{ 'notice-important': n.level === 'important', 'notice-read': n.isRead === 1 }"
          @click="openNotice(n)"
        >
          <div class="notice-icon">
            <el-icon :size="18"><component :is="n.level === 'important' ? 'BellFilled' : 'Notification'" /></el-icon>
          </div>
          <div class="notice-body">
            <div class="notice-head">
              <el-tag v-if="n.level === 'important'" type="danger" size="small" effect="dark">重要</el-tag>
              <el-tag v-if="n.isRead === 0" type="warning" size="small" effect="plain">未读</el-tag>
              <span class="notice-title">{{ n.title }}</span>
            </div>
            <div class="notice-preview">{{ n.content }}</div>
            <div class="notice-meta">{{ n.publisher || '系统' }} · {{ formatTime(n.createTime) }}</div>
          </div>
        </div>
      </div>

      <div class="quick-grid">
        <div
          v-for="item in quickEntries"
          :key="item.path"
          class="quick-card"
          @click="go(item.path)"
        >
          <div class="quick-icon">
            <el-icon :size="22"><component :is="item.icon" /></el-icon>
          </div>
          <div class="quick-text">
            <span class="quick-title">{{ item.title }}</span>
            <small class="quick-desc">{{ item.desc }}</small>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 公告详情弹窗（打开即标记已读） -->
  <el-dialog v-model="noticeDialogVisible" :title="currentNotice.title" width="520px">
    <div class="notice-detail-meta">{{ currentNotice.publisher || '系统' }} · {{ formatTime(currentNotice.createTime) }}</div>
    <div class="notice-detail-content">{{ currentNotice.content }}</div>
    <template #footer>
      <el-button type="primary" @click="noticeDialogVisible = false">我知道了</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getActiveNoticesApi, readNoticeApi } from '@/api/notice'
import FlowBackground from '@/components/FlowBackground.vue'
import logoMark from '@/assets/logo-mark.png'

const router = useRouter()
const userStore = useUserStore()

// 公告栏（P6-1）
const notices = ref([])
const noticeDialogVisible = ref(false)
const currentNotice = ref({})

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

const loadNotices = async () => {
  try {
    const res = await getActiveNoticesApi()
    if (res.code === 200) {
      notices.value = res.data || []
    }
  } catch {
    // 公告加载失败不影响首页
  }
}

const openNotice = async (n) => {
  currentNotice.value = n
  noticeDialogVisible.value = true
  if (n.isRead === 0) {
    n.isRead = 1
    readNoticeApi(n.id).catch(() => {})
  }
}

onMounted(loadNotices)

const displayName = computed(() => userStore.userInfo?.realName || userStore.username || '管理员')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const quickEntries = [
  { path: '/employee', title: '员工管理', desc: '在职人员与档案', icon: 'User' },
  { path: '/attendance', title: '考勤管理', desc: '打卡与请假记录', icon: 'Calendar' },
  { path: '/salary', title: '薪资管理', desc: '工资核算与发放', icon: 'Money' },
  { path: '/performance', title: '绩效管理', desc: '考核评分与结果', icon: 'TrendCharts' },
  { path: '/order', title: '销售订单', desc: '出口订单全流程', icon: 'Tickets' },
  { path: '/warehouse', title: '仓库库存', desc: '国内仓与海外仓', icon: 'Box' },
  { path: '/customer', title: '境外客户', desc: '美国客户档案', icon: 'UserFilled' },
  { path: '/business', title: '营业分析', desc: '营收与利润看板', icon: 'Histogram' }
]

const go = (path) => {
  router.push(path)
}
</script>

<style scoped lang="scss">
/* 冲掉 el-main 的 20px 内边距，让背景铺满头部以下的整个内容区 */
.home {
  position: relative;
  margin: -20px;
  height: calc(100vh - 60px);
  overflow: hidden;
  background: #050b1c;
}

.home-inner {
  position: relative;
  z-index: 1;
  height: 100%;
  overflow-y: auto;
  padding: 36px 48px 44px;
}

.home-brand {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-chip {
  width: 64px;
  height: 64px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.35);

  img {
    width: 48px;
    height: 48px;
    object-fit: contain;
  }
}

.brand-text {
  h1 {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    letter-spacing: 2px;
    color: #fff;
  }

  p {
    margin: 6px 0 0;
    font-size: 13px;
    letter-spacing: 3px;
    color: #e0bc6a;
  }
}

.welcome {
  margin-top: 40px;

  h2 {
    margin: 0;
    font-size: 30px;
    font-weight: 600;
    letter-spacing: 1px;
    color: #fff;
  }

  p {
    margin: 12px 0 0;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.6);
  }
}

.quick-grid {
  margin-top: 34px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
  max-width: 1120px;
}

/* 公告栏（P6-1）：深色主题下的毛玻璃卡片 */
.notice-board {
  margin-top: 24px;
  max-width: 1120px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-card {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    transform: translateY(-1px);
  }

  &.notice-important {
    border-color: rgba(245, 108, 108, 0.5);
    background: rgba(245, 108, 108, 0.08);
  }

  &.notice-read {
    opacity: 0.65;
  }
}

.notice-icon {
  flex: none;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(224, 188, 106, 0.15);
  color: #e0bc6a;
}

.notice-body {
  flex: 1;
  min-width: 0;
}

.notice-head {
  display: flex;
  align-items: center;
  gap: 8px;

  .notice-title {
    font-size: 14px;
    font-weight: 600;
    color: #fff;
  }
}

.notice-preview {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-meta {
  margin-top: 6px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

.notice-detail-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}

.notice-detail-content {
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  cursor: pointer;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(6px);
  transition: transform 0.25s, background 0.25s, border-color 0.25s, box-shadow 0.25s;

  &:hover {
    transform: translateY(-3px);
    background: rgba(255, 255, 255, 0.11);
    border-color: rgba(224, 188, 106, 0.55);
    box-shadow: 0 14px 30px rgba(0, 0, 0, 0.35);
  }
}

.quick-icon {
  width: 44px;
  height: 44px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  color: #e0bc6a;
  background: rgba(224, 188, 106, 0.12);
}

.quick-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.quick-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.quick-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}
</style>
