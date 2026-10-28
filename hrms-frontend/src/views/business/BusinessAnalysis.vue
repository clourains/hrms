<template>
  <div class="analysis-container">
    <div class="analysis-header">
      <div class="header-left">
        <h2 class="title">营业分析大屏</h2>
        <p class="subtitle">Business Intelligence Dashboard - 境外贸易业务</p>
      </div>
      <div class="header-right">
        <el-tag type="primary" effect="dark" size="large">
          <el-icon><DataAnalysis /></el-icon>
          <span style="margin-left: 6px">实时数据</span>
        </el-tag>
        <span class="update-time">数据更新时间：{{ updateTime }}</span>
        <el-button :icon="Refresh" circle size="small" @click="loadAllData" :loading="loading" />
      </div>
    </div>

    <div v-if="loading && !overview" class="loading-area">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <!-- 核心指标 -->
      <el-row :gutter="16" class="core-metrics">
        <el-col :xs="24" :sm="12" :md="6" v-for="metric in coreMetrics" :key="metric.title">
          <div class="metric-card" :style="{ borderTopColor: metric.color }">
            <div class="metric-header">
              <el-icon :size="20" :style="{ color: metric.color }"><component :is="metric.icon" /></el-icon>
              <span class="metric-title">{{ metric.title }}</span>
            </div>
            <div class="metric-value" :style="{ color: metric.color }">{{ metric.value }}</div>
            <div class="metric-footer">{{ metric.footer }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 图表行 1：月度销售 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :span="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">近12个月销售趋势</span>
              <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('monthly')" />
            </div>
            <div ref="monthlyChartRef" class="chart-content chart-large"></div>
          </div>
        </el-col>
      </el-row>

      <!-- 图表行 2 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :lg="8" :md="12" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">客户销售额 TOP10</span>
              <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('customer')" />
            </div>
            <div ref="customerChartRef" class="chart-content"></div>
          </div>
        </el-col>
        <el-col :lg="8" :md="12" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">产品销售额 TOP10</span>
              <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('product')" />
            </div>
            <div ref="productChartRef" class="chart-content"></div>
          </div>
        </el-col>
        <el-col :lg="8" :md="24" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">销售国家分布</span>
              <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('country')" />
            </div>
            <div ref="countryChartRef" class="chart-content"></div>
          </div>
        </el-col>
      </el-row>

      <!-- 图表行 3 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :lg="12" :md="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">各仓库库存价值 (CNY)</span>
              <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('inventory')" />
            </div>
            <div ref="inventoryChartRef" class="chart-content"></div>
          </div>
        </el-col>
        <el-col :lg="12" :md="24">
          <div class="chart-panel">
            <div class="panel-header"><span class="panel-title">资金概况</span></div>
            <div class="fund-panel">
              <div class="fund-item">
                <div class="fund-label">累计收款 (USD)</div>
                <div class="fund-value" style="color: #67C23A">${{ formatNum(overview?.totalReceivedUsd) }}</div>
              </div>
              <div class="fund-item">
                <div class="fund-label">累计付款 (USD)</div>
                <div class="fund-value" style="color: #F56C6C">${{ formatNum(overview?.totalPaidUsd) }}</div>
              </div>
              <div class="fund-item">
                <div class="fund-label">待收账款 (USD)</div>
                <div class="fund-value" style="color: #E6A23C">${{ formatNum(overview?.pendingReceivableUsd) }}</div>
              </div>
              <div class="fund-item">
                <div class="fund-label">库存价值 (CNY)</div>
                <div class="fund-value" style="color: #409EFF">¥{{ formatNum(overview?.inventoryValueCny) }}</div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
      <!-- 图表行 4：毛利排行（按客户 / 按产品） -->
      <el-row :gutter="16" class="chart-row">
        <el-col :lg="12" :md="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">客户毛利 TOP10</span>
              <span class="panel-tip">毛利 = 售价 − 成本(按订单汇率折算)，未计运费</span>
            </div>
            <el-table :data="profitRanking.byCustomer" size="small" max-height="330" class="profit-table">
              <el-table-column type="index" label="#" width="45" align="center" />
              <el-table-column prop="name" label="客户" min-width="150" show-overflow-tooltip />
              <el-table-column prop="country" label="国家" width="95" />
              <el-table-column label="毛利(USD)" width="120" align="right">
                <template #default="{ row }">
                  <span :class="row.profitUsd >= 0 ? 'profit-up' : 'profit-down'">{{ formatNum(row.profitUsd) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="毛利率" width="95" align="right">
                <template #default="{ row }">{{ formatRate(row.marginRate) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
        <el-col :lg="12" :md="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">产品毛利 TOP10</span>
              <span class="panel-tip">用于识别真正赚钱的品类</span>
            </div>
            <el-table :data="profitRanking.byProduct" size="small" max-height="330" class="profit-table">
              <el-table-column type="index" label="#" width="45" align="center" />
              <el-table-column prop="name" label="产品" min-width="150" show-overflow-tooltip />
              <el-table-column prop="sku" label="SKU" width="115" />
              <el-table-column label="毛利(USD)" width="120" align="right">
                <template #default="{ row }">
                  <span :class="row.profitUsd >= 0 ? 'profit-up' : 'profit-down'">{{ formatNum(row.profitUsd) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="毛利率" width="95" align="right">
                <template #default="{ row }">{{ formatRate(row.marginRate) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>

      <!-- 单图全屏查看 -->
      <el-dialog
        v-model="zoomVisible"
        :title="zoomTitle"
        fullscreen
        destroy-on-close
        @closed="closeChartZoom"
      >
        <div ref="zoomChartRef" class="fullscreen-chart"></div>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { useAppStore } from '@/store/app'
import { DataAnalysis, Refresh, Money, TrendCharts, Tickets, User, Wallet, Box, Coin, ShoppingCart, FullScreen } from '@element-plus/icons-vue'
import {
  getBusinessOverviewApi, getMonthlySalesApi, getTopCustomersApi,
  getTopProductsApi, getCountryDistributionApi, getInventoryValueApi, getProfitRankingApi
} from '@/api/trade'

const loading = ref(false)
const updateTime = ref(new Date().toLocaleString('zh-CN'))

const overview = ref(null)
const monthlySales = ref({ months: [], sales: [], orderCounts: [] })
const topCustomers = ref([])
const topProducts = ref([])
const countryDistribution = ref([])
const inventoryValue = ref([])
const profitRanking = ref({ byCustomer: [], byProduct: [] })

const monthlyChartRef = ref(null)
const customerChartRef = ref(null)
const productChartRef = ref(null)
const countryChartRef = ref(null)
const inventoryChartRef = ref(null)

let monthlyChart = null
let customerChart = null
let productChart = null
let countryChart = null
let inventoryChart = null

// 单图全屏放大：点击卡片右上角放大按钮后，用源图表当前 option 在全屏弹窗中重建该图
const zoomVisible = ref(false)
const zoomTitle = ref('')
const zoomChartRef = ref(null)
let zoomChart = null

const chartRegistry = {
  monthly: { title: '近12个月销售趋势', get: () => monthlyChart },
  customer: { title: '客户销售额 TOP10', get: () => customerChart },
  product: { title: '产品销售额 TOP10', get: () => productChart },
  country: { title: '销售国家分布', get: () => countryChart },
  inventory: { title: '各仓库库存价值 (CNY)', get: () => inventoryChart }
}

const openChartZoom = async (key) => {
  const item = chartRegistry[key]
  const source = item && item.get()
  if (!source) return
  zoomTitle.value = item.title
  zoomVisible.value = true
  await nextTick()
  zoomChart?.dispose()
  zoomChart = echarts.init(zoomChartRef.value, chartThemeName())
  zoomChart.setOption(source.getOption())
  zoomChart.resize()
}

const closeChartZoom = () => {
  zoomChart?.dispose()
  zoomChart = null
}

const coreMetrics = ref([
  { title: '总销售额', value: '--', icon: 'Money', color: '#409EFF', footer: '全部订单累计 (USD)' },
  { title: '本年销售额', value: '--', icon: 'TrendCharts', color: '#67C23A', footer: '本年度订单累计 (USD)' },
  { title: '毛利润', value: '--', icon: 'Coin', color: '#E6A23C', footer: '销售额 - 产品成本 (USD)' },
  { title: '订单总数', value: '--', icon: 'Tickets', color: '#9b59b6', footer: '全部销售订单' },
  { title: '客户总数', value: '--', icon: 'User', color: '#1abc9c', footer: '境外客户数量' },
  { title: '待收账款', value: '--', icon: 'Wallet', color: '#F56C6C', footer: '应收未收金额 (USD)' },
  { title: '库存价值', value: '--', icon: 'Box', color: '#e67e22', footer: '全仓库存货值 (CNY)' },
  { title: '累计收款', value: '--', icon: 'ShoppingCart', color: '#16a085', footer: '客户回款累计 (USD)' }
])

const appStore = useAppStore()

// 暗黑模式：图表以 ECharts 内置 dark 主题重建，显式适配文字颜色
const chartThemeName = () => (appStore.isDark ? 'dark' : undefined)
const labelText = () => (appStore.isDark ? '#E5EAF3' : '#303133')

const formatNum = (n) => (n == null ? '0.00' : Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }))
const formatInt = (n) => (n == null ? '0' : Number(n).toLocaleString('en-US'))
// 毛利率：后端已按百分比返回（45.67 = 45.67%）
const formatRate = (n) => (n == null ? '—' : Number(n).toFixed(2) + '%')

const loadAllData = async () => {
  loading.value = true
  updateTime.value = new Date().toLocaleString('zh-CN')
  try {
    const [ov, monthly, customers, products, countries, inventory, profit] = await Promise.all([
      getBusinessOverviewApi(),
      getMonthlySalesApi(),
      getTopCustomersApi(),
      getTopProductsApi(),
      getCountryDistributionApi(),
      getInventoryValueApi(),
      getProfitRankingApi()
    ])

    overview.value = ov.data
    monthlySales.value = monthly.data || { months: [], sales: [], orderCounts: [] }
    topCustomers.value = customers.data || []
    topProducts.value = products.data || []
    countryDistribution.value = countries.data || []
    inventoryValue.value = inventory.data || []
    profitRanking.value = profit.data || { byCustomer: [], byProduct: [] }

    updateCoreMetrics()

    await nextTick()
    initAllCharts()
  } catch (e) {
    console.error('Load business analysis data error:', e)
  } finally {
    loading.value = false
  }
}

const updateCoreMetrics = () => {
  if (!overview.value) return
  const s = overview.value
  coreMetrics.value[0].value = '$' + formatNum(s.totalSalesUsd)
  coreMetrics.value[1].value = '$' + formatNum(s.yearSalesUsd)
  coreMetrics.value[2].value = '$' + formatNum(s.grossProfitUsd)
  coreMetrics.value[2].footer = '毛利率 ' + formatRate(s.grossMarginRate) + ' · 销售额 - 产品成本 (USD)'
  coreMetrics.value[3].value = formatInt(s.totalOrders) + ' 单'
  coreMetrics.value[4].value = formatInt(s.totalCustomers) + ' 家'
  coreMetrics.value[5].value = '$' + formatNum(s.pendingReceivableUsd)
  coreMetrics.value[6].value = '¥' + formatNum(s.inventoryValueCny)
  coreMetrics.value[7].value = '$' + formatNum(s.totalReceivedUsd)
}

const initAllCharts = () => {
  initMonthlyChart()
  initCustomerChart()
  initProductChart()
  initCountryChart()
  initInventoryChart()
}

const initMonthlyChart = () => {
  if (!monthlyChartRef.value) return
  monthlyChart?.dispose()
  monthlyChart = echarts.init(monthlyChartRef.value, chartThemeName())
  const { months, sales, orderCounts } = monthlySales.value
  monthlyChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额(USD)', '订单量'], top: 0, textStyle: { color: labelText() } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: months },
    yAxis: [
      { type: 'value', name: '销售额(USD)', axisLabel: { formatter: (v) => (v >= 10000 ? (v / 10000) + '万' : v) } },
      { type: 'value', name: '订单量' }
    ],
    series: [
      {
        name: '销售额(USD)',
        type: 'bar',
        barWidth: '45%',
        data: sales,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#66b1ff' }
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
        itemStyle: { color: '#E6A23C' },
        lineStyle: { width: 3 }
      }
    ]
  })
}

const horizontalBarOption = (data, nameKey, valueKey, color) => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: (ps) => `${ps[0].name}: $${formatNum(ps[0].value)}` },
  grid: { left: '3%', right: '8%', bottom: '3%', top: '5%', containLabel: true },
  xAxis: { type: 'value', axisLabel: { formatter: (v) => (v >= 10000 ? (v / 10000) + '万' : v), fontSize: 10 } },
  yAxis: { type: 'category', data: data.map(d => d[nameKey]).reverse(), axisLabel: { fontSize: 11, width: 90, overflow: 'truncate' } },
  series: [{
    type: 'bar',
    data: data.map(d => d[valueKey]).reverse(),
    barWidth: '55%',
    itemStyle: { color, borderRadius: [0, 4, 4, 0] },
    label: { show: true, position: 'right', formatter: (p) => '$' + formatNum(p.value), fontSize: 10, color: labelText() }
  }]
})

