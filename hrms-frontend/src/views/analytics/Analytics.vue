<template>
  <div class="analytics-container">
    <div class="analytics-header">
      <div class="header-left">
        <h2 class="title">人力资源智能分析大屏</h2>
        <p class="subtitle">HR Intelligence Analytics Dashboard</p>
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

    <div v-if="loading && !dashboardStats" class="loading-area">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <!-- Core metrics -->
      <el-row :gutter="16" class="core-metrics">
        <el-col :xs="24" :sm="12" :md="6" v-for="metric in coreMetrics" :key="metric.title">
          <div class="metric-card" :style="{ borderTopColor: metric.color }">
            <div class="metric-header">
              <el-icon :size="20" :style="{ color: metric.color }"><component :is="metric.icon" /></el-icon>
              <span class="metric-title">{{ metric.title }}</span>
            </div>
            <div class="metric-value" :style="{ color: metric.color }">{{ metric.value }}</div>
            <div class="metric-footer">
              <span class="metric-trend" :class="metric.trend > 0 ? 'up' : 'down'">
                <el-icon><component :is="metric.trend > 0 ? 'CaretTop' : 'CaretBottom'" /></el-icon>
                {{ Math.abs(metric.trend) }}%
              </span>
              <span class="metric-compare">较上月</span>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- Chart row 1 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :lg="8" :md="12" :xs="24">
          <div class="chart-panel">
            <div class="panel-header"><span class="panel-title">部门人员分布</span></div>
            <div ref="deptChartRef" class="chart-content"></div>
          </div>
        </el-col>
        <el-col :lg="8" :md="12" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">员工结构分析</span>
              <el-radio-group v-model="structureType" size="small">
                <el-radio-button label="age">年龄</el-radio-button>
                <el-radio-button label="education">学历</el-radio-button>
                <el-radio-button label="gender">性别</el-radio-button>
              </el-radio-group>
            </div>
            <div ref="structureChartRef" class="chart-content"></div>
          </div>
        </el-col>
        <el-col :lg="8" :md="12" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">人员流失率</span>
              <el-tag type="warning" size="small">AI预测</el-tag>
            </div>
            <div ref="attritionChartRef" class="chart-content"></div>
          </div>
        </el-col>
      </el-row>

      <!-- Chart row 2 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :lg="12" :md="24">
          <div class="chart-panel">
            <div class="panel-header"><span class="panel-title">薪资分布分析</span></div>
            <div ref="salaryChartRef" class="chart-content chart-large"></div>
          </div>
        </el-col>
        <el-col :lg="12" :md="24">
          <div class="chart-panel">
            <div class="panel-header"><span class="panel-title">绩效等级分布</span></div>
            <div ref="performanceChartRef" class="chart-content chart-large"></div>
          </div>
        </el-col>
      </el-row>

      <!-- Chart row 3 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :lg="8" :md="12" :xs="24">
          <div class="chart-panel">
            <div class="panel-header"><span class="panel-title">考勤统计分析</span></div>
            <div ref="attendanceChartRef" class="chart-content"></div>
          </div>
        </el-col>
        <el-col :lg="8" :md="12" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">人才画像雷达</span>
              <el-tag type="success" size="small">AI分析</el-tag>
            </div>
            <div ref="radarChartRef" class="chart-content"></div>
          </div>
        </el-col>
        <el-col :lg="8" :md="24" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">招聘需求预测</span>
              <el-tag type="warning" size="small">AI预测</el-tag>
            </div>
            <div class="hiring-prediction">
              <el-table :data="hiringData" size="small" border>
                <el-table-column prop="position" label="部门" width="100" />
                <el-table-column prop="currentCount" label="现有人数" width="80" align="center" />
                <el-table-column prop="predictedDemand" label="需求人数" width="80" align="center" />
                <el-table-column prop="priority" label="紧急度" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.priority === '高' ? 'danger' : row.priority === '中' ? 'warning' : 'info'" size="small">
                      {{ row.priority }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 入职离职趋势 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :span="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">人员入职离职趋势</span>
              <el-radio-group v-model="trendPeriod" size="small">
                <el-radio-button label="year">近一年</el-radio-button>
                <el-radio-button label="halfYear">近半年</el-radio-button>
                <el-radio-button label="quarter">近季度</el-radio-button>
              </el-radio-group>
            </div>
            <div ref="trendChartRef" class="chart-content chart-large"></div>
          </div>
        </el-col>
      </el-row>

      <!-- AI Insights -->
      <el-row :gutter="16" class="chart-row">
        <el-col :span="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">AI智能洞察</span>
              <el-tag type="primary" size="small">基于真实数据分析</el-tag>
            </div>
            <div class="ai-insights">
              <div v-for="(insight, index) in aiInsights" :key="index" class="insight-item">
                <div class="insight-icon" :class="insight.type">
                  <el-icon><component :is="getInsightIcon(insight.type)" /></el-icon>
                </div>
                <div class="insight-content">
                  <div class="insight-title">{{ insight.title }}</div>
                  <div class="insight-desc">{{ insight.description }}</div>
                  <div class="insight-suggestion" v-if="insight.suggestion">
                    <el-icon><Lightning /></el-icon>
                    <span>{{ insight.suggestion }}</span>
                  </div>
                  <div class="insight-confidence" v-if="insight.confidence">
                    置信度：{{ (insight.confidence * 100).toFixed(0) }}%
                  </div>
                </div>
              </div>
              <div v-if="aiInsights.length === 0" class="no-insights">
                <el-empty description="暂无AI洞察数据" />
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { DataAnalysis, CaretTop, CaretBottom, User, UserFilled, Warning, TrendCharts, Money, Lightning, InfoFilled, Refresh } from '@element-plus/icons-vue'
import { getDashboardStatsApi, getEmployeeStructureApi, getAttendanceAnalysisApi, getSalaryAnalysisApi, getPerformanceAnalysisApi, getTalentPredictionApi, getAiInsightsApi, getTurnoverTrendApi } from '@/api/analytics'

const loading = ref(false)
const updateTime = ref(new Date().toLocaleString('zh-CN'))
const structureType = ref('age')

const dashboardStats = ref(null)
const employeeStructure = ref(null)
const attendanceAnalysis = ref(null)
const salaryAnalysis = ref(null)
const performanceAnalysis = ref(null)
const talentPrediction = ref(null)
const aiInsights = ref([])

const deptChartRef = ref(null)
const structureChartRef = ref(null)
const attritionChartRef = ref(null)
const salaryChartRef = ref(null)
const performanceChartRef = ref(null)
const attendanceChartRef = ref(null)
const radarChartRef = ref(null)
const trendChartRef = ref(null)

let deptChart = null
let structureChart = null
let attritionChart = null
let salaryChart = null
let performanceChart = null
let attendanceChart = null
let radarChart = null
let trendChart = null

// 入职离职趋势数据（M1-5：真实统计接口，按入职/离职日期按月聚合）
const TREND_PERIODS = { year: 12, halfYear: 6, quarter: 3 }
const trendPeriod = ref('year')
const trendData = ref([])

const loadTrendData = async (months) => {
  const res = await getTurnoverTrendApi(months)
  trendData.value = res.data || []
}

const coreMetrics = ref([
  { title: '员工总数', value: '--', trend: 0, icon: 'User', color: '#409EFF' },
  { title: '在职员工', value: '--', trend: 0, icon: 'UserFilled', color: '#67C23A' },
  { title: '流失率', value: '--', trend: 0, icon: 'Warning', color: '#E6A23C' },
  { title: '人均薪资', value: '--', trend: 0, icon: 'Money', color: '#F56C6C' }
])

const hiringData = ref([])

const getInsightIcon = (type) => {
  const map = { warning: 'Warning', success: 'TrendCharts', info: 'Lightning', primary: 'InfoFilled' }
  return map[type] || 'InfoFilled'
}

const loadAllData = async () => {
  loading.value = true
  updateTime.value = new Date().toLocaleString('zh-CN')
  try {
    const [stats, structure, attendance, salary, performance, prediction, insights, trend] = await Promise.all([
      getDashboardStatsApi(),
      getEmployeeStructureApi(),
      getAttendanceAnalysisApi(),
      getSalaryAnalysisApi(),
      getPerformanceAnalysisApi(),
      getTalentPredictionApi(),
      getAiInsightsApi(),
      getTurnoverTrendApi(TREND_PERIODS[trendPeriod.value] || 12)
    ])

    dashboardStats.value = stats.data
    employeeStructure.value = structure.data
    attendanceAnalysis.value = attendance.data
    salaryAnalysis.value = salary.data
    performanceAnalysis.value = performance.data
    talentPrediction.value = prediction.data
    aiInsights.value = insights.data || []
    trendData.value = trend.data || []

    updateCoreMetrics()
    hiringData.value = prediction.data?.recruitmentDemand || []

    await nextTick()
    initAllCharts()
  } catch (e) {
    console.error('Load analytics data error:', e)
  } finally {
    loading.value = false
  }
}

const updateCoreMetrics = () => {
  if (!dashboardStats.value) return
  const s = dashboardStats.value
  coreMetrics.value[0].value = (s.totalEmployees || 0) + '人'
  coreMetrics.value[1].value = (s.activeEmployees || 0) + '人'
  coreMetrics.value[2].value = (talentPrediction.value?.predictedAttritionRate || 0) + '%'
  coreMetrics.value[3].value = s.avgSalary ? '¥' + Number(s.avgSalary).toLocaleString() : '--'
}

const initAllCharts = () => {
  initDeptChart()
  initStructureChart()
  initAttritionChart()
  initSalaryChart()
  initPerformanceChart()
  initAttendanceChart()
  initRadarChart()
  initTrendChart()
}

const initDeptChart = () => {
  if (!deptChartRef.value) return
  deptChart = echarts.init(deptChartRef.value)
  const data = employeeStructure.value?.departmentDistribution || []
  deptChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '55%'],
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
      labelLine: { length: 8, length2: 6 },
      data: data.map((d, i) => ({
        value: d.value,
        name: d.name,
        itemStyle: { color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9b59b6', '#1abc9c', '#e67e22'][i % 8] }
      }))
    }]
  })
}

