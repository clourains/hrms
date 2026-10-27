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

      <!-- Tab 切换：概览 / 详细数据（渐进披露，默认概览态保持首页干净） -->
      <el-tabs v-model="activeTab" class="home-tabs" @tab-change="onTabChange">
        <el-tab-pane label="概览" name="overview">
          <!-- 公告栏 -->
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

          <!-- 快捷入口 -->
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
        </el-tab-pane>

        <el-tab-pane label="详细数据" name="dashboard">
          <!-- P8-B 仪表盘统计卡片（详细数据态才展示，避免首页过杂） -->
          <div v-if="stats" class="stats-grid">
            <div class="stat-card">
              <div class="stat-icon stat-icon-blue"><el-icon :size="22"><User /></el-icon></div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.totalEmployees || 0 }}</div>
                <div class="stat-label">员工总数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon stat-icon-green"><el-icon :size="22"><UserFilled /></el-icon></div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.activeEmployees || 0 }}</div>
                <div class="stat-label">在职员工</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon stat-icon-purple"><el-icon :size="22"><TrendCharts /></el-icon></div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.newEmployees || 0 }}</div>
                <div class="stat-label">本月入职</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon stat-icon-orange"><el-icon :size="22"><ChatDotRound /></el-icon></div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.activeUsersToday || 0 }}</div>
                <div class="stat-label">今日活跃</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon stat-icon-red"><el-icon :size="22"><Notification /></el-icon></div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.noticeCount || 0 }}</div>
                <div class="stat-label">生效公告</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon stat-icon-cyan"><el-icon :size="22"><Calendar /></el-icon></div>
              <div class="stat-info">
                <div class="stat-value">{{ formatRate(stats.attendanceRate) }}</div>
                <div class="stat-label">出勤率</div>
              </div>
            </div>
          </div>

          <!-- P8-B 可视化图表 -->
          <div v-if="structure" class="charts-row">
            <div class="chart-card">
              <div class="chart-title">部门人数 Top 5</div>
              <div ref="deptChartRef" class="chart-canvas" />
            </div>
            <div class="chart-card">
              <div class="chart-title">性别分布</div>
              <div ref="genderChartRef" class="chart-canvas" />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
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
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getActiveNoticesApi, readNoticeApi } from '@/api/notice'
import { getDashboardStatsApi, getEmployeeStructureApi } from '@/api/analytics'
import * as echarts from 'echarts'
import FlowBackground from '@/components/FlowBackground.vue'
import logoMark from '@/assets/logo-mark.png'

const router = useRouter()
const userStore = useUserStore()

// 公告栏（P6-1）
const notices = ref([])
const noticeDialogVisible = ref(false)
const currentNotice = ref({})

// P8-B 仪表盘
const stats = ref(null)
const structure = ref(null)
const deptChartRef = ref(null)
const genderChartRef = ref(null)
let deptChart = null
let genderChart = null

// Tab：默认概览态（公告 + 快捷入口），详细数据态才出统计卡和图表
const activeTab = ref('overview')

const onTabChange = async (tab) => {
  if (tab === 'dashboard' && structure.value) {
    // 等 DOM 渲染出两个 chart-canvas ref 再 init
    await nextTick()
    renderCharts()
  }
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')
const formatRate = (v) => v != null ? `${Number(v).toFixed(1)}%` : '-'

const loadNotices = async () => {
  try {
    const res = await getActiveNoticesApi()
    if (res.code === 200) {
      notices.value = res.data || []
    }
  } catch { /* 公告加载失败不影响首页 */ }
}

const loadDashboard = async () => {
  try {
    const [sRes, pRes] = await Promise.all([
      getDashboardStatsApi().catch(() => ({ code: 500, data: null })),
      getEmployeeStructureApi().catch(() => ({ code: 500, data: null }))
    ])
    if (sRes.code === 200) stats.value = sRes.data
    if (pRes.code === 200) structure.value = pRes.data
    await nextTick()
    if (structure.value) renderCharts()
  } catch { /* 仪表盘加载失败静默 */ }
}

// 部门柱状图
const renderDeptChart = () => {
  if (!deptChartRef.value) return
  const data = (structure.value.departmentDistribution || []).slice(0, 5)
  const labels = data.map(d => d.name || d.deptName || d.label || Object.values(d)[0])
  const values = data.map(d => Number(d.count ?? d.value ?? d.num ?? 0))
  deptChart = echarts.init(deptChartRef.value)
  deptChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(20,20,20,0.9)', borderColor: '#444', textStyle: { color: '#fff' } },
    grid: { left: 10, right: 10, top: 16, bottom: 10, containLabel: true },
    xAxis: { type: 'category', data: labels, axisLabel: { color: 'rgba(255,255,255,0.7)', fontSize: 11 }, axisLine: { lineStyle: { color: 'rgba(255,255,255,0.15)' } } },
    yAxis: { type: 'value', axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 11 }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } } },
    series: [{
      type: 'bar',
      data: values,
      barWidth: 22,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#3B82F6' },
          { offset: 1, color: '#1D4ED8' }
        ])
      }
    }]
  })
}