const initCustomerChart = () => {
  if (!customerChartRef.value) return
  customerChart?.dispose()
  customerChart = echarts.init(customerChartRef.value, chartThemeName())
  customerChart.setOption(horizontalBarOption(topCustomers.value.slice(0, 10), 'customerName', 'totalSales', '#67C23A'))
}

const initProductChart = () => {
  if (!productChartRef.value) return
  productChart?.dispose()
  productChart = echarts.init(productChartRef.value, chartThemeName())
  productChart.setOption(horizontalBarOption(topProducts.value.slice(0, 10), 'productName', 'totalSales', '#409EFF'))
}

const initCountryChart = () => {
  if (!countryChartRef.value) return
  countryChart?.dispose()
  countryChart = echarts.init(countryChartRef.value, chartThemeName())
  const data = countryDistribution.value
  countryChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: (p) => `${p.name}: $${formatNum(p.value)} (${p.percent}%)` },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '52%'],
      itemStyle: { borderRadius: 4, borderColor: appStore.isDark ? '#141414' : '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 11, color: labelText() },
      data: data.map((d, i) => ({
        value: d.totalSales,
        name: d.country || '未知',
        itemStyle: { color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9b59b6', '#1abc9c', '#e67e22', '#909399'][i % 8] }
      }))
    }]
  })
}

