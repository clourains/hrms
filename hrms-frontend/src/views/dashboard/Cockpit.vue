<template>
  <div class="cockpit">
    <!-- 背景网格 + 扫描光效 -->
    <div class="bg-grid"></div>
    <div class="bg-glow"></div>
    <div class="scan-line"></div>

    <!-- 顶栏 -->
    <header class="cp-header">
      <div class="cp-brand">
        <div class="cp-logo">
          <el-icon :size="22"><DataBoard /></el-icon>
        </div>
        <div class="cp-brand-text">
          <div class="cp-title">深度数据驾驶舱</div>
          <div class="cp-sub">星跃出海国际贸易有限公司 · 运营实时监控</div>
        </div>
      </div>

      <!-- 视图切换：人力 / 业务 -->
      <div class="cp-switch">
        <button
          type="button"
          :class="{ active: viewMode === 'hr' }"
          @click="switchView('hr')"
        >人力驾驶舱</button>
        <button
          type="button"
          :class="{ active: viewMode === 'business' }"
          @click="switchView('business')"
        >业务驾驶舱</button>
      </div>

      <div class="cp-header-right">
        <div class="cp-live">
          <span class="live-dot"></span>
          数据实时同步
        </div>
        <div class="cp-clock">{{ clockText }}</div>
        <el-button link class="cp-btn" :icon="Refresh" :loading="loading" @click="refreshAll">刷新</el-button>
        <el-button link class="cp-btn" :icon="FullScreen" @click="toggleFullscreen">全屏</el-button>
        <el-button class="cp-exit" :icon="Back" @click="goBack">返回仪表盘</el-button>
      </div>
    </header>

    <!-- 核心指标带 -->
    <section class="cp-kpi-row" :class="{ pulsing: pulse }">
      <div v-for="k in currentKpis" :key="k.key" class="cp-kpi" :style="{ '--kpi-color': k.color }">
        <div class="kpi-head">
          <el-icon :size="15"><component :is="k.icon" /></el-icon>
          <span>{{ k.label }}</span>
        </div>
        <div class="kpi-value">
          <span class="kpi-num">{{ k.display }}</span>
          <span v-if="k.unit" class="kpi-unit">{{ k.unit }}</span>
        </div>
        <div class="kpi-foot">{{ k.foot }}</div>
        <div class="kpi-bar"><i :style="{ width: k.ratio + '%' }"></i></div>
      </div>
    </section>

    <!-- 主体三列（人力视图） -->
    <section v-if="viewMode === 'hr'" class="cp-main">
      <!-- 左列 -->
      <div class="cp-col cp-col-left">
        <div class="cp-panel">
          <div class="panel-head">
            <span class="panel-name">部门人员分布</span>
            <span class="panel-tag">{{ deptRows.length }} 个部门</span>
          </div>
          <div ref="deptChartRef" class="panel-chart"></div>
        </div>
        <div class="cp-panel">
          <div class="panel-head">
            <span class="panel-name">薪资区间分布</span>
            <span class="panel-tag">按月工资单</span>
          </div>
          <div ref="salaryChartRef" class="panel-chart"></div>
        </div>
      </div>

      <!-- 中列 -->
      <div class="cp-col cp-col-center">
        <div class="cp-panel panel-core">
          <div class="core-ring">
            <div ref="gaugeChartRef" class="core-chart"></div>
            <div class="core-center">
              <div class="core-num">{{ animateValue.active }}</div>
              <div class="core-label">在职员工</div>
              <div class="core-sub">总人数 {{ animateValue.total }} 人</div>
            </div>
          </div>
          <div class="core-side">
            <div class="core-side-item">
              <span class="csi-label">出勤率</span>
              <span class="csi-value">{{ stats.attendanceRate ?? '--' }}%</span>
            </div>
            <div class="core-side-item">
              <span class="csi-label">平均绩效</span>
              <span class="csi-value">{{ stats.avgPerformance ?? '--' }}</span>
            </div>
            <div class="core-side-item">
              <span class="csi-label">流失风险</span>
              <span class="csi-value warn">{{ prediction.predictedAttritionRate ?? '--' }}%</span>
            </div>
            <div class="core-side-item">
              <span class="csi-label">部门 / 岗位</span>
              <span class="csi-value">{{ stats.deptCount ?? '--' }} / {{ stats.positionCount ?? '--' }}</span>
            </div>
          </div>
        </div>

        <div class="cp-panel">
          <div class="panel-head">
            <span class="panel-name">人员入离职趋势</span>
            <span class="panel-tag">近 12 个月</span>
          </div>
          <div ref="trendChartRef" class="panel-chart"></div>
        </div>
      </div>

      <!-- 右列 -->
      <div class="cp-col cp-col-right">
        <div class="cp-panel">
          <div class="panel-head">
            <span class="panel-name">绩效等级分布</span>
            <span class="panel-tag">均分 {{ perfData.avgTotalScore ?? '--' }}</span>
          </div>
          <div ref="perfChartRef" class="panel-chart"></div>
        </div>
        <div class="cp-panel">
          <div class="panel-head">
            <span class="panel-name">考勤趋势</span>
            <span class="panel-tag">近 14 天</span>
          </div>
          <div ref="attendanceChartRef" class="panel-chart"></div>
        </div>
      </div>
    </section>

    <!-- 主体三列（业务视图） -->
    <section v-else class="cp-main">
      <div class="cp-col cp-col-left">
        <div class="cp-panel">
          <div class="panel-head">
            <span class="panel-name">近 12 个月销售趋势</span>
            <span class="panel-tag">USD · 订单量</span>
          </div>
          <div ref="bizTrendChartRef" class="panel-chart"></div>
        </div>
        <div class="cp-panel">
          <div class="panel-head">
            <span class="panel-name">销售国家分布</span>
            <span class="panel-tag">按销售额</span>
          </div>
          <div ref="bizCountryChartRef" class="panel-chart"></div>
        </div>
      </div>

      <div class="cp-col cp-col-center">
        <div class="cp-panel">
          <div class="panel-head">
            <span class="panel-name">客户销售额 TOP10</span>
            <span class="panel-tag">USD</span>
          </div>
          <div ref="bizCustomerChartRef" class="panel-chart"></div>
        </div>
        <div class="cp-panel">
          <div class="panel-head">
            <span class="panel-name">产品销售额 TOP10</span>
            <span class="panel-tag">USD</span>
          </div>
          <div ref="bizProductChartRef" class="panel-chart"></div>
        </div>
      </div>

      <div class="cp-col cp-col-right">
        <div class="cp-panel">
          <div class="panel-head">
            <span class="panel-name">各仓库库存价值</span>
            <span class="panel-tag">CNY</span>
          </div>
          <div ref="bizInventoryChartRef" class="panel-chart"></div>
        </div>
        <div class="cp-panel">
          <div class="panel-head">
            <span class="panel-name">产品利润排行 TOP10</span>
            <span class="panel-tag">USD</span>
          </div>
          <div ref="bizProfitChartRef" class="panel-chart"></div>
        </div>
      </div>
    </section>

    <!-- 业务：全球物流实时航线 -->
    <section v-if="viewMode === 'business'" class="cp-logistics">
      <div class="cp-panel">
        <div class="panel-head">
          <span class="panel-name">全球物流实时航线</span>
          <div class="live-query">
            <el-input
              v-model="liveQueryNo"
              size="small"
              class="live-input"
              placeholder="输入真实快递单号，如 1Z999AA10123456784"
              clearable
              @keyup.enter="handleLiveQuery"
            />
            <el-button size="small" type="primary" :loading="liveQueryLoading" @click="handleLiveQuery">查实时轨迹</el-button>
            <el-button v-if="liveTrailNo" size="small" link class="live-clear" @click="clearLiveTrail">清除</el-button>
          </div>
          <span class="panel-tag">
            {{ liveTrailNo ? `17TRACK · ${liveTrailNo} · ${liveNodeCount} 个节点` : '在途动态追踪 · 每 60 秒刷新' }}
          </span>
        </div>
        <div ref="bizShipmentMapRef" class="panel-chart"></div>
      </div>
    </section>

    <!-- 底部：实时数据流 -->
    <section class="cp-stream">
      <div class="stream-head">
        <span class="stream-title">
          <span class="live-dot small"></span>
          实时数据流
        </span>
        <span class="stream-hint">自动轮播 · 每 4 秒刷新</span>
      </div>

      <div class="stream-body">
        <div class="stream-list">
          <TransitionGroup name="flow">
            <div v-for="item in visibleActivities" :key="item.uid" class="stream-item" :class="'type-' + item.type">
              <span class="si-type">{{ activityTypeText(item.type) }}</span>
              <span class="si-content">{{ item.content }}</span>
              <span class="si-time">{{ item.at || item.time }}</span>
            </div>
          </TransitionGroup>
          <div v-if="!visibleActivities.length" class="stream-empty">暂无实时动态</div>
        </div>

        <div class="stream-metrics" v-if="viewMode === 'hr'">
          <div class="sm-item">
            <div class="sm-label">累计销售额</div>
            <div class="sm-value">${{ fmtCompact(business.totalSalesUsd) }}</div>
          </div>
          <div class="sm-item">
            <div class="sm-label">毛利率</div>
            <div class="sm-value">{{ business.grossMarginRate ?? '--' }}%</div>
          </div>
          <div class="sm-item">
            <div class="sm-label">订单总量</div>
            <div class="sm-value">{{ business.totalOrders ?? '--' }}</div>
          </div>
          <div class="sm-item">
            <div class="sm-label">库存价值</div>
            <div class="sm-value">¥{{ fmtCompact(business.inventoryValueCny) }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- AI 洞察字幕 -->
    <div class="cp-insight" v-if="insightText">
      <span class="insight-label">AI 洞察</span>
      <span class="insight-text">{{ insightText }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  DataBoard, Refresh, FullScreen, Back, User, UserFilled, Clock,
  TrendCharts, Money, Warning, Tickets, Box, ShoppingCart
} from '@element-plus/icons-vue'
import {
  getDashboardStatsApi, getEmployeeStructureApi, getAttendanceAnalysisApi,
  getSalaryAnalysisApi, getPerformanceAnalysisApi, getTalentPredictionApi,
  getTurnoverTrendApi, getRecentActivitiesApi, getAiInsightsApi
} from '@/api/analytics'
import {
  getBusinessOverviewApi, getMonthlySalesApi, getTopCustomersApi,
  getTopProductsApi, getCountryDistributionApi, getInventoryValueApi,
  getProfitRankingApi, getInventoryWarningCountApi, getShipmentPageApi,
  queryLiveTrackByNoApi
} from '@/api/trade'
import worldGeo from '@/assets/geo/world.json'
import { getCoord, getCoordSmart } from '@/utils/geoCoord'

const router = useRouter()
const loading = ref(false)
const pulse = ref(false)
const clockText = ref('')

const stats = ref({})
const structure = ref({})
const attendance = ref({})
const salary = ref({})
const perfData = ref({})
const prediction = ref({})
const business = ref({})
const trend = ref([])
const activities = ref([])
const insightText = ref('')

// 视图模式：人力 / 业务
const viewMode = ref('hr')

// 业务数据
const bizTrend = ref({ months: [], sales: [], orderCounts: [] })
const bizCustomers = ref([])
const bizProducts = ref([])
const bizCountries = ref([])
const bizInventory = ref([])
const bizProfit = ref({ byCustomer: [], byProduct: [] })
const warningCount = ref(0)

// 数字滚动动画：驾驶舱的数字必须"动起来"才有数据流的感觉
const animateValue = ref({ total: 0, active: 0 })
const rafIds = {}

const tweenTo = (key, target, duration = 900) => {
  const start = Number(animateValue.value[key]) || 0
  const end = Number(target) || 0
  if (start === end) return
  if (rafIds[key]) cancelAnimationFrame(rafIds[key])
  const t0 = performance.now()
  const step = (now) => {
    const p = Math.min(1, (now - t0) / duration)
    const eased = 1 - Math.pow(1 - p, 3)
    animateValue.value[key] = Math.round(start + (end - start) * eased)
    if (p < 1) rafIds[key] = requestAnimationFrame(step)
  }
  rafIds[key] = requestAnimationFrame(step)
}

const fmtCompact = (n) => {
  const v = Number(n) || 0
  if (v >= 10000000) return (v / 10000000).toFixed(2) + ' 千万'
  if (v >= 10000) return (v / 10000).toFixed(1) + ' 万'
  return v.toLocaleString()
}

const kpis = computed(() => {
  const s = stats.value
  const total = Number(s.totalEmployees || 0)
  const active = Number(s.activeEmployees || 0)
  return [
    {
      key: 'total', label: '员工总数', icon: User, color: '#38bdf8',
      display: animateValue.value.total, unit: '人',
      foot: `本月入职 ${s.newEmployees || 0} 人`,
      ratio: 100
    },
    {
      key: 'active', label: '在职员工', icon: UserFilled, color: '#22d3ee',
      display: animateValue.value.active, unit: '人',
      foot: `本月离职 ${s.resignedEmployees || 0} 人`,
      ratio: total ? Math.round(active / total * 100) : 0
    },
    {
      key: 'attend', label: '出勤率', icon: Clock, color: '#34d399',
      display: s.attendanceRate ?? '--', unit: '%',
      foot: `平均工时 ${attendance.value.avgWorkHours ?? '--'} h`,
      ratio: Math.min(100, Number(s.attendanceRate || 0))
    },
    {
      key: 'perf', label: '平均绩效', icon: TrendCharts, color: '#a78bfa',
      display: s.avgPerformance ?? '--', unit: '分',
      foot: `S 级 ${perfGradeMap.value.S || 0} 人`,
      ratio: Math.min(100, Number(s.avgPerformance || 0))
    },
    {
      key: 'salary', label: '人均薪资', icon: Money, color: '#fbbf24',
      display: s.avgSalary ? Math.round(Number(s.avgSalary)).toLocaleString() : '--', unit: '元',
      foot: `薪资总额 ${fmtCompact(s.totalSalary)}`,
      ratio: 100
    },
    {
      key: 'risk', label: '流失风险', icon: Warning, color: '#f87171',
      display: prediction.value.predictedAttritionRate ?? '--', unit: '%',
      foot: '近 6 个月离职 ÷ 在职',
      ratio: Math.min(100, Number(prediction.value.predictedAttritionRate || 0) * 10)
    }
  ]
})

// 业务视图 KPI
const bizKpis = computed(() => {
  const b = business.value
  const lastSales = (bizTrend.value.sales || []).slice(-1)[0] || 0
  const orders = Number(b.totalOrders || 0)
  const sales = Number(b.totalSalesUsd || 0)
  const avgTicket = orders ? Math.round(sales / orders) : 0
  const topCustomer = (bizCustomers.value[0] || {}).totalSales
  return [
    {
      key: 'bsales', label: '累计销售额', icon: Money, color: '#fbbf24',
      display: '$' + fmtCompact(sales), unit: '',
      foot: `最近月 $${fmtCompact(lastSales)}`, ratio: 100
    },
    {
      key: 'bmargin', label: '毛利率', icon: TrendCharts, color: '#34d399',
      display: b.grossMarginRate ?? '--', unit: '%',
      foot: '销售额 - 产品成本', ratio: Math.min(100, Number(b.grossMarginRate || 0))
    },
    {
      key: 'borders', label: '订单总量', icon: Tickets, color: '#38bdf8',
      display: orders, unit: '单',
      foot: `平均客单 $${fmtCompact(avgTicket)}`, ratio: 100
    },
    {
      key: 'bcustomer', label: '客户总数', icon: ShoppingCart, color: '#a78bfa',
      display: b.totalCustomers ?? '--', unit: '家',
      foot: `TOP1 客户 $${fmtCompact(topCustomer)}`, ratio: 100
    },
    {
      key: 'binventory', label: '库存价值', icon: Box, color: '#22d3ee',
      display: '¥' + fmtCompact(b.inventoryValueCny), unit: '',
      foot: `${bizInventory.value.length} 个仓库`, ratio: 100
    },
    {
      key: 'bwarn', label: '库存预警', icon: Warning, color: '#f87171',
      display: warningCount.value, unit: '项',
      foot: '低于安全库存', ratio: Math.min(100, warningCount.value * 6)
    }
  ]
})

const currentKpis = computed(() => (viewMode.value === 'hr' ? kpis.value : bizKpis.value))

const switchView = (mode) => {
  if (viewMode.value !== mode) viewMode.value = mode
}

const perfGradeMap = computed(() => {
  const map = {}
  ;(perfData.value.gradeDistribution || []).forEach(d => { map[d.name] = d.value })
  return map
})

const deptRows = computed(() => {
  const list = [...(structure.value.departmentDistribution || [])]
  return list.sort((a, b) => b.value - a.value)
})

// 实时数据流：每次只展示 4 条，定时向后滚动，形成"流水"观感
const streamOffset = ref(0)
const visibleActivities = computed(() => {
  const list = activities.value
  if (!list.length) return []
  const size = Math.min(4, list.length)
  const out = []
  for (let i = 0; i < size; i++) {
    out.push(list[(streamOffset.value + i) % list.length])
  }
  return out
})

const activityTypeText = (type) => {
  const map = { approved: '通过', pending: '待批', rejected: '驳回', leave: '离职', entry: '入职' }
  return map[type] || '动态'
}

// ===== 图表 =====
const deptChartRef = ref(null)
const salaryChartRef = ref(null)
const gaugeChartRef = ref(null)
const trendChartRef = ref(null)
const perfChartRef = ref(null)
const attendanceChartRef = ref(null)

// 业务视图图表容器
const bizTrendChartRef = ref(null)
const bizCountryChartRef = ref(null)
const bizCustomerChartRef = ref(null)
const bizProductChartRef = ref(null)
const bizInventoryChartRef = ref(null)
const bizProfitChartRef = ref(null)
const bizShipmentMapRef = ref(null)

let deptChart = null
let salaryChart = null
let gaugeChart = null
let trendChart = null
let perfChart = null
let attendanceChart = null

// 业务视图图表实例
let bizTrendChart = null
let bizCountryChart = null
let bizCustomerChart = null
let bizProductChart = null
let bizInventoryChart = null
let bizProfitChart = null
let bizShipmentMap = null

// 业务物流航线数据
const bizShipments = ref([])
let shipmentRoutes = []
let bizShipmentTimer = null
let worldRegistered = false
let lastShipKey = ''

// 实时查单（17TRACK）：手动输入真实单号，按节点地点绘制真实轨迹
const liveQueryNo = ref('')
const liveQueryLoading = ref(false)
const liveTrailNo = ref('')
const liveNodeCount = ref(0)
let liveTrail = null

const SHIP_STATUS_TEXT = { 1: '待发货', 2: '运输中', 3: '已到达', 4: '已签收' }
const SHIP_BASE_PROGRESS = { 1: 0.03, 2: 0.18, 3: 0.95, 4: 1 }
const shipStatusColor = (s) => ({ 1: '#64748b', 2: '#22d3ee', 3: '#3b82f6', 4: '#22c55e' }[s] || '#94a3b8')

const AXIS = { line: 'rgba(56,189,248,0.18)', label: '#7b8ca6' }
const PALETTE = ['#22d3ee', '#38bdf8', '#a78bfa', '#34d399', '#fbbf24', '#f87171', '#818cf8', '#2dd4bf']

const baseOption = {
  backgroundColor: 'transparent',
  textStyle: { color: '#cbd5e1' },
  tooltip: {
    backgroundColor: 'rgba(8, 18, 34, 0.94)',
    borderColor: 'rgba(56, 189, 248, 0.35)',
    textStyle: { color: '#e2e8f0', fontSize: 12 }
  }
}

const initDeptChart = () => {
  if (!deptChartRef.value) return
  deptChart?.dispose()
  deptChart = echarts.init(deptChartRef.value)
  const rows = deptRows.value.slice(0, 9).reverse()
  deptChart.setOption({
    ...baseOption,
    grid: { left: 8, right: 34, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', axisLine: { show: false }, axisLabel: { color: AXIS.label, fontSize: 10 }, splitLine: { lineStyle: { color: AXIS.line } } },
    yAxis: { type: 'category', data: rows.map(d => d.name), axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#9fb0c7', fontSize: 11 } },
    series: [{
      type: 'bar',
      barWidth: 10,
      data: rows.map((d, i) => ({
        value: d.value,
        itemStyle: {
          borderRadius: [0, 5, 5, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: 'rgba(34, 211, 238, 0.15)' },
            { offset: 1, color: PALETTE[i % PALETTE.length] }
          ])
        }
      })),
      label: { show: true, position: 'right', color: '#22d3ee', fontSize: 11, formatter: '{c}人' }
    }]
  })
}

const initSalaryChart = () => {
  if (!salaryChartRef.value) return
  salaryChart?.dispose()
  salaryChart = echarts.init(salaryChartRef.value)
  const rows = salary.value.salaryDistribution || []
  salaryChart.setOption({
    ...baseOption,
    grid: { left: 8, right: 8, top: 16, bottom: 4, containLabel: true },
    xAxis: { type: 'category', data: rows.map(d => d.name), axisLine: { lineStyle: { color: AXIS.line } }, axisTick: { show: false }, axisLabel: { color: AXIS.label, fontSize: 10 } },
    yAxis: { type: 'value', axisLabel: { color: AXIS.label, fontSize: 10 }, splitLine: { lineStyle: { color: AXIS.line } } },
    series: [{
      type: 'bar',
      barWidth: '46%',
      data: rows.map((d, i) => ({ value: d.value, itemStyle: { borderRadius: [4, 4, 0, 0], color: PALETTE[i % PALETTE.length] } })),
      label: { show: true, position: 'top', color: '#cbd5e1', fontSize: 10, formatter: '{c}' }
    }]
  })
}

const initGaugeChart = () => {
  if (!gaugeChartRef.value) return
  gaugeChart?.dispose()
  gaugeChart = echarts.init(gaugeChartRef.value)
  const total = Number(stats.value.totalEmployees || 0)
  const active = Number(stats.value.activeEmployees || 0)
  const rate = total ? Math.round(active / total * 100) : 0
  gaugeChart.setOption({
    backgroundColor: 'transparent',
    series: [{
      type: 'gauge',
      startAngle: 90,
      endAngle: -270,
      radius: '88%',
      pointer: { show: false },
      progress: {
        show: true,
        overlap: false,
        roundCap: true,
        clip: false,
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
          { offset: 0, color: '#22d3ee' },
          { offset: 1, color: '#38bdf8' }
        ]) }
      },
      axisLine: { lineStyle: { width: 10, color: [[1, 'rgba(56, 189, 248, 0.12)']] } },
      splitLine: { show: false },
      axisTick: { show: false },
      axisLabel: { show: false },
      data: [{ value: rate }],
      detail: { show: false },
      animationDuration: 1200
    }]
  })
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart?.dispose()
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    ...baseOption,
    grid: { left: 8, right: 12, top: 26, bottom: 4, containLabel: true },
    legend: { data: ['入职', '离职'], top: 0, right: 0, itemWidth: 9, itemHeight: 9, textStyle: { color: '#7b8ca6', fontSize: 10 } },
    xAxis: {
      type: 'category', boundaryGap: false, data: trend.value.map(d => d.month),
      axisLine: { lineStyle: { color: AXIS.line } }, axisTick: { show: false },
      axisLabel: { color: AXIS.label, fontSize: 10, hideOverlap: true }
    },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { color: AXIS.label, fontSize: 10 }, splitLine: { lineStyle: { color: AXIS.line } } },
    series: [
      {
        name: '入职', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
        data: trend.value.map(d => d.hireCount),
        itemStyle: { color: '#34d399' }, lineStyle: { color: '#34d399', width: 2 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(52, 211, 153, 0.30)' },
          { offset: 1, color: 'rgba(52, 211, 153, 0.02)' }
        ]) }
      },
      {
        name: '离职', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
        data: trend.value.map(d => d.leaveCount),
        itemStyle: { color: '#f87171' }, lineStyle: { color: '#f87171', width: 2 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(248, 113, 113, 0.28)' },
          { offset: 1, color: 'rgba(248, 113, 113, 0.02)' }
        ]) }
      }
    ]
  })
}

