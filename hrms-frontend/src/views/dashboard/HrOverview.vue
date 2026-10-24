<template>
  <div class="dashboard-container">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.title">
        <div class="stat-card" :style="{ borderLeftColor: card.color }">
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.title }}</div>
            <div class="stat-trend" :class="card.trend > 0 ? 'up' : 'down'">
              <el-icon><component :is="card.trend > 0 ? 'CaretTop' : 'CaretBottom'" /></el-icon>
              <span>{{ Math.abs(card.trend) }}% 较上月</span>
            </div>
          </div>
          <div class="stat-icon" :style="{ backgroundColor: card.color + '20', color: card.color }">
            <el-icon :size="36"><component :is="card.icon" /></el-icon>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <!-- 人员结构 -->
      <el-col :lg="12" :md="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>人员结构分布</span>
              <el-radio-group v-model="structureType" size="small">
                <el-radio-button label="department">部门</el-radio-button>
                <el-radio-button label="gender">性别</el-radio-button>
                <el-radio-button label="education">学历</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="structureChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 入职离职趋势 -->
      <el-col :lg="12" :md="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>人员入职离职趋势</span>
              <el-radio-group v-model="trendPeriod" size="small">
                <el-radio-button label="year">近一年</el-radio-button>
                <el-radio-button label="halfYear">近半年</el-radio-button>
                <el-radio-button label="quarter">近季度</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <!-- 考勤概况 -->
      <el-col :lg="8" :md="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span>本月考勤概况</span>
          </template>
          <div ref="attendanceChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 薪资分布 -->
      <el-col :lg="8" :md="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span>薪资区间分布</span>
          </template>
          <div ref="salaryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 绩效分布 -->
      <el-col :lg="8" :md="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span>绩效等级分布</span>
          </template>
          <div ref="performanceChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口和最新动态 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :lg="12" :md="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span>快捷入口</span>
          </template>
          <div class="quick-actions">
            <div v-for="action in quickActions" :key="action.title" class="quick-item" @click="handleQuickAction(action.path)">
              <div class="quick-icon" :style="{ backgroundColor: action.color + '20', color: action.color }">
                <el-icon :size="28"><component :is="action.icon" /></el-icon>
              </div>
              <span class="quick-title">{{ action.title }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :lg="12" :md="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span>最新动态</span>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(item, index) in recentActivities"
              :key="index"
              :timestamp="item.time"
              :color="item.color"
            >
              {{ item.content }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'

const router = useRouter()

const structureType = ref('department')
const trendPeriod = ref('year')

const structureChartRef = ref(null)
const trendChartRef = ref(null)
const attendanceChartRef = ref(null)
const salaryChartRef = ref(null)
const performanceChartRef = ref(null)

let structureChart = null
let trendChart = null
let attendanceChart = null
let salaryChart = null
let performanceChart = null

const statCards = [
  { title: '员工总数', value: '256', trend: 5.2, icon: 'User', color: '#409EFF' },
  { title: '本月入职', value: '12', trend: 12.5, icon: 'UserFilled', color: '#67C23A' },
  { title: '本月离职', value: '3', trend: -25.0, icon: 'SwitchButton', color: '#F56C6C' },
  { title: '出勤率', value: '96.8%', trend: 2.1, icon: 'Calendar', color: '#E6A23C' }
]

const quickActions = [
  { title: '员工入职', icon: 'Plus', color: '#67C23A', path: '/employee' },
  { title: '考勤打卡', icon: 'Calendar', color: '#409EFF', path: '/attendance' },
  { title: '薪资核算', icon: 'Money', color: '#E6A23C', path: '/salary' },
  { title: '绩效评估', icon: 'TrendCharts', color: '#909399', path: '/performance' },
  { title: '部门管理', icon: 'OfficeBuilding', color: '#409EFF', path: '/department' },
  { title: '数据分析', icon: 'DataAnalysis', color: '#67C23A', path: '/analytics' }
]

const recentActivities = [
  { content: '张三 提交了请假申请', time: '10分钟前', color: '#409EFF' },
  { content: '李四 完成了本月绩效自评', time: '30分钟前', color: '#67C23A' },
  { content: '王五 入职，分配到技术部', time: '2小时前', color: '#E6A23C' },
  { content: '赵六 提交了补卡申请', time: '3小时前', color: '#909399' },
  { content: '技术部 新增职位：高级工程师', time: '昨天', color: '#409EFF' }
]

const handleQuickAction = (path) => {
  router.push(path)
}

// 人员结构饼图
const initStructureChart = () => {
  if (!structureChartRef.value) return
  structureChart = echarts.init(structureChartRef.value)

  const departmentData = [
    { name: '技术部', value: 85 },
    { name: '产品部', value: 35 },
    { name: '市场部', value: 28 },
    { name: '运营部', value: 32 },
    { name: '人事部', value: 18 },
    { name: '财务部', value: 15 },
    { name: '行政部', value: 20 },
    { name: '销售部', value: 23 }
  ]

  const genderData = [
    { name: '男', value: 145 },
    { name: '女', value: 111 }
  ]

  const educationData = [
    { name: '博士', value: 12 },
    { name: '硕士', value: 58 },
    { name: '本科', value: 142 },
    { name: '大专', value: 32 },
    { name: '其他', value: 12 }
  ]

  const dataMap = {
    department: departmentData,
    gender: genderData,
    education: educationData
  }

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center'
    },
    series: [
      {
        name: '人员结构',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: dataMap[structureType.value]
      }
    ],
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9b59b6', '#1abc9c', '#34495e']
  }

  structureChart.setOption(option)
}

