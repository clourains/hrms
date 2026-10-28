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
            <div class="metric-footer" v-if="metric.trend !== null || metric.hint">
              <span v-if="metric.trend !== null" class="metric-trend" :class="metric.trend > 0 ? 'up' : 'down'">
                <el-icon><component :is="metric.trend > 0 ? 'CaretTop' : 'CaretBottom'" /></el-icon>
                {{ Math.abs(metric.trend) }}%
              </span>
              <span class="metric-compare">{{ metric.hint || '较上月' }}</span>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- Chart row 1 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :lg="8" :md="12" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">部门人员分布</span>
              <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('dept')" />
            </div>
            <div ref="deptChartRef" class="chart-content"></div>
          </div>
        </el-col>
        <el-col :lg="8" :md="12" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">员工结构分析</span>
              <div class="panel-actions">
                <el-radio-group v-model="structureType" size="small">
                  <el-radio-button label="age">年龄</el-radio-button>
                  <el-radio-button label="education">学历</el-radio-button>
                  <el-radio-button label="gender">性别</el-radio-button>
                </el-radio-group>
                <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('structure')" />
              </div>
            </div>
            <div ref="structureChartRef" class="chart-content"></div>
          </div>
        </el-col>
        <el-col :lg="8" :md="12" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">人员流失率</span>
              <div class="panel-actions">
                <el-tag type="warning" size="small">趋势预测</el-tag>
                <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('attrition')" />
              </div>
            </div>
            <div ref="attritionChartRef" class="chart-content"></div>
          </div>
        </el-col>
      </el-row>

      <!-- Chart row 2 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :lg="12" :md="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">薪资分布分析</span>
              <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('salary')" />
            </div>
            <div ref="salaryChartRef" class="chart-content chart-large"></div>
          </div>
        </el-col>
        <el-col :lg="12" :md="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">绩效等级分布</span>
              <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('performance')" />
            </div>
            <div ref="performanceChartRef" class="chart-content chart-large"></div>
          </div>
        </el-col>
      </el-row>

      <!-- Chart row 3 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :lg="8" :md="12" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">考勤统计分析</span>
              <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('attendance')" />
            </div>
            <div ref="attendanceChartRef" class="chart-content"></div>
          </div>
        </el-col>
        <el-col :lg="8" :md="12" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">人才画像雷达</span>
              <div class="panel-actions">
                <el-tag type="success" size="small">统计计算</el-tag>
                <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('radar')" />
              </div>
            </div>
            <div ref="radarChartRef" class="chart-content"></div>
          </div>
        </el-col>
        <el-col :lg="8" :md="24" :xs="24">
          <div class="chart-panel">
            <div class="panel-header">
              <span class="panel-title">招聘需求预测</span>
              <el-tag type="warning" size="small">规则预估</el-tag>
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
              <div class="panel-actions">
                <el-radio-group v-model="trendPeriod" size="small">
                  <el-radio-button label="year">近一年</el-radio-button>
                  <el-radio-button label="halfYear">近半年</el-radio-button>
                  <el-radio-button label="quarter">近季度</el-radio-button>
                </el-radio-group>
                <el-button link :icon="FullScreen" class="zoom-btn" title="全屏查看" @click="openChartZoom('trend')" />
              </div>
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
              <div class="insight-head">
                <span class="panel-title">智能洞察</span>
                <el-tag :type="aiSource === 'model' ? 'success' : 'info'" size="small" effect="plain">
                  {{ aiSource === 'model' ? `${aiModelName} 生成` : '规则推算（AI 不可用）' }}
                </el-tag>
                <span v-if="aiGeneratedAt" class="insight-time" :class="{ 'is-expired': aiExpired }">
                  <el-icon><Clock /></el-icon>
                  分析于 {{ aiGeneratedAt }}
                  <em class="insight-age" v-if="aiAgeText">{{ aiAgeText }}</em>
                </span>
              </div>
              <el-button
                size="small"
                type="primary"
                plain
                :icon="MagicStick"
                :loading="aiLoading"
                @click="runAiAnalysis"
              >
                {{ aiLoading ? '分析中…' : 'AI 深度分析' }}
              </el-button>
            </div>
            <!-- 结论过期备注：超过 24 小时必须明示，避免把过期结论当作当前数据展示 -->
            <el-alert
              v-if="aiExpireNote"
              :title="aiExpireNote"
              :type="aiExpireLevel === 'stale' ? 'error' : 'warning'"
              :closable="false"
              show-icon
              class="insight-alert"
            />
            <el-alert
              v-if="aiFallback"
              :title="aiFallback"
              type="warning"
              :closable="false"
              show-icon
              class="insight-alert"
            />
            <div class="ai-trend" v-if="aiTrendComment">
              <div class="trend-head">
                <el-icon><TrendCharts /></el-icon>
                <span class="trend-title">未来 12 个月人员趋势</span>
                <el-tag :type="aiSource === 'model' ? 'success' : 'info'" size="small" effect="plain">
                  {{ aiSource === 'model' ? '模型解读' : '规则推算' }}
                </el-tag>
              </div>
              <div class="trend-body">{{ aiTrendComment }}</div>
            </div>
            <div class="ai-insights">
              <div v-for="(insight, index) in aiInsights" :key="index" class="insight-item">
                <div class="insight-icon" :class="insight.type">
                  <el-icon><component :is="getInsightIcon(insight.type)" /></el-icon>
                </div>
                <div class="insight-content">
                  <div class="insight-title-row">
                    <span class="insight-title">{{ insight.title }}</span>
                    <span
                      v-if="insight.evidence && insight.evidence.length"
                      class="ev-link"
                      @click="openEvidence(insight)"
                    >
                      查看依据 · {{ insight.evidence.length }} 条
                      <el-icon><ArrowRight /></el-icon>
                    </span>
                  </div>
                  <div class="insight-desc">{{ insight.description }}</div>
                  <div class="insight-suggestion" v-if="insight.suggestion">
                    <el-icon><Lightning /></el-icon>
                    <span>{{ insight.suggestion }}</span>
                  </div>
                </div>
              </div>
              <div v-if="aiInsights.length === 0" class="no-insights">
                <el-empty description="暂无洞察数据，可点击右上角 AI 深度分析" />
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 洞察依据弹窗：数值全部由后端查库回填，模型只写文字 -->
      <el-dialog v-model="evidenceVisible" width="620px">
        <template #header>
          <div class="ev-dialog-head">
            <span class="ev-dialog-title">{{ activeInsight?.title || '洞察依据' }}</span>
            <el-tag
              v-if="activeInsight"
              :type="activeInsight.type || 'info'"
              size="small"
              effect="plain"
            >{{ insightTypeText(activeInsight.type) }}</el-tag>
          </div>
        </template>
        <div v-if="activeInsight" class="ev-body">
          <div class="ev-block">
            <div class="ev-block-title">结论</div>
            <div class="ev-text">{{ activeInsight.description }}</div>
          </div>

          <div class="ev-block" v-if="activeInsight.evidence && activeInsight.evidence.length">
            <div class="ev-block-title">
              依据明细<span class="ev-count">{{ activeInsight.evidence.length }} 条</span>
            </div>
            <div class="ev-table">
              <div v-for="(row, i) in activeInsight.evidence" :key="i" class="ev-row">
                <div class="ev-row-main">
                  <span class="ev-label">{{ row.label }}</span>
                  <span class="ev-value">{{ row.value }}</span>
                </div>
                <div class="ev-source" v-if="row.source">来源：{{ row.source }}</div>
              </div>
            </div>
          </div>
          <el-alert
            v-else
            title="本条结论由模型归纳，未匹配到可列明的依据行"
            type="info"
            :closable="false"
            show-icon
            class="ev-empty"
          />

          <div class="ev-block" v-if="activeInsight.basis">
            <div class="ev-block-title">计算口径</div>
            <div class="ev-basis">{{ activeInsight.basis }}</div>
          </div>

          <div class="ev-block" v-if="activeInsight.actions && activeInsight.actions.length">
            <div class="ev-block-title">建议动作</div>
            <ol class="ev-actions">
              <li v-for="(a, i) in activeInsight.actions" :key="i">{{ a }}</li>
            </ol>
          </div>
        </div>
        <template #footer>
          <div class="ev-footer">
            <span class="ev-footer-note">
              <el-icon><InfoFilled /></el-icon>
              依据数值取自数据库实时统计<template v-if="aiGeneratedAt">，分析生成于 {{ aiGeneratedAt }}</template>
            </span>
            <div class="ev-footer-actions">
              <el-button
                v-for="(t, i) in (activeInsight && activeInsight.targets) || []"
                :key="i"
                size="small"
                type="primary"
                plain
                @click="goTarget(t)"
              >
                {{ t.label }}
                <el-icon class="el-icon--right"><ArrowRight /></el-icon>
              </el-button>
              <el-button size="small" @click="evidenceVisible = false">关闭</el-button>
            </div>
          </div>
        </template>
      </el-dialog>

      <!-- 单图全屏查看：用源图表当前 option 在全屏弹窗中重建 -->
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
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { DataAnalysis, CaretTop, CaretBottom, User, UserFilled, Warning, TrendCharts, Money, Lightning, InfoFilled, Refresh, MagicStick, ArrowRight, Clock, FullScreen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getDashboardStatsApi, getEmployeeStructureApi, getAttendanceAnalysisApi, getSalaryAnalysisApi, getPerformanceAnalysisApi, getTalentPredictionApi, getAiInsightsApi, getTurnoverTrendApi } from '@/api/analytics'
import { useAppStore } from '@/store/app'