const initInventoryChart = () => {
  if (!inventoryChartRef.value) return
  inventoryChart?.dispose()
  inventoryChart = echarts.init(inventoryChartRef.value, chartThemeName())
  const data = inventoryValue.value
  inventoryChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: (ps) => `${ps[0].name}: ¥${formatNum(ps[0].value)}` },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.warehouseName), axisLabel: { fontSize: 10, interval: 0, rotate: data.length > 5 ? 25 : 0 } },
    yAxis: { type: 'value', name: '库存价值(CNY)', axisLabel: { formatter: (v) => (v >= 10000 ? (v / 10000) + '万' : v) } },
    series: [{
      type: 'bar',
      barWidth: '45%',
      data: data.map(d => ({
        value: d.totalValue,
        itemStyle: {
          color: d.warehouseType === 2 ? '#E6A23C' : '#409EFF',
          borderRadius: [4, 4, 0, 0]
        }
      })),
      label: { show: true, position: 'top', formatter: (p) => '¥' + formatNum(p.value), fontSize: 10, color: labelText() }
    }]
  })
}

// 暗黑模式切换后重建全部图表
watch(() => appStore.isDark, () => {
  nextTick(() => initAllCharts())
})

const handleResize = () => {
  monthlyChart?.resize()
  customerChart?.resize()
  productChart?.resize()
  countryChart?.resize()
  inventoryChart?.resize()
}

