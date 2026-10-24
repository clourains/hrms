<template>
  <div class="dashboard-container">
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
import { useRouter } from 'vue-router'

const router = useRouter()

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
</style>