const loading = ref(false)
const updateTime = ref(new Date().toLocaleString('zh-CN'))
const structureType = ref('age')
const appStore = useAppStore()

// 暗黑模式：图表以 ECharts 内置 dark 主题重建，显式适配文字颜色
const chartThemeName = () => (appStore.isDark ? 'dark' : undefined)
const labelText = () => (appStore.isDark ? '#E5EAF3' : '#303133')

const dashboardStats = ref(null)
const employeeStructure = ref(null)
const attendanceAnalysis = ref(null)
const salaryAnalysis = ref(null)
const performanceAnalysis = ref(null)
const talentPrediction = ref(null)
const aiInsights = ref([])

// 洞察来源：model = 大模型生成（默认展示最近一次结果）；rule = 模型不可用时的规则兜底
const aiSource = ref('model')
const aiModelName = ref('AI')
const aiGeneratedAt = ref('')
const aiCached = ref(false)
const aiFallback = ref('')
const aiLoading = ref(false)
// 新鲜度：后端按生成时间判定，fresh(≤24h) / aging(24h~3天) / stale(>3天)
const aiAgeHours = ref(0)
const aiExpireLevel = ref('fresh')
const aiExpireNote = ref('')
// 趋势解读：模型撰写，规则兜底时由后端按真实入离职拼装
const aiTrendComment = ref('')

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