// 入职离职趋势图
const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['入职人数', '离职人数']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '入职人数',
        type: 'line',
        smooth: true,
        data: [10, 15, 8, 12, 18, 14, 10, 16, 13, 12, 15, 12],
        itemStyle: {
          color: '#67C23A'
        },
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
        data: [5, 3, 4, 6, 2, 5, 4, 3, 5, 4, 3, 3],
        itemStyle: {
          color: '#F56C6C'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245, 108, 108, 0.3)' },
            { offset: 1, color: 'rgba(245, 108, 108, 0.05)' }
          ])
        }
      }
    ]
  }

  trendChart.setOption(option)
}

// 考勤概况
const initAttendanceChart = () => {
  if (!attendanceChartRef.value) return
  attendanceChart = echarts.init(attendanceChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}人 ({d}%)'
    },
    series: [
      {
        name: '考勤概况',
        type: 'pie',
        radius: ['50%', '75%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          position: 'outside',
          formatter: '{b}\n{d}%'
        },
        labelLine: {
          show: true
        },
        data: [
          { value: 248, name: '正常出勤', itemStyle: { color: '#67C23A' } },
          { value: 5, name: '迟到', itemStyle: { color: '#E6A23C' } },
          { value: 2, name: '早退', itemStyle: { color: '#F56C6C' } },
          { value: 1, name: '旷工', itemStyle: { color: '#909399' } }
        ]
      }
    ]
  }

  attendanceChart.setOption(option)
}

// 薪资分布
const initSalaryChart = () => {
  if (!salaryChartRef.value) return
  salaryChart = echarts.init(salaryChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['5k以下', '5-8k', '8-12k', '12-18k', '18-25k', '25k以上'],
      axisLabel: {
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '人数'
    },
    series: [
      {
        name: '人数',
        type: 'bar',
        data: [25, 68, 82, 45, 25, 11],
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#66b1ff' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '50%'
      }
    ]
  }

  salaryChart.setOption(option)
}

// 绩效分布
const initPerformanceChart = () => {
  if (!performanceChartRef.value) return
  performanceChart = echarts.init(performanceChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}人 ({d}%)'
    },
    series: [
      {
        name: '绩效等级',
        type: 'pie',
        radius: '70%',
        center: ['50%', '50%'],
        roseType: 'radius',
        itemStyle: {
          borderRadius: 6
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%'
        },
        data: [
          { value: 38, name: 'S-优秀', itemStyle: { color: '#67C23A' } },
          { value: 95, name: 'A-良好', itemStyle: { color: '#409EFF' } },
          { value: 85, name: 'B-合格', itemStyle: { color: '#E6A23C' } },
          { value: 28, name: 'C-待改进', itemStyle: { color: '#F56C6C' } },
          { value: 10, name: 'D-不合格', itemStyle: { color: '#909399' } }
        ]
      }
    ]
  }

  performanceChart.setOption(option)
}

const handleResize = () => {
  structureChart?.resize()
  trendChart?.resize()
  attendanceChart?.resize()
  salaryChart?.resize()
  performanceChart?.resize()
}

watch(structureType, () => {
  initStructureChart()
})

onMounted(() => {
  initStructureChart()
  initTrendChart()
  initAttendanceChart()
  initSalaryChart()
  initPerformanceChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  structureChart?.dispose()
  trendChart?.dispose()
  attendanceChart?.dispose()
  salaryChart?.dispose()
  performanceChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  .stat-cards {
    margin-bottom: 20px;
  }
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-left: 4px solid #409EFF;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);
  transition: all 0.3s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.12);
  }
}

.stat-info {
  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 6px;
  }

  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-bottom: 8px;
  }

  .stat-trend {
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 2px;

    &.up {
      color: #67C23A;
    }

    &.down {
      color: #F56C6C;
    }
  }
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  width: 100%;
  height: 300px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 10px 0;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background-color: #f5f7fa;
    transform: translateY(-2px);
  }
}

.quick-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-title {
  font-size: 13px;
  color: #606266;
}
</style>
