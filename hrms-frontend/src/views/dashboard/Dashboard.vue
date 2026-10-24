<template>
  <div class="cockpit" v-loading="loading" element-loading-background="rgba(5, 15, 32, 0.7)">
    <!-- 顶部标题栏 -->
    <div class="cockpit-header">
      <div class="header-left">
        <h1 class="cockpit-title">星跃出海 · 高管驾驶舱</h1>
        <span class="cockpit-subtitle">EXECUTIVE COCKPIT</span>
      </div>
      <div class="header-right">
        <span class="update-time">数据更新于 {{ updateTime }}</span>
        <el-button size="small" class="refresh-btn" :icon="Refresh" @click="loadAllData">刷新</el-button>
      </div>
    </div>

    <!-- KPI 总览条 -->
    <div class="kpi-strip">
      <div v-for="card in kpiCards" :key="card.title" class="kpi-card" :style="{ '--accent': card.color }">
        <div class="kpi-icon">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="kpi-body">
          <div class="kpi-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="kpi-label">{{ card.title }}</div>
        </div>
      </div>
    </div>

    <!-- ══════════ 第一区：经营分析（营收） ══════════ -->
    <div class="section-head">
      <div class="section-title">
        <span class="section-badge badge-biz">Ⅰ</span>
        <div>
          <h2>经营分析</h2>
          <p>营收 · 客户 · 资金 · 库存</p>
        </div>
      </div>
      <div class="section-line"></div>
    </div>

    <el-row :gutter="14" class="chart-row">
      <el-col :lg="16" :md="24" :xs="24">
        <div class="panel">
          <div class="panel-title">月度营收与订单趋势</div>
          <div ref="monthlyChartRef" class="chart chart-high"></div>
        </div>
      </el-col>
      <el-col :lg="8" :md="24" :xs="24">
        <div class="panel fund-panel">
          <div class="panel-title">资金概况</div>
          <div class="fund-grid">
            <div class="fund-item">
              <div class="fund-label">累计收款</div>
              <div class="fund-value" style="color: #3ddc97">${{ formatNum(overview?.totalReceivedUsd) }}</div>
            </div>
            <div class="fund-item">
              <div class="fund-label">累计付款</div>
              <div class="fund-value" style="color: #ff9f6b">${{ formatNum(overview?.totalPaidUsd) }}</div>
            </div>
            <div class="fund-item">
              <div class="fund-label">待收账款</div>
              <div class="fund-value" style="color: #ff6b81">${{ formatNum(overview?.pendingReceivableUsd) }}</div>
            </div>
            <div class="fund-item">
              <div class="fund-label">库存价值</div>
              <div class="fund-value" style="color: #ffd166">¥{{ formatNum(overview?.inventoryValueCny) }}</div>
            </div>
          </div>
          <div class="fund-tip">资金口径与收付款模块一致，单位：USD / CNY</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="chart-row">
      <el-col :lg="6" :md="12" :xs="24">
        <div class="panel">
          <div class="panel-title">客户贡献 TOP10</div>
          <div ref="customerChartRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :lg="6" :md="12" :xs="24">
        <div class="panel">
          <div class="panel-title">产品销售 TOP10</div>
          <div ref="productChartRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :lg="6" :md="12" :xs="24">
        <div class="panel">
          <div class="panel-title">国家 / 地区分布</div>
          <div ref="countryChartRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :lg="6" :md="12" :xs="24">
        <div class="panel">
          <div class="panel-title">仓库库存价值</div>
          <div ref="inventoryChartRef" class="chart"></div>
        </div>
      </el-col>
    </el-row>

    <!-- ══════════ 第二区：人力资源分析 ══════════ -->
    <div class="section-head">
      <div class="section-title">
        <span class="section-badge badge-hr">Ⅱ</span>
        <div>
          <h2>人力资源分析</h2>
          <p>结构 · 流动 · 薪酬 · 绩效 · 考勤</p>
        </div>
      </div>
      <div class="section-line"></div>
    </div>

    <el-row :gutter="14" class="chart-row">
      <el-col :lg="6" :md="12" :xs="24">
        <div class="panel">
          <div class="panel-title">部门人员分布</div>
          <div ref="deptChartRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :lg="6" :md="12" :xs="24">
        <div class="panel">
          <div class="panel-title structure-head">
            <span>人员结构</span>
            <el-radio-group v-model="structureType" size="small" class="dark-radio">
              <el-radio-button label="age">年龄</el-radio-button>
              <el-radio-button label="education">学历</el-radio-button>
              <el-radio-button label="gender">性别</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="structureChartRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :lg="6" :md="12" :xs="24">
        <div class="panel">
          <div class="panel-title">未来人员规模预测</div>
          <div ref="attritionChartRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :lg="6" :md="12" :xs="24">
        <div class="panel">
          <div class="panel-title">绩效等级分布</div>
          <div ref="performanceChartRef" class="chart"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="chart-row">
      <el-col :lg="8" :md="12" :xs="24">
        <div class="panel">
          <div class="panel-title">薪资区间分布</div>
          <div ref="salaryChartRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :lg="8" :md="12" :xs="24">
        <div class="panel">
          <div class="panel-title">各部门出勤率</div>
          <div ref="attendanceChartRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :lg="8" :md="12" :xs="24">
        <div class="panel">
          <div class="panel-title">招聘需求预测</div>
          <div class="hiring-table">
            <el-table :data="hiringData" size="small" class="table-dark" height="260">
              <el-table-column prop="position" label="部门" />
              <el-table-column prop="currentCount" label="现有" width="70" align="center" />
              <el-table-column prop="predictedDemand" label="需求" width="70" align="center" />
              <el-table-column prop="priority" label="紧急度" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.priority === '高' ? 'danger' : row.priority === '中' ? 'warning' : 'info'" size="small" effect="dark">
                    {{ row.priority }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- AI 智能洞察精选 -->
    <div class="panel insight-panel">
      <div class="panel-title">AI 智能洞察 · 精选</div>
      <div v-if="topInsights.length" class="insight-grid">
        <div v-for="(insight, index) in topInsights" :key="index" class="insight-item" :class="insight.type">
          <div class="insight-icon">
            <el-icon><component :is="getInsightIcon(insight.type)" /></el-icon>
          </div>
          <div class="insight-content">
            <div class="insight-title">{{ insight.title }}</div>
            <div class="insight-desc">{{ insight.description }}</div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无洞察" :image-size="60" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import { Refresh, Money, TrendCharts, Coin, Wallet, Tickets, User, UserFilled } from '@element-plus/icons-vue'
import { Warning, Lightning, InfoFilled } from '@element-plus/icons-vue'
import {
  getBusinessOverviewApi, getMonthlySalesApi, getTopCustomersApi,
  getTopProductsApi, getCountryDistributionApi, getInventoryValueApi
} from '@/api/trade'
import {
  getDashboardStatsApi, getEmployeeStructureApi, getAttendanceAnalysisApi,
  getSalaryAnalysisApi, getPerformanceAnalysisApi, getTalentPredictionApi, getAiInsightsApi
} from '@/api/analytics'

/* ---------- 深色主题公共样式 ---------- */
const TXT = '#d5e5ff'      // 主文字
const AXIS = '#8fb3e6'     // 坐标轴文字
const SPLIT = 'rgba(120, 170, 255, 0.12)'  // 分隔线
const PALETTE = ['#36d9f0', '#3ddc97', '#ffd166', '#ff6b81', '#b18cff', '#4ecdc4', '#6ea8ff', '#ffa94d']

const darkTooltip = {
  backgroundColor: 'rgba(10, 28, 55, 0.92)',
  borderColor: 'rgba(110, 165, 255, 0.35)',
  textStyle: { color: TXT, fontSize: 12 }
}
const darkCategoryAxis = (data, name) => ({
  type: 'category',
  data,
  name,
  nameTextStyle: { color: AXIS, fontSize: 10 },
  axisLine: { lineStyle: { color: 'rgba(120,170,255,.25)' } },
  axisTick: { show: false },
  axisLabel: { color: AXIS, fontSize: 10 }
})
const darkValueAxis = (name) => ({
  type: 'value',
  name,
  nameTextStyle: { color: AXIS, fontSize: 10 },
  axisLabel: { color: AXIS, fontSize: 10, formatter: (v) => (Math.abs(v) >= 10000 ? (v / 10000) + '万' : v) },
  splitLine: { lineStyle: { color: SPLIT } }
})

const fmt = (v) => (Math.abs(v) >= 10000 ? (v / 10000) + '万' : v)

/* ---------- 状态 ---------- */
const loading = ref(false)
const updateTime = ref(new Date().toLocaleString('zh-CN'))
const structureType = ref('age')

const overview = ref(null)
const monthlySales = ref({ months: [], sales: [], orderCounts: [] })
const topCustomers = ref([])
const topProducts = ref([])
const countryDistribution = ref([])
const inventoryValue = ref([])

const dashboardStats = ref(null)
const employeeStructure = ref(null)
const attendanceAnalysis = ref(null)
const salaryAnalysis = ref(null)
const performanceAnalysis = ref(null)
const talentPrediction = ref(null)
const aiInsights = ref([])

const hiringData = ref([])

/* ---------- KPI ---------- */
const formatNum = (n) => (n == null ? '0.00' : Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }))
const formatInt = (n) => (n == null ? '0' : Number(n).toLocaleString('en-US'))