const initPerfChart = () => {
  if (!perfChartRef.value) return
  perfChart?.dispose()
  perfChart = echarts.init(perfChartRef.value)
  const rows = perfData.value.gradeDistribution || []
  perfChart.setOption({
    ...baseOption,
    tooltip: { ...baseOption.tooltip, trigger: 'item', formatter: '{b} 级: {c} 人 ({d}%)' },
    legend: { bottom: 0, left: 'center', itemWidth: 9, itemHeight: 9, textStyle: { color: '#7b8ca6', fontSize: 10 } },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      roseType: 'radius',
      itemStyle: { borderRadius: 6, borderColor: 'rgba(5, 11, 24, 0.9)', borderWidth: 2 },
      label: { color: '#cbd5e1', fontSize: 10, formatter: '{b}\n{c}' },
      data: rows.map(d => ({
        value: d.value,
        name: d.name,
        itemStyle: { color: { S: '#34d399', A: '#22d3ee', B: '#38bdf8', C: '#fbbf24', D: '#f87171' }[d.name] || '#818cf8' }
      }))
    }]
  })
}

const initAttendanceChart = () => {
  if (!attendanceChartRef.value) return
  attendanceChart?.dispose()
  attendanceChart = echarts.init(attendanceChartRef.value)
  const rows = (attendance.value.dailyTrend || []).slice(-14)
  attendanceChart.setOption({
    ...baseOption,
    grid: { left: 8, right: 8, top: 14, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category', boundaryGap: false, data: rows.map(d => (d.date || '').slice(5)),
      axisLine: { lineStyle: { color: AXIS.line } }, axisTick: { show: false },
      axisLabel: { color: AXIS.label, fontSize: 9, hideOverlap: true }
    },
    yAxis: { type: 'value', max: 100, axisLabel: { color: AXIS.label, fontSize: 10, formatter: '{value}%' }, splitLine: { lineStyle: { color: AXIS.line } } },
    series: [{
      type: 'line', smooth: true, symbol: 'none',
      data: rows.map(d => d.attendanceRate),
      lineStyle: { color: '#a78bfa', width: 2 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(167, 139, 250, 0.32)' },
        { offset: 1, color: 'rgba(167, 139, 250, 0.02)' }
      ]) }
    }]
  })
}