// 单图全屏放大：点击卡片右上角放大按钮后，用源图表当前 option 在全屏弹窗中重建该图
const zoomVisible = ref(false)
const zoomTitle = ref('')
const zoomChartRef = ref(null)
let zoomChart = null

const chartRegistry = {
  dept: { title: '部门人员分布', get: () => deptChart },
  structure: { title: '员工结构分析', get: () => structureChart },
  attrition: { title: '人员流失率', get: () => attritionChart },
  salary: { title: '薪资分布分析', get: () => salaryChart },
  performance: { title: '绩效等级分布', get: () => performanceChart },
  attendance: { title: '考勤统计分析', get: () => attendanceChart },
  radar: { title: '人才画像雷达', get: () => radarChart },
  trend: { title: '人员入职离职趋势', get: () => trendChart }
}

const openChartZoom = async (key) => {
  const item = chartRegistry[key]
  const source = item && item.get()
  if (!source) {
    ElMessage.warning('图表尚未加载完成，请稍后再试')
    return
  }
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

// 入职离职趋势数据（M1-5：真实统计接口，按入职/离职日期按月聚合）
const TREND_PERIODS = { year: 12, halfYear: 6, quarter: 3 }
const trendPeriod = ref('year')
const trendData = ref([])

const loadTrendData = async (months) => {
  const res = await getTurnoverTrendApi(months)
  trendData.value = res.data || []
}

// trend 为 null 表示暂无环比口径（后端未提供历史快照），此时不渲染「较上月」，
// 改用 hint 展示本月真实发生数，避免出现恒为 0 的假环比误导使用者
const coreMetrics = ref([
  { title: '员工总数', value: '--', trend: null, hint: '', icon: 'User', color: '#409EFF' },
  { title: '在职员工', value: '--', trend: null, hint: '', icon: 'UserFilled', color: '#67C23A' },
  { title: '流失率', value: '--', trend: null, hint: '', icon: 'Warning', color: '#E6A23C' },
  { title: '人均薪资', value: '--', trend: null, hint: '', icon: 'Money', color: '#F56C6C' }
])

const hiringData = ref([])

const getInsightIcon = (type) => {
  const map = { warning: 'Warning', success: 'TrendCharts', info: 'Lightning', primary: 'InfoFilled' }
  return map[type] || 'InfoFilled'
}

// ===== 洞察依据弹窗 =====
const evidenceVisible = ref(false)
const activeInsight = ref(null)
const router = useRouter()

const insightTypeText = (type) => ({ warning: '预警', success: '亮点', info: '观察' }[type] || '观察')

const openEvidence = (insight) => {
  activeInsight.value = insight
  evidenceVisible.value = true
}

// 跳转到目标页并带入筛选条件；目标页由后端按类别给出，前端只负责 push
const goTarget = (target) => {
  if (!target || !target.path) return
  evidenceVisible.value = false
  router.push({ path: target.path, query: target.query || {} })
}

const loadAllData = async () => {
  loading.value = true
  updateTime.value = new Date().toLocaleString('zh-CN')
  try {
    const [stats, structure, attendance, salary, performance, prediction, trend] = await Promise.all([
      getDashboardStatsApi(),
      getEmployeeStructureApi(),
      getAttendanceAnalysisApi(),
      getSalaryAnalysisApi(),
      getPerformanceAnalysisApi(),
      getTalentPredictionApi(),
      getTurnoverTrendApi(TREND_PERIODS[trendPeriod.value] || 12)
    ])

    dashboardStats.value = stats.data
    employeeStructure.value = structure.data
    attendanceAnalysis.value = attendance.data
    salaryAnalysis.value = salary.data
    performanceAnalysis.value = performance.data
    talentPrediction.value = prediction.data
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
  // 洞察单独加载：无留存结果时后端会自动生成一次（数秒），不能阻塞图表渲染
  loadInsightSilently()
}

/** 进入页面加载洞察：后端优先回放最近一次结果，没有才生成 */
const loadInsightSilently = async () => {
  aiLoading.value = true
  try {
    const res = await getAiInsightsApi()
    applyInsightPayload(res.data, 'model')
  } catch (e) {
    console.error('Load AI insight error:', e)
  } finally {
    aiLoading.value = false
  }
}

/**
 * 统一处理洞察接口返回。
 * 后端返回 { list, source, model, generatedAt, elapsedMs, cached,
 *          ageHours, expireLevel, expireNote, fallbackReason, trendComment }；
 * 兼容旧版直接返回数组的情况。
 */
const applyInsightPayload = (data, fallbackSource = 'model') => {
  const list = Array.isArray(data) ? data : (data?.list || [])
  aiInsights.value = list
  aiSource.value = Array.isArray(data) ? fallbackSource : (data?.source || fallbackSource)
  aiModelName.value = data?.model || 'AI'
  aiGeneratedAt.value = data?.generatedAt || ''
  aiCached.value = Boolean(data?.cached)
  aiFallback.value = data?.fallbackReason || ''
  aiTrendComment.value = Array.isArray(data) ? '' : (data?.trendComment || '')
  aiAgeHours.value = Array.isArray(data) ? 0 : Number(data?.ageHours || 0)
  aiExpireLevel.value = Array.isArray(data) ? 'fresh' : (data?.expireLevel || 'fresh')
  aiExpireNote.value = Array.isArray(data) ? '' : (data?.expireNote || '')
}

// 结论是否已过期（超过 24 小时）
const aiExpired = computed(() => aiExpireLevel.value === 'aging' || aiExpireLevel.value === 'stale')

// 生成时间距今多久，用于「什么时候的结果」这一眼看清
const aiAgeText = computed(() => {
  if (!aiGeneratedAt.value) return ''
  const h = Number(aiAgeHours.value || 0)
  if (h < 1) return '刚刚生成'
  // 48 小时内仍按小时显示，与过期提示里的「26 小时前」保持同一口径
  if (h < 48) return `${h} 小时前`
  return `${Math.floor(h / 24)} 天前`
})

// 用户主动点击「AI 深度分析」：强制重新生成（无论是否过期都会覆盖当前结果）
const runAiAnalysis = async () => {
  if (aiLoading.value) return
  aiLoading.value = true
  try {
    const res = await getAiInsightsApi(true)
    applyInsightPayload(res.data, 'model')
    if (aiSource.value === 'model') {
      ElMessage.success('分析结果已更新')
    } else {
      ElMessage.warning(aiFallback.value || '智能分析服务暂不可用，已展示规则推算结果')
    }
  } catch (e) {
    console.error('AI insight error:', e)
    ElMessage.error('分析失败，已保留现有结果')
  } finally {
    aiLoading.value = false
  }
}

// 金额缩写：卡片底部空间有限，万元以内直接显示，超过按万折算
const fmtMoney = (n) => {
  const v = Number(n) || 0
  if (v >= 10000) return `¥${(v / 10000).toFixed(1)} 万`
  return `¥${Math.round(v).toLocaleString()}`
}

const updateCoreMetrics = () => {
  if (!dashboardStats.value) return
  const s = dashboardStats.value
  coreMetrics.value[0].value = (s.totalEmployees || 0) + '人'
  coreMetrics.value[1].value = (s.activeEmployees || 0) + '人'
  coreMetrics.value[2].value = (talentPrediction.value?.predictedAttritionRate || 0) + '%'
  coreMetrics.value[3].value = s.avgSalary ? '¥' + Number(s.avgSalary).toLocaleString() : '--'
  // 无真实环比时，底部改成本月真实发生数，比留空或显示 0% 更有参考价值
  coreMetrics.value[0].hint = `本月入职 ${s.newEmployees || 0} 人`
  coreMetrics.value[1].hint = `本月离职 ${s.resignedEmployees || 0} 人`
  coreMetrics.value[2].hint = '近 6 个月离职人数 ÷ 当前在职'
  coreMetrics.value[3].hint = s.totalSalary ? `薪资总额 ${fmtMoney(s.totalSalary)}` : '本月薪资合计'
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
  deptChart?.dispose()
  deptChart = echarts.init(deptChartRef.value, chartThemeName())
  const data = employeeStructure.value?.departmentDistribution || []
  deptChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '55%'],
      itemStyle: { borderRadius: 4, borderColor: appStore.isDark ? '#141414' : '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 11, color: labelText() },
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
  structureChart?.dispose()
  structureChart = echarts.init(structureChartRef.value, chartThemeName())
  const dataMap = {
    age: employeeStructure.value?.ageDistribution || [],
    education: employeeStructure.value?.educationDistribution || [],
    gender: employeeStructure.value?.genderDistribution || []
  }
  structureChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { bottom: '5%', left: 'center', itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11, color: labelText() } },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['50%', '45%'],
      itemStyle: { borderRadius: 4, borderColor: appStore.isDark ? '#141414' : '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', color: labelText() } },
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
  attritionChart?.dispose()
  attritionChart = echarts.init(attritionChartRef.value, chartThemeName())
  const futureData = talentPrediction.value?.futureHeadcount || []
  const attritionRate = talentPrediction.value?.predictedAttritionRate || 0
  const months = futureData.map(d => d.month)
  const counts = futureData.map(d => d.predictedCount)
  attritionChart.setOption({
    backgroundColor: 'transparent',
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
  salaryChart?.dispose()
  salaryChart = echarts.init(salaryChartRef.value, chartThemeName())
  const data = salaryAnalysis.value?.salaryDistribution || []
  salaryChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    // 右上角标注：说明口径（按月工资单）
    graphic: [{
      type: 'text',
      right: 8, top: 4,
      style: { text: '按月工资单', fontSize: 11, fill: appStore.isDark ? '#909399' : '#909399' }
    }],
    grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
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
      label: { show: true, position: 'top', formatter: '{c}人', color: labelText() }
    }]
  })
}