const kpiCards = ref([
  { title: '累计销售额', value: '--', icon: 'Money', color: '#36d9f0' },
  { title: '本年销售额', value: '--', icon: 'TrendCharts', color: '#3ddc97' },
  { title: '毛利润', value: '--', icon: 'Coin', color: '#ffd166' },
  { title: '待收账款', value: '--', icon: 'Wallet', color: '#ff6b81' },
  { title: '订单总数', value: '--', icon: 'Tickets', color: '#b18cff' },
  { title: '境外客户', value: '--', icon: 'User', color: '#4ecdc4' },
  { title: '员工总数', value: '--', icon: 'UserFilled', color: '#6ea8ff' },
  { title: '人均薪资', value: '--', icon: 'Money', color: '#ffa94d' }
])

const updateKpi = () => {
  const s = overview.value
  const h = dashboardStats.value
  if (s) {
    kpiCards.value[0].value = '$' + formatNum(s.totalSalesUsd)
    kpiCards.value[1].value = '$' + formatNum(s.yearSalesUsd)
    kpiCards.value[2].value = '$' + formatNum(s.grossProfitUsd)
    kpiCards.value[3].value = '$' + formatNum(s.pendingReceivableUsd)
    kpiCards.value[4].value = formatInt(s.totalOrders)
    kpiCards.value[5].value = formatInt(s.totalCustomers)
  }
  if (h) {
    kpiCards.value[6].value = (h.totalEmployees || 0) + ' 人'
    kpiCards.value[7].value = h.avgSalary ? '¥' + Number(h.avgSalary).toLocaleString() : '--'
  }
}