// 性别环形图
const renderGenderChart = () => {
  if (!genderChartRef.value) return
  const data = (structure.value.genderDistribution || []).map(d => ({
    name: d.name || (d.gender === 1 ? '男' : d.gender === 0 ? '女' : '未知'),
    value: Number(d.count ?? d.value ?? d.num ?? 0)
  }))
  genderChart = echarts.init(genderChartRef.value)
  genderChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: 'rgba(20,20,20,0.9)', borderColor: '#444', textStyle: { color: '#fff' } },
    legend: { bottom: 2, textStyle: { color: 'rgba(255,255,255,0.7)', fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: 'rgba(20,20,20,0.6)', borderWidth: 2 },
      label: { color: 'rgba(255,255,255,0.7)', fontSize: 12, formatter: '{d}%' },
      data: data.length > 0 ? data : [
        { name: '男', value: 0, itemStyle: { color: '#60A5FA' } },
        { name: '女', value: 0, itemStyle: { color: '#F472B6' } }
      ],
      color: ['#60A5FA', '#F472B6', '#A78BFA']
    }]
  })
}

const renderCharts = () => {
  renderDeptChart()
  renderGenderChart()
}

const resizeCharts = () => {
  deptChart?.resize()
  genderChart?.resize()
}

const openNotice = async (n) => {
  currentNotice.value = n
  noticeDialogVisible.value = true
  if (n.isRead === 0) {
    n.isRead = 1
    readNoticeApi(n.id).catch(() => {})
  }
}

onMounted(async () => {
  await Promise.all([loadNotices(), loadDashboard()])
  // 默认概览态，图表不渲染；切到详细数据时 onTabChange 触发
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  deptChart?.dispose()
  genderChart?.dispose()
})

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

/* 首页 Tab 条：深色主题下轻量不抢眼 */
.home-tabs {
  margin-top: 30px;
  max-width: 1120px;

  :deep(.el-tabs__nav-wrap::after) {
    background-color: rgba(255, 255, 255, 0.1);
  }
  :deep(.el-tabs__item) {
    color: rgba(255, 255, 255, 0.55);
    font-weight: 500;
    height: 36px;
    line-height: 36px;
    font-size: 14px;

    &.is-active {
      color: #e0bc6a;
    }
    &:hover {
      color: rgba(255, 255, 255, 0.9);
    }
  }
  :deep(.el-tabs__active-bar) {
    background-color: #e0bc6a;
    height: 2px;
  }
}

/* P8-B 仪表盘统计卡片 */
.stats-grid {
  margin-top: 28px;
  max-width: 1120px;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
}

.stat-card {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  transition: background 0.2s, transform 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    transform: translateY(-1px);
  }
}

.stat-icon {
  flex: none;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-icon-blue   { background: rgba(59, 130, 246, 0.25); color: #60A5FA; }
.stat-icon-green  { background: rgba(34, 197, 94, 0.25); color: #4ADE80; }
.stat-icon-purple { background: rgba(139, 92, 246, 0.25); color: #A78BFA; }
.stat-icon-orange { background: rgba(245, 158, 11, 0.25); color: #FBBF24; }
.stat-icon-red    { background: rgba(239, 68, 68, 0.25);  color: #F87171; }
.stat-icon-cyan   { background: rgba(6, 182, 212, 0.25);  color: #22D3EE; }

.stat-info { min-width: 0; }

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
}

.stat-label {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

/* P8-B 图表行 */
.charts-row {
  margin-top: 28px;
  max-width: 1120px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card {
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.12);
  padding: 14px 16px;
}

.chart-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 8px;
}

.chart-canvas {
  width: 100%;
  height: 220px;
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
