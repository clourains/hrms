<template>
  <div class="dashboard-container">
    <!-- 招聘漏斗：候选人各阶段人数 + 招聘关键指标，一眼看出招聘卡在哪一环 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>招聘漏斗</span>
              <el-button link type="primary" @click="router.push('/recruit/candidate')">
                查看候选人
              </el-button>
            </div>
          </template>
          <div v-loading="funnelLoading" class="funnel-body">
            <div
              v-for="(item, index) in funnelList"
              :key="item.stage"
              class="funnel-row"
              :class="{ clickable: item.count > 0 }"
              @click="item.count > 0 && router.push('/recruit/candidate')"
            >
              <span class="funnel-label">{{ item.name }}</span>
              <div class="funnel-track">
                <div
                  class="funnel-bar"
                  :style="{ width: barWidth(item.count), backgroundColor: STAGE_COLORS[index] }"
                ></div>
              </div>
              <span class="funnel-count">{{ item.count }} 人</span>
            </div>

            <div v-if="!funnelLoading && !funnelList.length" class="funnel-empty">暂无招聘数据</div>
          </div>

          <div class="funnel-foot">
            <div class="foot-item">
              <span class="foot-label">在招需求</span>
              <span class="foot-value">{{ stats.openRequisition ?? 0 }}</span>
            </div>
            <div class="foot-item">
              <span class="foot-label">计划招聘</span>
              <span class="foot-value">{{ stats.headcount ?? 0 }}</span>
            </div>
            <div class="foot-item">
              <span class="foot-label">已录用</span>
              <span class="foot-value">{{ stats.hiredCount ?? 0 }}</span>
            </div>
            <div class="foot-item">
              <span class="foot-label">待招缺口</span>
              <span class="foot-value warn">{{ stats.gap ?? 0 }}</span>
            </div>
            <div class="foot-item">
              <span class="foot-label">待面试</span>
              <span class="foot-value">{{ stats.pendingInterview ?? 0 }}</span>
            </div>
          </div>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCandidateFunnelApi, getRecruitStatisticsApi } from '@/api/recruit'

const router = useRouter()

/** 候选人六个阶段的配色：筛选→初试→复试→待录用 由浅到深，录用绿、淘汰红 */
const STAGE_COLORS = ['#a0cfff', '#79bbff', '#409eff', '#337ecc', '#67c23a', '#f56c6c']

const funnelLoading = ref(false)
const funnelList = ref([])
const stats = ref({})

/** 条形宽度按最大阶段人数归一，全为 0 时不显示条形，避免出现满格误导 */
const barWidth = (count) => {
  const max = Math.max(...funnelList.value.map((x) => Number(x.count) || 0), 0)
  if (!max || !count) return '0%'
  return Math.max(6, Math.round((Number(count) / max) * 100)) + '%'
}

const loadRecruitData = async () => {
  funnelLoading.value = true
  try {
    // 两个接口互不依赖，一起发，任一失败不影响另一个展示
    const [funnelRes, statsRes] = await Promise.allSettled([
      getCandidateFunnelApi(),
      getRecruitStatisticsApi()
    ])
    if (funnelRes.status === 'fulfilled') {
      funnelList.value = funnelRes.value.data || []
    }
    if (statsRes.status === 'fulfilled') {
      stats.value = statsRes.value.data || {}
    }
  } finally {
    funnelLoading.value = false
  }
}

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

onMounted(loadRecruitData)
</script>

<style scoped lang="scss">
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

.funnel-body {
  min-height: 120px;
  padding: 4px 0;
}

.funnel-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;

  &.clickable {
    cursor: pointer;

    &:hover .funnel-label {
      color: #409eff;
    }
  }
}

.funnel-label {
  width: 76px;
  flex: none;
  font-size: 13px;
  color: #606266;
  text-align: right;
}

.funnel-track {
  flex: 1;
  height: 20px;
  border-radius: 4px;
  background-color: #f5f7fa;
  overflow: hidden;
}

.funnel-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

.funnel-count {
  width: 56px;
  flex: none;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.funnel-empty {
  padding: 40px 0;
  text-align: center;
  color: #b1b3b8;
  font-size: 13px;
}

.funnel-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 28px;
  padding-top: 14px;
  margin-top: 6px;
  border-top: 1px dashed #ebeef5;
}

.foot-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.foot-label {
  font-size: 12px;
  color: #909399;
}

.foot-value {
  font-size: 18px;
  font-weight: 700;
  color: #303133;

  &.warn {
    color: #f56c6c;
  }
}
</style>