const topInsights = computed(() => (aiInsights.value || []).slice(0, 3))

const getInsightIcon = (type) => {
  const map = { warning: 'Warning', success: 'TrendCharts', info: 'Lightning', primary: 'InfoFilled' }
  return map[type] || 'InfoFilled'
}

/* ---------- 图表引用 ---------- */
const monthlyChartRef = ref(null)
const customerChartRef = ref(null)
const productChartRef = ref(null)
const countryChartRef = ref(null)
const inventoryChartRef = ref(null)
const deptChartRef = ref(null)
const structureChartRef = ref(null)
const attritionChartRef = ref(null)
const performanceChartRef = ref(null)
const salaryChartRef = ref(null)
const attendanceChartRef = ref(null)

const charts = []

const createChart = (refEl) => {
  const inst = echarts.init(refEl)
  charts.push(inst)
  return inst
}

/* ---------- 数据加载 ---------- */
const loadAllData = async () => {
  loading.value = true
  updateTime.value = new Date().toLocaleString('zh-CN')
  try {
    const [ov, monthly, customers, products, countries, inventory,
      stats, structure, attendance, salary, performance, prediction, insights] = await Promise.all([
      getBusinessOverviewApi(), getMonthlySalesApi(), getTopCustomersApi(),
      getTopProductsApi(), getCountryDistributionApi(), getInventoryValueApi(),
      getDashboardStatsApi(), getEmployeeStructureApi(), getAttendanceAnalysisApi(),
      getSalaryAnalysisApi(), getPerformanceAnalysisApi(), getTalentPredictionApi(), getAiInsightsApi()
    ])

    overview.value = ov.data
    monthlySales.value = monthly.data || { months: [], sales: [], orderCounts: [] }
    topCustomers.value = customers.data || []
    topProducts.value = products.data || []
    countryDistribution.value = countries.data || []
    inventoryValue.value = inventory.data || []
    dashboardStats.value = stats.data
    employeeStructure.value = structure.data
    attendanceAnalysis.value = attendance.data
    salaryAnalysis.value = salary.data
    performanceAnalysis.value = performance.data
    talentPrediction.value = prediction.data
    aiInsights.value = insights.data || []
    hiringData.value = prediction.data?.recruitmentDemand || []

    updateKpi()
    await nextTick()
    initAllCharts()
  } catch (e) {
    console.error('Load cockpit data error:', e)
  } finally {
    loading.value = false
  }
}