// ===== 业务视图图表 =====
const bizBarOption = (rows, nameKey, valueKey, color, unit = '$') => ({
  ...baseOption,
  grid: { left: 8, right: 46, top: 6, bottom: 4, containLabel: true },
  xAxis: {
    type: 'value',
    axisLabel: { color: AXIS.label, fontSize: 9, formatter: (v) => (Math.abs(v) >= 10000 ? (v / 10000) + '万' : v) },
    splitLine: { lineStyle: { color: AXIS.line } }
  },
  yAxis: {
    type: 'category',
    data: rows.map(d => d[nameKey]).reverse(),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#9fb0c7', fontSize: 9, width: 78, overflow: 'truncate' }
  },
  series: [{
    type: 'bar',
    barWidth: 9,
    data: rows.map(d => ({ value: d[valueKey], itemStyle: { borderRadius: [0, 4, 4, 0], color } })),
    label: { show: true, position: 'right', color: '#93a4bb', fontSize: 9, formatter: (p) => unit + fmtCompact(p.value) }
  }]
})

const initBizTrendChart = () => {
  if (!bizTrendChartRef.value) return
  bizTrendChart?.dispose()
  bizTrendChart = echarts.init(bizTrendChartRef.value)
  const { months = [], sales = [], orderCounts = [] } = bizTrend.value || {}
  bizTrendChart.setOption({
    ...baseOption,
    grid: { left: 10, right: 14, top: 26, bottom: 4, containLabel: true },
    legend: { data: ['销售额', '订单量'], top: 0, right: 0, itemWidth: 9, itemHeight: 9, textStyle: { color: '#7b8ca6', fontSize: 10 } },
    xAxis: {
      type: 'category', data: months,
      axisLine: { lineStyle: { color: AXIS.line } }, axisTick: { show: false },
      axisLabel: { color: AXIS.label, fontSize: 10, hideOverlap: true }
    },
    yAxis: [
      {
        type: 'value', name: 'USD', nameTextStyle: { color: AXIS.label, fontSize: 10 },
        axisLabel: { color: AXIS.label, fontSize: 10, formatter: (v) => (v >= 10000 ? (v / 10000) + '万' : v) },
        splitLine: { lineStyle: { color: AXIS.line } }
      },
      {
        type: 'value', name: '单', nameTextStyle: { color: AXIS.label, fontSize: 10 },
        axisLabel: { color: AXIS.label, fontSize: 10 }, splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '销售额', type: 'bar', barWidth: '42%', data: sales,
        itemStyle: {
          borderRadius: [3, 3, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#22d3ee' },
            { offset: 1, color: 'rgba(34, 211, 238, 0.12)' }
          ])
        }
      },
      {
        name: '订单量', type: 'line', yAxisIndex: 1, smooth: true, symbol: 'circle', symbolSize: 4,
        data: orderCounts, itemStyle: { color: '#fbbf24' }, lineStyle: { color: '#fbbf24', width: 2 }
      }
    ]
  })
}