const initPerformanceChart = () => {
  if (!performanceChartRef.value) return
  performanceChart?.dispose()
  performanceChart = echarts.init(performanceChartRef.value, chartThemeName())
  const data = performanceAnalysis.value?.gradeDistribution || []
  performanceChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['50%', '55%'],
      roseType: 'radius',
      itemStyle: { borderRadius: 6 },
      label: { formatter: '{b}\n{d}%', fontSize: 11, color: labelText() },
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
  attendanceChart?.dispose()
  attendanceChart = echarts.init(attendanceChartRef.value, chartThemeName())
  const deptData = attendanceAnalysis.value?.deptAttendance || []
  attendanceChart.setOption({
    backgroundColor: 'transparent',
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
      label: { show: true, position: 'top', formatter: '{c}%', fontSize: 10, color: labelText() }
    }]
  })
}

const initRadarChart = () => {
  if (!radarChartRef.value) return
  radarChart?.dispose()
  radarChart = echarts.init(radarChartRef.value, chartThemeName())
  const dimensions = performanceAnalysis.value?.dimensionAnalysis || []
  radarChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {},
    radar: {
      indicator: dimensions.map(d => ({ name: d.name, max: 100 })),
      center: ['50%', '55%'],
      radius: '65%',
      axisName: { fontSize: 10, color: appStore.isDark ? '#A3A6AD' : '#606266' },
      splitArea: { areaStyle: { color: ['rgba(64, 158, 255, 0.05)', 'rgba(64, 158, 255, 0.1)'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: dimensions.map(d => Math.round((d.avg || 0) * 100) / 100),
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
  trendChart?.dispose()
  trendChart = echarts.init(trendChartRef.value, chartThemeName())
  trendChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: { data: ['入职人数', '离职人数'], top: 0, textStyle: { color: labelText() } },
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

// 暗黑模式切换后重建全部图表
watch(() => appStore.isDark, () => {
  nextTick(() => initAllCharts())
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

.panel-actions { display: flex; align-items: center; gap: 8px; }

.zoom-btn {
  font-size: 16px;
  color: #909399;
  &:hover { color: #409EFF; }
}

.fullscreen-chart { width: 100%; height: calc(100vh - 140px); }

.hiring-prediction { padding: 10px; }

.ai-insights { padding: 10px 16px; }

.insight-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.insight-time {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 12px; color: #909399;
  &.is-expired { color: #E6A23C; }
  .insight-age {
    font-style: normal;
    padding: 0 5px; border-radius: 3px;
    background: rgba(144, 147, 153, 0.14);
  }
  &.is-expired .insight-age { background: rgba(230, 162, 60, 0.16); }
}
.insight-alert { margin: 10px 16px 0; }

/* 未来 12 个月趋势解读：与逐条洞察区分开，作为整体结论放在最前 */
.ai-trend {
  margin: 12px 16px 0; padding: 12px 14px; border-radius: 6px;
  background: var(--el-fill-color-light, #f5f7fa);
  border-left: 3px solid var(--el-color-primary, #409EFF);

  .trend-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
  .trend-title { font-size: 13px; font-weight: 600; color: var(--el-text-color-primary, #303133); }
  .trend-body { font-size: 13px; line-height: 1.7; color: var(--el-text-color-regular, #606266); }
}


.insight-item {
  display: flex; gap: 12px; padding: 16px 0; border-bottom: 1px dashed #ebeef5;
  &:last-child { border-bottom: none; }

  .insight-icon {
    width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; font-size: 18px;
    &.warning { background: #E6A23C; } &.success { background: #67C23A; } &.info { background: #909399; } &.primary { background: #409EFF; }
  }

  .insight-content {
    flex: 1;
    .insight-title-row { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; flex-wrap: wrap; }
    .insight-title { font-size: 14px; font-weight: 500; color: #303133; }
    .ev-link {
      display: inline-flex; align-items: center; gap: 2px; font-size: 12px; color: #409EFF;
      cursor: pointer; user-select: none;
      &:hover { text-decoration: underline; }
    }
    .insight-desc { font-size: 13px; color: #606266; line-height: 1.6; margin-bottom: 8px; }
    .insight-suggestion { font-size: 12px; color: #E6A23C; line-height: 1.5; display: flex; gap: 6px; align-items: flex-start; }
  }
}

.no-insights { padding: 40px; }

/* ===== 洞察依据弹窗 ===== */
.ev-dialog-head { display: flex; align-items: center; gap: 10px; }
.ev-dialog-title { font-size: 15px; font-weight: 600; color: #303133; }
.ev-body { max-height: 62vh; overflow-y: auto; }

.ev-block {
  margin-bottom: 18px;
  &:last-child { margin-bottom: 0; }
}
.ev-block-title {
  display: flex; align-items: center; gap: 8px; margin-bottom: 8px;
  font-size: 12px; font-weight: 600; color: #909399; letter-spacing: .5px;
}
.ev-count {
  font-weight: 400; color: #409EFF; background: rgba(64, 158, 255, .1);
  padding: 1px 6px; border-radius: 8px; letter-spacing: 0;
}
.ev-text { font-size: 13px; line-height: 1.7; color: #606266; }

.ev-table { border: 1px solid #ebeef5; border-radius: 6px; overflow: hidden; }
.ev-row {
  padding: 10px 12px; border-bottom: 1px solid #ebeef5;
  &:last-child { border-bottom: none; }
  &:nth-child(even) { background: #fafafa; }
}
.ev-row-main { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.ev-label { font-size: 13px; color: #606266; }
.ev-value { font-size: 14px; font-weight: 600; color: #303133; white-space: nowrap; }
.ev-source { margin-top: 3px; font-size: 11px; color: #b1b3b8; }

.ev-empty { margin-bottom: 18px; }

.ev-basis {
  padding: 10px 12px; border-radius: 6px; background: #f5f7fa;
  font-size: 12px; line-height: 1.7; color: #606266;
}

.ev-actions {
  margin: 0; padding-left: 18px;
  li {
    font-size: 13px; line-height: 1.7; color: #606266;
    & + li { margin-top: 6px; }
  }
}

.ev-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.ev-footer-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.ev-footer-note { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; }
</style>