/* ---------- 图表初始化（深色主题） ---------- */
const initAllCharts = () => {
  initMonthlyChart()
  initCustomerChart()
  initProductChart()
  initCountryChart()
  initInventoryChart()
  initDeptChart()
  initStructureChart()
  initAttritionChart()
  initPerformanceChart()
  initSalaryChart()
  initAttendanceChart()
}

// 月度营收 + 订单量
const initMonthlyChart = () => {
  if (!monthlyChartRef.value) return
  const chart = createChart(monthlyChartRef.value)
  const { months, sales, orderCounts } = monthlySales.value
  chart.setOption({
    tooltip: { trigger: 'axis', ...darkTooltip },
    legend: { data: ['销售额(USD)', '订单量'], top: 0, textStyle: { color: AXIS, fontSize: 11 } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '18%', containLabel: true },
    xAxis: darkCategoryAxis(months),
    yAxis: [
      { ...darkValueAxis('销售额(USD)'), nameGap: 12 },
      { type: 'value', name: '订单量', nameTextStyle: { color: AXIS, fontSize: 10 }, axisLabel: { color: AXIS, fontSize: 10 }, splitLine: { show: false } }
    ],
    series: [
      {
        name: '销售额(USD)',
        type: 'bar',
        barWidth: '45%',
        data: sales,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#36d9f0' },
            { offset: 1, color: 'rgba(54, 217, 240, 0.25)' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: '订单量',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: orderCounts,
        itemStyle: { color: '#ffd166' },
        lineStyle: { width: 3, color: '#ffd166' }
      }
    ]
  })
}

// 横向条形图（客户/产品/国家/库存共用）
const horizontalBarOption = (data, nameKey, valueKey, color, money = true) => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...darkTooltip, formatter: (ps) => `${ps[0].name}: ${money ? '$' + formatNum(ps[0].value) : ps[0].value}` },
  grid: { left: '3%', right: '14%', bottom: '3%', top: '5%', containLabel: true },
  xAxis: { type: 'value', axisLabel: { color: AXIS, fontSize: 10, formatter: fmt }, splitLine: { lineStyle: { color: SPLIT } } },
  yAxis: {
    type: 'category',
    data: data.map(d => d[nameKey]).reverse(),
    axisLine: { lineStyle: { color: 'rgba(120,170,255,.25)' } },
    axisTick: { show: false },
    axisLabel: { color: AXIS, fontSize: 10, width: 80, overflow: 'truncate' }
  },
  series: [{
    type: 'bar',
    data: data.map(d => d[valueKey]).reverse(),
    barWidth: '55%',
    itemStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
        { offset: 0, color: 'rgba(110, 168, 255, 0.25)' },
        { offset: 1, color }
      ]),
      borderRadius: [0, 4, 4, 0]
    },
    label: { show: true, position: 'right', color: TXT, fontSize: 10, formatter: (p) => (money ? '$' + formatNum(p.value) : formatNum(p.value)) }
  }]
})

const initCustomerChart = () => {
  if (!customerChartRef.value) return
  createChart(customerChartRef.value).setOption(horizontalBarOption(topCustomers.value.slice(0, 8), 'customerName', 'totalSales', '#3ddc97'))
}
const initProductChart = () => {
  if (!productChartRef.value) return
  createChart(productChartRef.value).setOption(horizontalBarOption(topProducts.value.slice(0, 8), 'productName', 'totalSales', '#36d9f0'))
}
const initCountryChart = () => {
  if (!countryChartRef.value) return
  createChart(countryChartRef.value).setOption(horizontalBarOption(countryDistribution.value.slice(0, 8), 'country', 'totalSales', '#b18cff'))
}
const initInventoryChart = () => {
  if (!inventoryChartRef.value) return
  createChart(inventoryChartRef.value).setOption(horizontalBarOption(inventoryValue.value.slice(0, 8), 'warehouseName', 'valueCny', '#ffd166', false))
}