const initBizCountryChart = () => {
  if (!bizCountryChartRef.value) return
  bizCountryChart?.dispose()
  bizCountryChart = echarts.init(bizCountryChartRef.value)
  const rows = bizCountries.value || []
  bizCountryChart.setOption({
    ...baseOption,
    tooltip: { ...baseOption.tooltip, trigger: 'item', formatter: '{b}: ${c} ({d}%)' },
    legend: { bottom: 0, left: 'center', itemWidth: 8, itemHeight: 8, textStyle: { color: '#7b8ca6', fontSize: 9 } },
    series: [{
      type: 'pie',
      radius: ['40%', '66%'],
      center: ['50%', '44%'],
      itemStyle: { borderRadius: 5, borderColor: 'rgba(5, 11, 24, 0.9)', borderWidth: 2 },
      label: { color: '#cbd5e1', fontSize: 9, formatter: '{b}\n{d}%' },
      data: rows.map((d, i) => ({
        value: d.totalSales,
        name: d.country || '未知',
        itemStyle: { color: PALETTE[i % PALETTE.length] }
      }))
    }]
  })
}

const initBizCustomerChart = () => {
  if (!bizCustomerChartRef.value) return
  bizCustomerChart?.dispose()
  bizCustomerChart = echarts.init(bizCustomerChartRef.value)
  bizCustomerChart.setOption(bizBarOption((bizCustomers.value || []).slice(0, 10), 'customerName', 'totalSales', '#34d399'))
}

const initBizProductChart = () => {
  if (!bizProductChartRef.value) return
  bizProductChart?.dispose()
  bizProductChart = echarts.init(bizProductChartRef.value)
  bizProductChart.setOption(bizBarOption((bizProducts.value || []).slice(0, 10), 'productName', 'totalSales', '#38bdf8'))
}

const initBizInventoryChart = () => {
  if (!bizInventoryChartRef.value) return
  bizInventoryChart?.dispose()
  bizInventoryChart = echarts.init(bizInventoryChartRef.value)
  const rows = bizInventory.value || []
  bizInventoryChart.setOption({
    ...baseOption,
    grid: { left: 10, right: 10, top: 14, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category', data: rows.map(d => d.warehouseName),
      axisLine: { lineStyle: { color: AXIS.line } }, axisTick: { show: false },
      axisLabel: { color: AXIS.label, fontSize: 9, interval: 0, rotate: rows.length > 4 ? 22 : 0 }
    },
    yAxis: {
      type: 'value', name: 'CNY', nameTextStyle: { color: AXIS.label, fontSize: 10 },
      axisLabel: { color: AXIS.label, fontSize: 9, formatter: (v) => (v >= 10000 ? (v / 10000) + '万' : v) },
      splitLine: { lineStyle: { color: AXIS.line } }
    },
    series: [{
      type: 'bar',
      barWidth: '44%',
      data: rows.map(d => ({
        value: d.totalValue,
        itemStyle: { borderRadius: [4, 4, 0, 0], color: d.warehouseType === 2 ? '#fbbf24' : '#22d3ee' }
      })),
      label: { show: true, position: 'top', color: '#93a4bb', fontSize: 9, formatter: (p) => '¥' + fmtCompact(p.value) }
    }]
  })
}