const initStructureChart = () => {
  if (!structureChartRef.value) return
  structureChart = echarts.init(structureChartRef.value)
  const dataMap = {
    age: employeeStructure.value?.ageDistribution || [],
    education: employeeStructure.value?.educationDistribution || [],
    gender: employeeStructure.value?.genderDistribution || []
  }
  structureChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { bottom: '5%', left: 'center', itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['50%', '45%'],
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: dataMap[structureType.value].map((d, i) => ({
        value: d.value,
        name: d.name,
        itemStyle: { color: ['#67C23A', '#409EFF', '#E6A23C', '#F56C6C', '#909399', '#9b59b6'][i % 6] }
      }))
    }]
  })
}

const initAttritionChart = () => {
  if (!attritionChartRef.value) return
  attritionChart = echarts.init(attritionChartRef.value)
  const futureData = talentPrediction.value?.futureHeadcount || []
  const attritionRate = talentPrediction.value?.predictedAttritionRate || 0
  const months = futureData.map(d => d.month)
  const counts = futureData.map(d => d.predictedCount)
  attritionChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '10%', right: '10%', top: '15%', bottom: '15%' },
    xAxis: { type: 'category', data: months.length > 0 ? months : ['本月'], axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', name: '预测人数', nameTextStyle: { fontSize: 10 }, axisLabel: { fontSize: 10 } },
    series: [{
      name: '预测人数',
      type: 'line',
      smooth: true,
      data: counts.length > 0 ? counts : [dashboardStats.value?.totalEmployees || 0],
      itemStyle: { color: '#409EFF' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
        { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
      ]) },
      markLine: {
        data: [{ yAxis: dashboardStats.value?.totalEmployees || 0, name: '当前', label: { formatter: '当前' }, lineStyle: { color: '#E6A23C', type: 'dashed' } }]
      }
    }]
  })
}