// 深色饼图公共
const darkPieOption = (data) => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)', ...darkTooltip },
  legend: { bottom: 0, left: 'center', itemWidth: 10, itemHeight: 10, textStyle: { color: AXIS, fontSize: 10 } },
  series: [{
    type: 'pie',
    radius: ['38%', '66%'],
    center: ['50%', '46%'],
    itemStyle: { borderRadius: 4, borderColor: 'rgba(8, 20, 40, 0.9)', borderWidth: 2 },
    label: { show: false },
    emphasis: { label: { show: true, color: TXT, fontSize: 13, fontWeight: 'bold', formatter: '{b}\n{d}%' } },
    data: data.map((d, i) => ({
      value: d.value,
      name: d.name,
      itemStyle: { color: PALETTE[i % PALETTE.length] }
    }))
  }]
})

const initDeptChart = () => {
  if (!deptChartRef.value) return
  createChart(deptChartRef.value).setOption(darkPieOption(employeeStructure.value?.departmentDistribution || []))
}

const initStructureChart = () => {
  if (!structureChartRef.value) return
  const dataMap = {
    age: employeeStructure.value?.ageDistribution || [],
    education: employeeStructure.value?.educationDistribution || [],
    gender: employeeStructure.value?.genderDistribution || []
  }
  createChart(structureChartRef.value).setOption(darkPieOption(dataMap[structureType.value] || []))
}

// 人员规模预测（含当前基准线）
const initAttritionChart = () => {
  if (!attritionChartRef.value) return
  const chart = createChart(attritionChartRef.value)
  const futureData = talentPrediction.value?.futureHeadcount || []
  const months = futureData.map(d => d.month)
  const counts = futureData.map(d => d.predictedCount)
  chart.setOption({
    tooltip: { trigger: 'axis', ...darkTooltip },
    grid: { left: '8%', right: '8%', top: '16%', bottom: '12%', containLabel: true },
    xAxis: darkCategoryAxis(months.length > 0 ? months : ['本月']),
    yAxis: { ...darkValueAxis('预测人数'), nameGap: 12 },
    series: [{
      name: '预测人数',
      type: 'line',
      smooth: true,
      data: counts.length > 0 ? counts : [dashboardStats.value?.totalEmployees || 0],
      itemStyle: { color: '#6ea8ff' },
      lineStyle: { width: 3, color: '#6ea8ff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(110, 168, 255, 0.4)' },
          { offset: 1, color: 'rgba(110, 168, 255, 0.03)' }
        ])
      },
      markLine: {
        symbol: 'none',
        data: [{ yAxis: dashboardStats.value?.totalEmployees || 0, name: '当前', label: { formatter: '当前', color: '#ffa94d' }, lineStyle: { color: '#ffa94d', type: 'dashed' } }]
      }
    }]
  })
}

// 绩效玫瑰图
const initPerformanceChart = () => {
  if (!performanceChartRef.value) return
  createChart(performanceChartRef.value).setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)', ...darkTooltip },
    legend: { bottom: 0, left: 'center', itemWidth: 10, itemHeight: 10, textStyle: { color: AXIS, fontSize: 10 } },
    series: [{
      type: 'pie',
      radius: ['22%', '62%'],
      center: ['50%', '44%'],
      roseType: 'radius',
      itemStyle: { borderRadius: 6, borderColor: 'rgba(8, 20, 40, 0.9)', borderWidth: 2 },
      label: { show: false },
      data: (performanceAnalysis.value?.gradeDistribution || []).map((d, i) => ({
        value: d.value,
        name: d.name,
        itemStyle: { color: ['#3ddc97', '#36d9f0', '#ffd166', '#ff6b81', '#8fb3e6'][i % 5] }
      }))
    }]
  })
}

// 薪资分布
const initSalaryChart = () => {
  if (!salaryChartRef.value) return
  const data = salaryAnalysis.value?.salaryDistribution || []
  createChart(salaryChartRef.value).setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...darkTooltip },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
    xAxis: darkCategoryAxis(data.map(d => d.name)),
    yAxis: { ...darkValueAxis('人数'), nameGap: 12 },
    series: [{
      name: '人数',
      type: 'bar',
      barWidth: '50%',
      data: data.map((d, i) => ({
        value: d.value,
        itemStyle: { color: PALETTE[i % PALETTE.length], borderRadius: [4, 4, 0, 0] }
      })),
      label: { show: true, position: 'top', formatter: '{c}人', color: TXT, fontSize: 10 }
    }]
  })
}