const initBizProfitChart = () => {
  if (!bizProfitChartRef.value) return
  bizProfitChart?.dispose()
  bizProfitChart = echarts.init(bizProfitChartRef.value)
  const rows = (bizProfit.value.byProduct || []).slice(0, 10)
  bizProfitChart.setOption(bizBarOption(rows, 'name', 'profitUsd', '#a78bfa'))
}

// 二次贝塞尔采样，形成跨洋弧线
const buildRouteCurve = (p0, p2, samples = 48) => {
  const mid = [(p0[0] + p2[0]) / 2, (p0[1] + p2[1]) / 2]
  const dx = p2[0] - p0[0]
  const dy = p2[1] - p0[1]
  const ctrl = [mid[0] - dy * 0.16, mid[1] + dx * 0.16]
  const pts = []
  for (let i = 0; i <= samples; i++) {
    const t = i / samples
    const mt = 1 - t
    pts.push([
      mt * mt * p0[0] + 2 * mt * t * ctrl[0] + t * t * p2[0],
      mt * mt * p0[1] + 2 * mt * t * ctrl[1] + t * t * p2[1]
    ])
  }
  return pts
}

const updateShipmentFrame = () => {
  if (!bizShipmentMap) return
  const passed = []
  const moving = []
  shipmentRoutes.forEach((it) => {
    const total = it.curve.length - 1
    const idx = Math.max(0, Math.min(total, Math.round(it.progress * total)))
    passed.push({ id: `passed-${it.id}`, data: [{ coords: it.curve.slice(0, Math.max(2, idx + 1)) }] })
    if (it.status === 2) {
      const cur = it.curve[idx]
      moving.push({
        value: [...cur, 1],
        name: it.shipmentNo,
        portFrom: it.portFrom,
        portTo: it.portTo,
        status: it.status
      })
    }
  })
  bizShipmentMap.setOption({ series: [...passed, { id: 'ships', data: moving }] })
}

const initBizShipmentMap = () => {
  if (!bizShipmentMapRef.value) return
  if (!worldRegistered) {
    echarts.registerMap('world', worldGeo)
    worldRegistered = true
  }
  if (bizShipmentTimer) {
    clearInterval(bizShipmentTimer)
    bizShipmentTimer = null
  }
  bizShipmentMap?.dispose()
  bizShipmentMap = echarts.init(bizShipmentMapRef.value)

  shipmentRoutes = (bizShipments.value || [])
    .map((s) => {
      const p0 = getCoord(s.portFrom)
      const p2 = getCoord(s.portTo)
      if (!p0 || !p2) return null
      return {
        ...s,
        p0,
        p2,
        curve: buildRouteCurve(p0, p2),
        progress: SHIP_BASE_PROGRESS[s.status] ?? 0.05
      }
    })
    .filter(Boolean)

  bizShipmentMap.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(56, 189, 248, 0.4)',
      textStyle: { color: '#e2e8f0', fontSize: 12 },
      formatter: (p) => {
        if (!p.data) return ''
        // 17TRACK 实时查单节点
        if (p.data.content) {
          return `<b>${p.data.location || '-'}</b><br/>${p.data.content}<br/><span style="color:#7b8ca6">${p.data.trackTime || ''}</span>`
        }
        if (p.data.node) {
          return `<b>${p.data.location || '-'}</b><br/>${p.data.node}<br/><span style="color:#7b8ca6">${p.data.trackTime || ''}</span>`
        }
        if (p.data.portFrom) {
          return `<b>${p.data.name}</b><br/>${p.data.portFrom} → ${p.data.portTo}<br/>状态：${SHIP_STATUS_TEXT[p.data.status] || '-'}`
        }
        return p.name
      }
    },
    geo: {
      map: 'world',
      roam: false,
      zoom: 1.02,
      itemStyle: {
        areaColor: 'rgba(30, 58, 95, 0.45)',
        borderColor: 'rgba(56, 189, 248, 0.22)',
        borderWidth: 0.5
      },
      emphasis: { itemStyle: { areaColor: 'rgba(45, 80, 130, 0.6)' }, label: { show: false } },
      silent: true
    },
    series: [
      ...shipmentRoutes.map((it) => ({
        id: `rest-${it.id}`,
        type: 'lines',
        coordinateSystem: 'geo',
        polyline: true,
        data: [{ coords: it.curve }],
        silent: true,
        lineStyle: { color: 'rgba(148, 163, 184, 0.4)', width: 1.2, type: 'dashed' }
      })),
      ...shipmentRoutes.map((it) => ({
        id: `passed-${it.id}`,
        type: 'lines',
        coordinateSystem: 'geo',
        polyline: true,
        data: [],
        silent: true,
        lineStyle: {
          color: shipStatusColor(it.status),
          width: 2.2,
          shadowBlur: 8,
          shadowColor: shipStatusColor(it.status)
        }
      })),
      ...shipmentRoutes.map((it) => ({
        id: `flow-${it.id}`,
        type: 'lines',
        coordinateSystem: 'geo',
        polyline: true,
        data: [{ coords: it.curve }],
        lineStyle: { width: 0 },
        effect: {
          show: true,
          period: 3.6,
          trailLength: 0.2,
          symbol: 'circle',
          symbolSize: 3,
          color: shipStatusColor(it.status)
        },
        silent: true,
        zlevel: 2
      })),
      {
        id: 'ships',
        type: 'effectScatter',
        coordinateSystem: 'geo',
        data: [],
        rippleEffect: { brushType: 'stroke', scale: 3 },
        symbolSize: 9,
        itemStyle: { color: '#22d3ee', shadowBlur: 10, shadowColor: '#22d3ee' },
        zlevel: 3
      }
    ]
  })

  updateShipmentFrame()

  // 视图切换 / 发货单变化重建地图后，把已查到的实时轨迹补绘回来
  if (liveTrail) applyLiveTrail()

  // 在途单据沿航线持续推进，形成动态位置
  bizShipmentTimer = setInterval(() => {
    let changed = false
    shipmentRoutes.forEach((it) => {
      if (it.status === 2 && it.progress < 0.93) {
        it.progress = Math.min(0.93, it.progress + 0.0045)
        changed = true
      }
    })
    if (changed) updateShipmentFrame()
  }, 220)
}

// 实时查单系列固定 id，便于按 id 增量更新 / 清空
const LIVE_SERIES = ['live-route', 'live-flow', 'live-stop', 'live-start', 'live-now']