const initSalaryChart = () => {
  if (!salaryChartRef.value) return
  salaryChart = echarts.init(salaryChartRef.value)
  const data = salaryAnalysis.value?.salaryDistribution || []
  salaryChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.name) },
    yAxis: { type: 'value', name: '人数' },
    series: [{
      name: '人数',
      type: 'bar',
      barWidth: '50%',
      data: data.map((d, i) => ({
        value: d.value,
        itemStyle: {
          color: ['#909399', '#67C23A', '#409EFF', '#E6A23C', '#F56C6C', '#9b59b6'][i % 6],
          borderRadius: [4, 4, 0, 0]
        }
      })),
      label: { show: true, position: 'top', formatter: '{c}人' }
    }]
  })
}

const initPerformanceChart = () => {
  if (!performanceChartRef.value) return
  performanceChart = echarts.init(performanceChartRef.value)
  const data = performanceAnalysis.value?.gradeDistribution || []
  performanceChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['50%', '55%'],
      roseType: 'radius',
      itemStyle: { borderRadius: 6 },
      label: { formatter: '{b}\n{d}%', fontSize: 11 },
      data: data.map((d, i) => ({
        value: d.value,
        name: d.name,
        itemStyle: { color: ['#67C23A', '#409EFF', '#E6A23C', '#F56C6C', '#909399'][i % 5] }
      }))
    }]
  })
}

const initAttendanceChart = () => {
  if (!attendanceChartRef.value) return
  attendanceChart = echarts.init(attendanceChartRef.value)
  const deptData = attendanceAnalysis.value?.deptAttendance || []
  attendanceChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: deptData.map(d => d.name), axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', name: '出勤率(%)', nameTextStyle: { fontSize: 10 } },
    series: [{
      name: '出勤率',
      type: 'bar',
      data: deptData.map(d => d.attendanceRate),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#409EFF' },
          { offset: 1, color: '#66b1ff' }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      barWidth: '40%',
      label: { show: true, position: 'top', formatter: '{c}%', fontSize: 10 }
    }]
  })
}