// 各部门出勤率
const initAttendanceChart = () => {
  if (!attendanceChartRef.value) return
  const deptData = attendanceAnalysis.value?.deptAttendance || []
  createChart(attendanceChartRef.value).setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...darkTooltip, valueFormatter: (v) => v + '%' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '14%', containLabel: true },
    xAxis: darkCategoryAxis(deptData.map(d => d.name)),
    yAxis: { ...darkValueAxis('出勤率(%)'), nameGap: 12 },
    series: [{
      name: '出勤率',
      type: 'bar',
      barWidth: '40%',
      data: deptData.map(d => d.attendanceRate),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#3ddc97' },
          { offset: 1, color: 'rgba(61, 220, 151, 0.2)' }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      label: { show: true, position: 'top', formatter: '{c}%', color: TXT, fontSize: 10 }
    }]
  })
}

/* ---------- 交互与生命周期 ---------- */
watch(structureType, () => {
  const idx = charts.findIndex(c => c.getDom() === structureChartRef.value)
  if (idx >= 0) {
    const dataMap = {
      age: employeeStructure.value?.ageDistribution || [],
      education: employeeStructure.value?.educationDistribution || [],
      gender: employeeStructure.value?.genderDistribution || []
    }
    charts[idx].setOption(darkPieOption(dataMap[structureType.value] || []), { notMerge: true })
  }
})

const handleResize = () => charts.forEach(c => c.resize())

onMounted(() => {
  loadAllData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
})
</script>

<style scoped lang="scss">
.cockpit {
  min-height: 100%;
  padding: 16px 18px 24px;
  background:
    radial-gradient(ellipse at 20% 0%, rgba(54, 130, 240, 0.12) 0%, transparent 50%),
    radial-gradient(ellipse at 85% 100%, rgba(84, 217, 240, 0.08) 0%, transparent 45%),
    linear-gradient(165deg, #081428 0%, #0c2038 55%, #081a30 100%);
}

/* 顶部标题 */
.cockpit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 4px 14px;
  border-bottom: 1px solid rgba(110, 165, 255, 0.15);
  margin-bottom: 14px;

  .header-left {
    display: flex;
    align-items: baseline;
    gap: 10px;
  }
  .cockpit-title {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 2px;
    color: #eaf4ff;
    text-shadow: 0 0 18px rgba(54, 217, 240, 0.35);
  }
  .cockpit-subtitle {
    font-size: 11px;
    letter-spacing: 4px;
    color: rgba(54, 217, 240, 0.55);
  }
  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .update-time {
    font-size: 12px;
    color: #7fa8d9;
  }
  .refresh-btn {
    background: rgba(110, 165, 255, 0.12);
    border: 1px solid rgba(110, 165, 255, 0.3);
    color: #bcd6ff;
  }
}

/* KPI 总览条 */
.kpi-strip {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 12px;
  margin-bottom: 18px;

  @media (max-width: 1400px) {
    grid-template-columns: repeat(4, 1fr);
  }
  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.kpi-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 14px;
  background: linear-gradient(150deg, rgba(255, 255, 255, 0.06) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(110, 165, 255, 0.14);
  border-top: 2px solid var(--accent);
  border-radius: 10px;
  transition: transform 0.25s, box-shadow 0.25s;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35), 0 0 12px color-mix(in srgb, var(--accent) 30%, transparent);
  }

  .kpi-icon {
    width: 40px;
    height: 40px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 9px;
    color: var(--accent);
    background: color-mix(in srgb, var(--accent) 14%, transparent);
  }
  .kpi-value {
    font-size: 21px;
    font-weight: 700;
    line-height: 1.15;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }
  .kpi-label {
    margin-top: 2px;
    font-size: 12px;
    color: #7fa8d9;
  }
}

