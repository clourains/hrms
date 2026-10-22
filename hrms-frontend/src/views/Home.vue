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
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import FlowBackground from '@/components/FlowBackground.vue'
import logoMark from '@/assets/logo-mark.png'

const router = useRouter()
const userStore = useUserStore()

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