// 把 17TRACK 实时轨迹叠加到地图：真实地点连线 + 光流 + 起点/当前节点
const applyLiveTrail = () => {
  if (!bizShipmentMap || !liveTrail) return
  const { points } = liveTrail
  const coords = points.map((p) => p.coord)
  const first = points[0]
  const last = points[points.length - 1]

  // 自动聚焦到轨迹范围，避免国内/同城轨迹在世界地图上挤成一点
  const lons = coords.map((c) => c[0])
  const lats = coords.map((c) => c[1])
  const spanLon = Math.max(...lons) - Math.min(...lons)
  const spanLat = Math.max(...lats) - Math.min(...lats)
  const span = Math.max(spanLon, spanLat * 1.6, 8)
  const zoom = Math.min(5, Math.max(1.02, 300 / span))
  const center = [(Math.max(...lons) + Math.min(...lons)) / 2, (Math.max(...lats) + Math.min(...lats)) / 2]

  bizShipmentMap.setOption({
    geo: { center, zoom },
    series: [
      {
        id: 'live-route',
        type: 'lines',
        coordinateSystem: 'geo',
        polyline: true,
        data: [{ coords }],
        silent: true,
        lineStyle: { color: '#38bdf8', width: 2.4, shadowBlur: 12, shadowColor: '#38bdf8' },
        zlevel: 4
      },
      {
        id: 'live-flow',
        type: 'lines',
        coordinateSystem: 'geo',
        polyline: true,
        data: [{ coords }],
        lineStyle: { width: 0 },
        effect: { show: true, period: 2.8, trailLength: 0.3, symbol: 'circle', symbolSize: 4, color: '#34d399' },
        silent: true,
        zlevel: 5
      },
      {
        id: 'live-stop',
        type: 'scatter',
        coordinateSystem: 'geo',
        data: points.slice(0, -1).map((p) => ({
          value: [...p.coord, 1], name: p.location, node: p.node, content: p.content, trackTime: p.trackTime
        })),
        symbolSize: 5,
        itemStyle: { color: 'rgba(148, 163, 184, 0.9)' },
        zlevel: 4
      },
      {
        id: 'live-start',
        type: 'effectScatter',
        coordinateSystem: 'geo',
        data: [{ value: [...first.coord, 1], name: first.location, node: first.node, content: first.content, trackTime: first.trackTime }],
        rippleEffect: { brushType: 'stroke', scale: 2.4 },
        symbolSize: 7,
        itemStyle: { color: '#38bdf8', shadowBlur: 8, shadowColor: '#38bdf8' },
        zlevel: 5
      },
      {
        id: 'live-now',
        type: 'effectScatter',
        coordinateSystem: 'geo',
        data: [{ value: [...last.coord, 1], name: last.location, node: last.node, content: last.content, trackTime: last.trackTime }],
        rippleEffect: { brushType: 'stroke', scale: 3.6 },
        symbolSize: 11,
        itemStyle: { color: '#34d399', shadowBlur: 14, shadowColor: '#34d399' },
        label: { show: true, position: 'right', color: '#34d399', fontSize: 10, formatter: '当前节点' },
        zlevel: 6
      }
    ]
  })
}

// 实时查单：输入真实快递单号 → 17TRACK → 按节点地点绘制真实轨迹
const handleLiveQuery = async () => {
  const no = liveQueryNo.value.trim()
  if (!no) {
    ElMessage.warning('请输入快递单号')
    return
  }
  if (!bizShipmentMap) {
    ElMessage.warning('物流地图尚未加载完成，请稍后重试')
    return
  }
  liveQueryLoading.value = true
  try {
    const res = await queryLiveTrackByNoApi(no)
    const nodes = res.data?.nodes || []
    const points = nodes
      .map((n) => ({ ...n, coord: getCoordSmart(n.location) }))
      .filter((n) => n.coord)
    if (points.length < 2) {
      ElMessage.warning(points.length ? '仅识别到 1 个轨迹地点，无法绘制航线' : '未识别到可用的轨迹地点坐标，无法绘制航线')
      return
    }
    liveTrail = { no, points }
    liveTrailNo.value = no
    liveNodeCount.value = nodes.length
    applyLiveTrail()
    ElMessage.success(`已获取 ${nodes.length} 个实时节点，地图绘制 ${points.length} 个地点`)
  } catch (e) {
    // 响应拦截器已提示后端返回的具体原因（如承运商无法识别），保留原多单航线
  } finally {
    liveQueryLoading.value = false
  }
}

// 清除实时轨迹，恢复默认世界视角与多单航线
const clearLiveTrail = () => {
  liveTrail = null
  liveTrailNo.value = ''
  liveNodeCount.value = 0
  bizShipmentMap?.setOption({
    geo: { center: [0, 0], zoom: 1.02 },
    series: LIVE_SERIES.map((id) => ({ id, data: [] }))
  })
}

const initHrCharts = () => {
  initDeptChart()
  initSalaryChart()
  initGaugeChart()
  initTrendChart()
  initPerfChart()
  initAttendanceChart()
}

const initBizCharts = () => {
  initBizTrendChart()
  initBizCountryChart()
  initBizCustomerChart()
  initBizProductChart()
  initBizInventoryChart()
  initBizProfitChart()
  initBizShipmentMap()
}

// 按当前视图初始化对应的一组图表
const initActiveCharts = () => {
  if (viewMode.value === 'hr') initHrCharts()
  else initBizCharts()
}

// ===== 数据加载 =====
const loadAll = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const [s, st, att, sal, perf, pred, trd, acts, ai, biz,
      bTrend, bCust, bProd, bCountry, bInv, bProfit, bWarn, bShip] = await Promise.all([
      getDashboardStatsApi(),
      getEmployeeStructureApi(),
      getAttendanceAnalysisApi(),
      getSalaryAnalysisApi(),
      getPerformanceAnalysisApi(),
      getTalentPredictionApi(),
      getTurnoverTrendApi(12),
      getRecentActivitiesApi(20),
      getAiInsightsApi().catch(() => null),
      getBusinessOverviewApi().catch(() => null),
      getMonthlySalesApi().catch(() => null),
      getTopCustomersApi().catch(() => null),
      getTopProductsApi().catch(() => null),
      getCountryDistributionApi().catch(() => null),
      getInventoryValueApi().catch(() => null),
      getProfitRankingApi().catch(() => null),
      getInventoryWarningCountApi().catch(() => null),
      getShipmentPageApi({ pageNum: 1, pageSize: 100 }).catch(() => null)
    ])

    stats.value = s.data || {}
    structure.value = st.data || {}
    attendance.value = att.data || {}
    salary.value = sal.data || {}
    perfData.value = perf.data || {}
    prediction.value = pred.data || {}
    trend.value = trd.data || []
    business.value = (biz && biz.data) || {}

    // 业务视图数据
    if (bTrend && bTrend.data) bizTrend.value = bTrend.data
    if (Array.isArray(bCust?.data)) bizCustomers.value = bCust.data
    if (Array.isArray(bProd?.data)) bizProducts.value = bProd.data
    if (Array.isArray(bCountry?.data)) bizCountries.value = bCountry.data
    if (Array.isArray(bInv?.data)) bizInventory.value = bInv.data
    if (bProfit && bProfit.data) bizProfit.value = bProfit.data
    if (bWarn && bWarn.data !== undefined) warningCount.value = Number(bWarn.data) || 0

    // 物流航线：取发货单列表（含起运港/目的港/状态）
    const shipList = Array.isArray(bShip?.data?.list)
      ? bShip.data.list
      : (Array.isArray(bShip?.data) ? bShip.data : [])
    const shipKey = shipList.map((r) => {
      const s = r.shipment || r
      return `${s.id}:${s.status}:${s.portFrom}>${s.portTo}`
    }).join(',')
    bizShipments.value = shipList.map((r) => r.shipment || r)
    // 新增发货单或状态变化时重建航线图，保证新轨迹自动出现
    if (shipKey !== lastShipKey) {
      lastShipKey = shipKey
      if (viewMode.value === 'business' && bizShipmentMapRef.value) {
        setTimeout(() => initBizShipmentMap(), 60)
      }
    }

    const actList = Array.isArray(acts.data) ? acts.data : (acts.data?.list || [])
    activities.value = actList.map((a, i) => ({ ...a, uid: `${i}-${a.at || a.time || ''}` }))

    if (ai && ai.data) {
      const list = Array.isArray(ai.data) ? ai.data : (ai.data.list || [])
      insightText.value = list[0]?.content || ai.data.trendComment || ''
    }

    tweenTo('total', stats.value.totalEmployees)
    tweenTo('active', stats.value.activeEmployees)

    await nextTick()
    initActiveCharts()
    // 布局（flex/grid）稳定后再校正一次尺寸，避免首次渲染容器尺寸未定导致图表空白
    setTimeout(() => handleResize(), 260)

    pulse.value = true
    setTimeout(() => { pulse.value = false }, 700)
  } catch (e) {
    console.error('Cockpit load error:', e)
    ElMessage.error('驾驶舱数据加载失败')
  } finally {
    loading.value = false
  }
}

const refreshAll = () => loadAll()