onMounted(() => {
  loadAllData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  monthlyChart?.dispose()
  customerChart?.dispose()
  productChart?.dispose()
  countryChart?.dispose()
  inventoryChart?.dispose()
})
</script>

<style scoped lang="scss">
.analysis-container {
  min-height: 100%;
  background: #f0f2f5;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  color: #fff;
  padding: 20px 24px;
  border-radius: 8px;
  margin-bottom: 16px;

  .title { font-size: 24px; font-weight: 600; margin: 0 0 6px 0; }
  .subtitle { font-size: 13px; opacity: 0.85; margin: 0; }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
    .update-time { font-size: 13px; opacity: 0.9; }
  }
}

.loading-area { padding: 40px; background: #fff; border-radius: 8px; }

.core-metrics { margin-bottom: 16px; }

.metric-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border-top: 3px solid #409EFF;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: transform 0.3s;
  margin-bottom: 16px;
  &:hover { transform: translateY(-2px); }

  .metric-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; .metric-title { font-size: 14px; color: #606266; } }
  .metric-value { font-size: 24px; font-weight: 600; margin-bottom: 8px; }
  .metric-footer { font-size: 12px; color: #909399; }
}

.chart-row { margin-bottom: 16px; }

.chart-panel { background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); height: 100%; }

.panel-header {
  display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid #ebeef5;
  .panel-title { font-size: 15px; font-weight: 600; color: #303133; position: relative; padding-left: 10px;
    &::before { content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 4px; height: 16px; background: #e67e22; border-radius: 2px; }
  }
}

.chart-content { width: 100%; height: 280px; padding: 10px; &.chart-large { height: 320px; } }

.zoom-btn {
  font-size: 16px;
  color: #909399;
  &:hover { color: #409EFF; }
}

.fullscreen-chart { width: 100%; height: calc(100vh - 140px); }

// 毛利排行：右侧小字说明口径，表格紧凑一点
.panel-tip { font-size: 12px; color: #909399; }
.profit-table { padding: 4px 8px 8px; }
.profit-up { color: #F56C6C; font-weight: 600; }
.profit-down { color: #67C23A; font-weight: 600; }

.fund-panel {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 20px 16px;
  height: 280px;
  align-content: center;
}

.fund-item {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  text-align: center;

  .fund-label { font-size: 13px; color: #909399; margin-bottom: 10px; }
  .fund-value { font-size: 22px; font-weight: 600; }
}
</style>