const initRadarChart = () => {
  if (!radarChartRef.value) return
  radarChart = echarts.init(radarChartRef.value)
  const dimensions = performanceAnalysis.value?.dimensionAnalysis || []
  radarChart.setOption({
    tooltip: {},
    radar: {
      indicator: dimensions.map(d => ({ name: d.name, max: 100 })),
      center: ['50%', '55%'],
      radius: '65%',
      axisName: { fontSize: 10, color: '#606266' },
      splitArea: { areaStyle: { color: ['rgba(64, 158, 255, 0.05)', 'rgba(64, 158, 255, 0.1)'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: dimensions.map(d => d.avg || 0),
        name: '整体水平',
        areaStyle: { color: 'rgba(64, 158, 255, 0.3)' },
        lineStyle: { color: '#409EFF' },
        itemStyle: { color: '#409EFF' }
      }]
    }]
  })
}

// 入职离职趋势图（真实统计数据，紧邻 AI 流失预测形成"历史-预测"呼应）
const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['入职人数', '离职人数'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: trendData.value.map(d => d.month) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '入职人数',
        type: 'line',
        smooth: true,
        data: trendData.value.map(d => d.hireCount),
        itemStyle: { color: '#67C23A' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
          ])
        }
      },
      {
        name: '离职人数',
        type: 'line',
        smooth: true,
        data: trendData.value.map(d => d.leaveCount),
        itemStyle: { color: '#F56C6C' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245, 108, 108, 0.3)' },
            { offset: 1, color: 'rgba(245, 108, 108, 0.05)' }
          ])
        }
      }
    ]
  })
}

watch(trendPeriod, async () => {
  if (!trendChart) return
  await loadTrendData(TREND_PERIODS[trendPeriod.value] || 12)
  trendChart.setOption({
    xAxis: { data: trendData.value.map(d => d.month) },
    series: [
      { data: trendData.value.map(d => d.hireCount) },
      { data: trendData.value.map(d => d.leaveCount) }
    ]
  })
})

const handleResize = () => {
  deptChart?.resize()
  structureChart?.resize()
  attritionChart?.resize()
  salaryChart?.resize()
  performanceChart?.resize()
  attendanceChart?.resize()
  radarChart?.resize()
  trendChart?.resize()
}

watch(structureType, () => initStructureChart())

onMounted(() => {
  loadAllData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  deptChart?.dispose()
  structureChart?.dispose()
  attritionChart?.dispose()
  salaryChart?.dispose()
  performanceChart?.dispose()
  attendanceChart?.dispose()
  radarChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped lang="scss">
.analytics-container {
  min-height: 100%;
  background: #f0f2f5;
}

.analytics-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #409EFF 0%, #1d6fe0 100%);
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
  &:hover { transform: translateY(-2px); }

  .metric-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; .metric-title { font-size: 14px; color: #606266; } }
  .metric-value { font-size: 28px; font-weight: 600; margin-bottom: 8px; }
  .metric-footer { display: flex; align-items: center; gap: 8px; font-size: 12px;
    .metric-trend { display: flex; align-items: center; &.up { color: #67C23A; } &.down { color: #F56C6C; } }
    .metric-compare { color: #909399; }
  }
}

.chart-row { margin-bottom: 16px; }

.chart-panel { background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); height: 100%; }

.panel-header {
  display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid #ebeef5;
  .panel-title { font-size: 15px; font-weight: 600; color: #303133; position: relative; padding-left: 10px;
    &::before { content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 4px; height: 16px; background: #409EFF; border-radius: 2px; }
  }
}

.chart-content { width: 100%; height: 260px; padding: 10px; &.chart-large { height: 300px; } }

.hiring-prediction { padding: 10px; }

.ai-insights { padding: 10px 16px; }

.insight-item {
  display: flex; gap: 12px; padding: 16px 0; border-bottom: 1px dashed #ebeef5;
  &:last-child { border-bottom: none; }

  .insight-icon {
    width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; font-size: 18px;
    &.warning { background: #E6A23C; } &.success { background: #67C23A; } &.info { background: #909399; } &.primary { background: #409EFF; }
  }

  .insight-content {
    flex: 1;
    .insight-title { font-size: 14px; font-weight: 500; color: #303133; margin-bottom: 6px; }
    .insight-desc { font-size: 13px; color: #606266; line-height: 1.6; margin-bottom: 8px; }
    .insight-suggestion { font-size: 12px; color: #E6A23C; line-height: 1.5; display: flex; gap: 6px; align-items: flex-start; margin-bottom: 6px; }
    .insight-confidence { font-size: 12px; color: #909399; }
  }
}

.no-insights { padding: 40px; }
</style>