// ===== 时钟 / 轮播 / 定时刷新 =====
let clockTimer = null
let streamTimer = null
let refreshTimer = null

const updateClock = () => {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  clockText.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const toggleFullscreen = async () => {
  try {
    if (!document.fullscreenElement) {
      await document.documentElement.requestFullscreen()
    } else {
      await document.exitFullscreen()
    }
  } catch (e) {
    ElMessage.warning('当前浏览器不支持全屏，可使用 F11')
  }
}

const goBack = () => router.push('/dashboard')

const handleResize = () => {
  deptChart?.resize()
  salaryChart?.resize()
  gaugeChart?.resize()
  trendChart?.resize()
  perfChart?.resize()
  attendanceChart?.resize()
  bizTrendChart?.resize()
  bizCountryChart?.resize()
  bizCustomerChart?.resize()
  bizProductChart?.resize()
  bizInventoryChart?.resize()
  bizProfitChart?.resize()
  bizShipmentMap?.resize()
}

// 视图切换后，DOM 重建，需重新初始化当前视图的图表
watch(viewMode, async (mode) => {
  await nextTick()
  if (mode === 'hr') {
    // 离开业务视图时停止物流动画并销毁地图实例
    if (bizShipmentTimer) {
      clearInterval(bizShipmentTimer)
      bizShipmentTimer = null
    }
    bizShipmentMap?.dispose()
    bizShipmentMap = null
    initHrCharts()
  } else {
    // 等业务视图布局完成（容器有宽度）后再初始化，避免 canvas 宽度为 0
    setTimeout(() => {
      initBizCharts()
      handleResize()
    }, 60)
  }
  setTimeout(() => handleResize(), 160)
})

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  loadAll()
  // 数据流每 4 秒向前滚动一条
  streamTimer = setInterval(() => { streamOffset.value += 1 }, 4000)
  // 每 30 秒静默刷新，保证驾驶舱数据（含新增物流轨迹）不陈旧
  refreshTimer = setInterval(() => loadAll(true), 30000)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  clearInterval(clockTimer)
  clearInterval(streamTimer)
  clearInterval(refreshTimer)
  window.removeEventListener('resize', handleResize)
  Object.values(rafIds).forEach(id => cancelAnimationFrame(id))
  if (bizShipmentTimer) clearInterval(bizShipmentTimer)
  bizShipmentMap?.dispose()
  deptChart?.dispose()
  salaryChart?.dispose()
  gaugeChart?.dispose()
  trendChart?.dispose()
  perfChart?.dispose()
  attendanceChart?.dispose()
  bizTrendChart?.dispose()
  bizCountryChart?.dispose()
  bizCustomerChart?.dispose()
  bizProductChart?.dispose()
  bizInventoryChart?.dispose()
  bizProfitChart?.dispose()
  if (document.fullscreenElement) document.exitFullscreen().catch(() => {})
})
</script>

<style scoped lang="scss">
.cockpit {
  position: relative;
  min-height: 100vh;
  padding: 16px 20px 18px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-x: hidden;
  overflow-y: auto;
  background: radial-gradient(1200px 600px at 50% -10%, #0d1f3c 0%, #071021 45%, #04080f 100%);
  color: #e2e8f0;
  font-variant-numeric: tabular-nums;
}

/* 背景网格 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(56, 189, 248, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(56, 189, 248, 0.055) 1px, transparent 1px);
  background-size: 46px 46px;
  pointer-events: none;
}

.bg-glow {
  position: absolute;
  inset: -30% -10%;
  background: radial-gradient(600px 300px at 50% 110%, rgba(34, 211, 238, 0.16), transparent 70%);
  pointer-events: none;
}

/* 扫描光线 */
.scan-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 140px;
  background: linear-gradient(180deg, transparent, rgba(34, 211, 238, 0.055), transparent);
  animation: scan 7s linear infinite;
  pointer-events: none;
}

@keyframes scan {
  0% { top: -140px; }
  100% { top: 100%; }
}

/* ===== 顶栏 ===== */
.cp-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border: 1px solid rgba(56, 189, 248, 0.22);
  border-radius: 10px;
  background: linear-gradient(90deg, rgba(10, 24, 44, 0.92), rgba(8, 18, 34, 0.72));
  box-shadow: 0 0 26px rgba(34, 211, 238, 0.10) inset;
}

.cp-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cp-logo {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  color: #22d3ee;
  background: rgba(34, 211, 238, 0.10);
  border: 1px solid rgba(34, 211, 238, 0.35);
  box-shadow: 0 0 16px rgba(34, 211, 238, 0.28);
}

.cp-title {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 3px;
  background: linear-gradient(90deg, #e2f6ff, #22d3ee 60%, #38bdf8);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.cp-sub {
  margin-top: 2px;
  font-size: 11px;
  letter-spacing: 1px;
  color: #64748b;
}

.cp-switch {
  display: flex;
  padding: 3px;
  border-radius: 8px;
  border: 1px solid rgba(56, 189, 248, 0.25);
  background: rgba(6, 14, 28, 0.6);

  button {
    padding: 6px 18px;
    font-size: 12px;
    letter-spacing: 1px;
    color: #7b8ca6;
    background: transparent;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.25s;

    &:hover { color: #22d3ee; }

    &.active {
      color: #050b18;
      font-weight: 600;
      background: linear-gradient(90deg, #22d3ee, #38bdf8);
      box-shadow: 0 0 14px rgba(34, 211, 238, 0.45);
    }
  }
}

.cp-header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.cp-live {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #34d399;
  letter-spacing: 1px;
}

.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 8px #34d399;
  animation: blink 1.4s ease-in-out infinite;

  &.small { width: 6px; height: 6px; }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.25; }
}

.cp-clock {
  font-size: 13px;
  color: #94a3b8;
  letter-spacing: 1px;
}

.cp-btn {
  color: #7b8ca6;
  font-size: 12px;

  &:hover { color: #22d3ee; }
}

.cp-exit {
  height: 30px;
  padding: 0 14px;
  font-size: 12px;
  color: #bae6fd;
  background: rgba(34, 211, 238, 0.10);
  border: 1px solid rgba(34, 211, 238, 0.40);
  border-radius: 7px;

  &:hover {
    background: rgba(34, 211, 238, 0.20);
    color: #fff;
  }
}

/* ===== KPI 带 ===== */
.cp-kpi-row {
  position: relative;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.cp-kpi {
  position: relative;
  padding: 10px 12px 8px;
  border-radius: 9px;
  border: 1px solid rgba(56, 189, 248, 0.16);
  background: linear-gradient(160deg, rgba(13, 30, 54, 0.88), rgba(7, 16, 33, 0.72));
  overflow: hidden;
  transition: transform 0.3s, border-color 0.3s, box-shadow 0.3s;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    width: 3px;
    height: 100%;
    background: var(--kpi-color);
    box-shadow: 0 0 10px var(--kpi-color);
  }

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(56, 189, 248, 0.38);
    box-shadow: 0 6px 22px rgba(34, 211, 238, 0.12);
  }
}

.pulsing .cp-kpi {
  animation: kpiPulse 0.7s ease-out;
}

@keyframes kpiPulse {
  0% { box-shadow: 0 0 0 rgba(34, 211, 238, 0); }
  40% { box-shadow: 0 0 22px rgba(34, 211, 238, 0.35); }
  100% { box-shadow: 0 0 0 rgba(34, 211, 238, 0); }
}

.kpi-head {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--kpi-color);
}