/* 区块标题 */
.section-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 20px 2px 12px;

  .section-title {
    display: flex;
    align-items: center;
    gap: 12px;

    h2 {
      margin: 0;
      font-size: 17px;
      font-weight: 600;
      color: #eaf4ff;
      letter-spacing: 1px;
    }
    p {
      margin: 1px 0 0;
      font-size: 11px;
      color: #7fa8d9;
      letter-spacing: 2px;
    }
  }
  .section-badge {
    width: 34px;
    height: 34px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    font-weight: 700;
    font-size: 15px;

    &.badge-biz {
      color: #36d9f0;
      background: rgba(54, 217, 240, 0.12);
      border: 1px solid rgba(54, 217, 240, 0.4);
    }
    &.badge-hr {
      color: #3ddc97;
      background: rgba(61, 220, 151, 0.12);
      border: 1px solid rgba(61, 220, 151, 0.4);
    }
  }
  .section-line {
    flex: 1;
    height: 1px;
    background: linear-gradient(90deg, rgba(110, 165, 255, 0.35), transparent);
  }
}

/* 图表面板 */
.chart-row {
  margin-bottom: 14px;
}

.panel {
  background: rgba(255, 255, 255, 0.045);
  border: 1px solid rgba(110, 165, 255, 0.14);
  border-radius: 10px;
  padding: 12px 14px;
  height: 100%;
  transition: border-color 0.25s, box-shadow 0.25s;

  &:hover {
    border-color: rgba(110, 165, 255, 0.35);
    box-shadow: 0 6px 22px rgba(0, 0, 0, 0.3);
  }
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
  color: #dce9ff;
  margin-bottom: 6px;
  position: relative;
  padding-left: 10px;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 3px;
    height: 14px;
    border-radius: 2px;
    background: linear-gradient(180deg, #36d9f0, #6ea8ff);
  }
}

.chart {
  width: 100%;
  height: 250px;

  &.chart-high {
    height: 330px;
  }
}

/* 资金概况 */
.fund-panel {
  display: flex;
  flex-direction: column;
}

.fund-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-content: center;
}

.fund-item {
  padding: 16px 14px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(110, 165, 255, 0.1);

  .fund-label {
    font-size: 12px;
    color: #7fa8d9;
    margin-bottom: 6px;
  }
  .fund-value {
    font-size: 20px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
  }
}

.fund-tip {
  margin-top: 10px;
  font-size: 11px;
  color: #5d7ea8;
  text-align: center;
}

/* 深色单选按钮 */
.dark-radio {
  --el-color-primary: #36d9f0;
}
.dark-radio :deep(.el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(110, 165, 255, 0.25);
  color: #9fc1ec;
}
.dark-radio :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: rgba(54, 217, 240, 0.2);
  border-color: rgba(54, 217, 240, 0.5);
  color: #fff;
}

/* 深色表格 */
.table-dark {
  --el-table-border-color: rgba(110, 165, 255, 0.12);
  --el-table-header-bg-color: rgba(110, 165, 255, 0.1);
  background: transparent;

  :deep(.el-table__inner-wrapper) {
    background: transparent;
  }
  :deep(th.el-table__cell) {
    background: rgba(110, 165, 255, 0.1) !important;
    color: #9fc1ec;
  }
  :deep(tr) {
    background: transparent !important;
    color: #c9dcff;
  }
  :deep(td.el-table__cell) {
    background: transparent !important;
    border-color: rgba(110, 165, 255, 0.08);
  }
  :deep(.el-table__body tr:hover > td.el-table__cell) {
    background: rgba(110, 165, 255, 0.08) !important;
  }
}

.hiring-table {
  padding-top: 4px;
}

/* AI 洞察精选 */
.insight-panel {
  margin-top: 2px;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.insight-item {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(110, 165, 255, 0.1);

  &.warning .insight-icon { color: #ffd166; background: rgba(255, 209, 102, 0.12); }
  &.success .insight-icon { color: #3ddc97; background: rgba(61, 220, 151, 0.12); }
  &.info .insight-icon { color: #36d9f0; background: rgba(54, 217, 240, 0.12); }
  &.primary .insight-icon { color: #b18cff; background: rgba(177, 140, 255, 0.12); }

  .insight-icon {
    width: 34px;
    height: 34px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
  }
  .insight-title {
    font-size: 13px;
    font-weight: 600;
    color: #dce9ff;
    margin-bottom: 3px;
  }
  .insight-desc {
    font-size: 12px;
    color: #8fb3e6;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}
</style>