.kpi-value {
  margin-top: 4px;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.kpi-num {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.05;
  color: #f1f5f9;
  text-shadow: 0 0 16px rgba(226, 246, 255, 0.18);
}

.kpi-unit {
  font-size: 11px;
  color: #7b8ca6;
}

.kpi-foot {
  margin-top: 3px;
  font-size: 10px;
  color: #5b6b82;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kpi-bar {
  margin-top: 7px;
  height: 2px;
  border-radius: 2px;
  background: rgba(148, 163, 184, 0.14);
  overflow: hidden;

  i {
    display: block;
    height: 100%;
    background: var(--kpi-color);
    box-shadow: 0 0 8px var(--kpi-color);
    transition: width 1s ease;
  }
}

/* ===== 主体 ===== */
.cp-main {
  flex: 1 1 auto;
  min-height: 430px;
  display: grid;
  grid-template-columns: 1fr 1.25fr 1fr;
  gap: 12px;
}

.cp-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

/* 业务：全球物流实时航线 */
.cp-logistics {
  flex: 0 0 250px;
  display: flex;
  min-height: 250px;

  .cp-panel {
    flex: 1;
    min-height: 0;
  }

  .panel-chart {
    min-height: 180px;
  }

  /* 实时查单输入区（深色驾驶舱皮肤） */
  .live-query {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-left: auto;
    margin-right: 14px;

    .live-input {
      width: 260px;
    }

    :deep(.el-input__wrapper) {
      background: rgba(6, 16, 30, 0.92);
      box-shadow: 0 0 0 1px rgba(56, 189, 248, 0.26) inset;
      padding: 1px 8px;
    }

    :deep(.el-input__wrapper.is-focus) {
      box-shadow: 0 0 0 1px rgba(34, 211, 238, 0.65) inset;
    }

    :deep(.el-input__inner) {
      color: #cbd5e1;
      font-size: 11px;
      height: 24px;
    }

    :deep(.el-input__inner::placeholder) {
      color: #5b6b82;
    }

    :deep(.el-button--primary) {
      background: rgba(34, 211, 238, 0.14);
      border-color: rgba(34, 211, 238, 0.45);
      color: #22d3ee;
    }

    :deep(.el-button--primary:hover) {
      background: rgba(34, 211, 238, 0.24);
      border-color: rgba(34, 211, 238, 0.7);
      color: #67e8f9;
    }

    .live-clear {
      color: #7b8ca6;
      font-size: 11px;
    }
  }
}

.cp-panel {
  position: relative;
  flex: 1 1 0;
  min-height: 190px;
  display: flex;
  flex-direction: column;
  padding: 9px 12px 6px;
  border-radius: 9px;
  border: 1px solid rgba(56, 189, 248, 0.16);
  background: linear-gradient(160deg, rgba(12, 27, 49, 0.86), rgba(6, 14, 28, 0.78));
  overflow: hidden;

  /* 四角切角装饰 */
  &::after {
    content: '';
    position: absolute;
    right: -1px;
    top: -1px;
    width: 14px;
    height: 14px;
    border-top: 1px solid rgba(34, 211, 238, 0.55);
    border-right: 1px solid rgba(34, 211, 238, 0.55);
    border-top-right-radius: 9px;
    pointer-events: none;
  }
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2px;
}

.panel-name {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: #cbd5e1;

  &::before {
    content: '';
    display: inline-block;
    width: 3px;
    height: 10px;
    margin-right: 7px;
    vertical-align: middle;
    background: #22d3ee;
    box-shadow: 0 0 7px #22d3ee;
  }
}

.panel-tag {
  font-size: 10px;
  color: #5b6b82;
  letter-spacing: 0.5px;
}

.panel-chart {
  flex: 1 1 auto;
  min-height: 140px;
  width: 100%;
}

/* 中央核心 */
.panel-core {
  flex: 1.15 1 0;
  min-height: 250px;
  flex-direction: row;
  align-items: center;
  gap: 6px;
  padding: 6px 14px 6px 6px;
}

.core-ring {
  position: relative;
  flex: 1;
  height: 100%;
  min-width: 0;
  display: grid;
  place-items: center;
}

.core-chart {
  position: absolute;
  inset: 0;
}

.core-center {
  position: relative;
  text-align: center;
  pointer-events: none;
}

.core-num {
  font-size: 40px;
  font-weight: 800;
  line-height: 1;
  color: #f0f9ff;
  text-shadow: 0 0 24px rgba(34, 211, 238, 0.55);
}

.core-label {
  margin-top: 4px;
  font-size: 11px;
  letter-spacing: 3px;
  color: #22d3ee;
}

.core-sub {
  margin-top: 3px;
  font-size: 10px;
  color: #5b6b82;
}

.core-side {
  width: 128px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-left: 1px solid rgba(56, 189, 248, 0.16);
  padding-left: 12px;
}

.core-side-item {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.csi-label {
  font-size: 10px;
  color: #5b6b82;
  letter-spacing: 0.5px;
}

.csi-value {
  font-size: 14px;
  font-weight: 600;
  color: #38bdf8;

  &.warn { color: #fbbf24; }
}

/* ===== 底部数据流 ===== */
.cp-stream {
  position: relative;
  flex-shrink: 0;
  padding: 8px 14px 10px;
  border-radius: 9px;
  border: 1px solid rgba(56, 189, 248, 0.16);
  background: linear-gradient(90deg, rgba(10, 24, 44, 0.9), rgba(6, 14, 28, 0.8));
}

.stream-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.stream-title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #cbd5e1;
}

.stream-hint {
  font-size: 10px;
  color: #475569;
}

.stream-body {
  display: flex;
  gap: 14px;
  align-items: stretch;
}

.stream-list {
  position: relative;
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow: hidden;
}

.stream-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 8px;
  border-radius: 5px;
  font-size: 11px;
  background: rgba(56, 189, 248, 0.045);
  border-left: 2px solid rgba(56, 189, 248, 0.35);

  &.type-approved { border-left-color: #34d399; }
  &.type-pending { border-left-color: #fbbf24; }
  &.type-rejected { border-left-color: #f87171; }
  &.type-leave { border-left-color: #a78bfa; }
}

.si-type {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 9px;
  color: #22d3ee;
  background: rgba(34, 211, 238, 0.12);
}

.si-content {
  flex: 1;
  min-width: 0;
  color: #b6c4d8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.si-time {
  flex-shrink: 0;
  font-size: 10px;
  color: #4a5a70;
}

.stream-empty {
  padding: 8px;
  font-size: 11px;
  color: #4a5a70;
}

.stream-metrics {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(4, auto);
  gap: 18px;
  padding-left: 14px;
  border-left: 1px solid rgba(56, 189, 248, 0.16);
  align-content: center;
}

.sm-label {
  font-size: 10px;
  color: #5b6b82;
}

.sm-value {
  margin-top: 1px;
  font-size: 15px;
  font-weight: 600;
  color: #fbbf24;
  white-space: nowrap;
}

/* 数据流滚动动画 */
.flow-enter-active,
.flow-leave-active {
  transition: all 0.5s ease;
}

.flow-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.flow-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.flow-move {
  transition: transform 0.5s ease;
}

/* ===== AI 洞察字幕 ===== */
.cp-insight {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 14px;
  border-radius: 8px;
  border: 1px dashed rgba(34, 211, 238, 0.28);
  background: rgba(34, 211, 238, 0.045);
}

.insight-label {
  flex-shrink: 0;
  padding: 2px 9px;
  border-radius: 4px;
  font-size: 10px;
  letter-spacing: 1px;
  color: #050b18;
  font-weight: 700;
  background: linear-gradient(90deg, #22d3ee, #38bdf8);
}

.insight-text {
  font-size: 11.5px;
  color: #a5b8cf;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 窄屏兜底：不隐藏任何数据列，改为纵向堆叠 + 页面滚动，保证图表都有尺寸 */
@media (max-width: 1500px) {
  .cp-kpi-row { grid-template-columns: repeat(3, 1fr); }
  .cp-main {
    grid-template-columns: 1fr 1fr;
    grid-auto-rows: minmax(220px, auto);
  }
  .cp-col-right { grid-column: 1 / -1; flex-direction: row; }
}

@media (max-width: 1000px) {
  .cp-main { grid-template-columns: 1fr; }
  .cp-col-right { grid-column: auto; flex-direction: column; }
  .cp-header { flex-wrap: wrap; gap: 10px; }
  .stream-body { flex-direction: column; gap: 8px; }
  .stream-metrics {
    grid-template-columns: repeat(4, 1fr);
    padding-left: 0;
    border-left: none;
    border-top: 1px solid rgba(56, 189, 248, 0.16);
    padding-top: 8px;
  }
  .cp-insight { display: none; }
}
</style>
