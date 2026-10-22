<template>
  <div class="performance-container">
    <el-card shadow="hover">
      <!-- 考核周期选择 -->
      <div class="cycle-selector">
        <div class="cycle-info">
          <span class="label">当前考核周期：</span>
          <el-tag type="primary" size="large">{{ currentCycle.name }}</el-tag>
          <span class="cycle-date">{{ currentCycle.startDate }} ~ {{ currentCycle.endDate }}</span>
          <el-tag :type="currentCycle.status === 'ongoing' ? 'success' : 'info'" size="small">
            {{ currentCycle.status === 'ongoing' ? '进行中' : '已结束' }}
          </el-tag>
        </div>
        <div>
          <el-button :icon="Plus" type="primary" @click="handleCreateCycle">创建考核周期</el-button>
        </div>
      </div>

      <!-- 统计概览 -->
      <el-row :gutter="20" class="stat-row">
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon s-grade"><el-icon :size="28"><Trophy /></el-icon></div>
            <div class="stat-info">
              <div class="stat-num">{{ stats.gradeS }}人</div>
              <div class="stat-label">S级（优秀）</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon a-grade"><el-icon :size="28"><Medal /></el-icon></div>
            <div class="stat-info">
              <div class="stat-num">{{ stats.gradeA }}人</div>
              <div class="stat-label">A级（良好）</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon b-grade"><el-icon :size="28"><Star /></el-icon></div>
            <div class="stat-info">
              <div class="stat-num">{{ stats.gradeB }}人</div>
              <div class="stat-label">B级（合格）</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon c-grade"><el-icon :size="28"><Warning /></el-icon></div>
            <div class="stat-info">
              <div class="stat-num">{{ stats.gradeC }}人</div>
              <div class="stat-label">C/D级（待改进）</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="考核周期">
            <el-select v-model="searchForm.cycleId" placeholder="请选择周期" style="width: 180px">
              <el-option label="2024年Q1" :value="1" />
              <el-option label="2023年Q4" :value="2" />
              <el-option label="2023年Q3" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="员工姓名">
            <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="部门">
            <el-select v-model="searchForm.departmentId" placeholder="请选择部门" clearable style="width: 160px">
              <el-option label="技术部" :value="1" />
              <el-option label="产品部" :value="2" />
              <el-option label="市场部" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="绩效等级">
            <el-select v-model="searchForm.grade" placeholder="全部等级" clearable style="width: 140px">
              <el-option label="S-优秀" value="S" />
              <el-option label="A-良好" value="A" />
              <el-option label="B-合格" value="B" />
              <el-option label="C-待改进" value="C" />
              <el-option label="D-不合格" value="D" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 140px">
              <el-option label="待自评" value="pending_self" />
              <el-option label="待上级评" value="pending_manager" />
              <el-option label="待面谈" value="pending_interview" />
              <el-option label="已完成" value="completed" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增绩效</el-button>
        <el-button type="success" :icon="Upload" @click="handleImport">导入</el-button>
        <el-button type="warning" :icon="Download" @click="handleExport">导出</el-button>
        <el-button type="primary" :icon="Setting" @click="handleIndicators">指标配置</el-button>
        <el-button type="info" :icon="TrendCharts" @click="handleAnalytics">绩效分析</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="employeeNo" label="工号" width="100" align="center" />
        <el-table-column prop="name" label="姓名" width="100" align="center" />
        <el-table-column prop="department" label="部门" width="120" align="center" />
        <el-table-column prop="position" label="职位" width="120" align="center" />
        <el-table-column prop="selfScore" label="自评得分" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.selfScore !== null">{{ row.selfScore }}</span>
            <el-tag v-else type="info" size="small">未提交</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="managerScore" label="上级评分" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.managerScore !== null">{{ row.managerScore }}</span>
            <el-tag v-else type="info" size="small">未评分</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="finalScore" label="最终得分" width="100" align="center">
          <template #default="{ row }">
            <span class="final-score" v-if="row.finalScore !== null">{{ row.finalScore }}</span>
            <el-tag v-else type="info" size="small">-</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="grade" label="绩效等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.grade" :type="getGradeTagType(row.grade)" effect="dark" size="small">
              {{ row.grade }}
            </el-tag>
            <el-tag v-else type="info" size="small">待定</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">详情</el-button>
            <el-button v-if="row.status === 'pending_self'" type="warning" link size="small" @click="handleSelfReview(row)">
              自评
            </el-button>
            <el-button v-if="row.status === 'pending_manager'" type="success" link size="small" @click="handleManagerReview(row)">
              评分
            </el-button>
            <el-button v-if="row.status === 'pending_interview'" type="primary" link size="small" @click="handleInterview(row)">
              面谈
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>

    <!-- 自评弹窗 -->
    <el-dialog v-model="reviewDialogVisible" title="绩效自评" width="700px" destroy-on-close>
      <div class="review-header">
        <span>员工：{{ currentReview.name }}</span>
        <span>部门：{{ currentReview.department }}</span>
        <span>周期：{{ currentCycle.name }}</span>
      </div>
      <el-form :model="reviewForm" label-width="120px">
        <el-table :data="indicatorList" border size="small">
          <el-table-column prop="name" label="考核指标" width="200" />
          <el-table-column prop="weight" label="权重" width="100" align="center">
            <template #default="{ row }">{{ row.weight }}%</template>
          </el-table-column>
          <el-table-column prop="description" label="指标说明" min-width="200" />
          <el-table-column label="自评分" width="150" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.selfScore"
                :min="0"
                :max="100"
                size="small"
                style="width: 100%"
              />
            </template>
          </el-table-column>
        </el-table>
        <el-form-item label="自我总结" style="margin-top: 20px">
          <el-input v-model="reviewForm.selfSummary" type="textarea" :rows="4" placeholder="请输入本周期的工作内容和自我总结" />
        </el-form-item>
        <el-form-item label="改进计划">
          <el-input v-model="reviewForm.improvementPlan" type="textarea" :rows="3" placeholder="请输入下一周期的改进计划" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSelfReview">提交自评</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Refresh, Plus, Upload, Download, Setting, TrendCharts,
  Trophy, Medal, Star, Warning
} from '@element-plus/icons-vue'

const loading = ref(false)
const reviewDialogVisible = ref(false)

const currentCycle = reactive({
  id: 1,
  name: '2024年第一季度',
  startDate: '2024-01-01',
  endDate: '2024-03-31',
  status: 'ongoing'
})

const stats = reactive({
  gradeS: 38,
  gradeA: 95,
  gradeB: 85,
  gradeC: 38
})

const searchForm = reactive({
  cycleId: 1,
  name: '',
  departmentId: '',
  grade: '',
  status: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 56
})

const tableData = ref([
  { id: 1, employeeNo: 'E001', name: '张三', department: '技术部', position: '高级工程师', selfScore: 92, managerScore: 90, finalScore: 90.5, grade: 'A', status: 'completed' },
  { id: 2, employeeNo: 'E002', name: '李四', department: '技术部', position: '前端工程师', selfScore: 85, managerScore: 82, finalScore: 83, grade: 'B', status: 'completed' },
  { id: 3, employeeNo: 'E003', name: '王五', department: '产品部', position: '产品经理', selfScore: 95, managerScore: null, finalScore: null, grade: null, status: 'pending_manager' },
  { id: 4, employeeNo: 'E004', name: '赵六', department: '市场部', position: '市场专员', selfScore: null, managerScore: null, finalScore: null, grade: null, status: 'pending_self' },
  { id: 5, employeeNo: 'E005', name: '钱七', department: '运营部', position: '运营主管', selfScore: 88, managerScore: 92, finalScore: 91, grade: 'A', status: 'pending_interview' },
  { id: 6, employeeNo: 'E006', name: '孙八', department: '人事部', position: 'HR专员', selfScore: 80, managerScore: 78, finalScore: 79, grade: 'C', status: 'completed' },
  { id: 7, employeeNo: 'E007', name: '周九', department: '财务部', position: '会计', selfScore: 86, managerScore: 85, finalScore: 85.5, grade: 'B', status: 'completed' },
  { id: 8, employeeNo: 'E008', name: '吴十', department: '行政部', position: '行政专员', selfScore: null, managerScore: null, finalScore: null, grade: null, status: 'pending_self' },
  { id: 9, employeeNo: 'E009', name: '郑十一', department: '技术部', position: '后端工程师', selfScore: 96, managerScore: 94, finalScore: 94.5, grade: 'S', status: 'completed' },
  { id: 10, employeeNo: 'E010', name: '冯十二', department: '销售部', position: '销售经理', selfScore: 90, managerScore: 88, finalScore: 89, grade: 'B', status: 'pending_interview' }
])

const currentReview = reactive({
  name: '',
  department: ''
})

const reviewForm = reactive({
  selfSummary: '',
  improvementPlan: ''
})

const indicatorList = ref([
  { id: 1, name: '工作业绩', weight: 40, description: '工作目标完成情况、工作质量和效率', selfScore: 0 },
  { id: 2, name: '工作能力', weight: 25, description: '专业技能、学习能力、问题解决能力', selfScore: 0 },
  { id: 3, name: '工作态度', weight: 20, description: '责任心、积极性、团队协作', selfScore: 0 },
  { id: 4, name: '创新改进', weight: 15, description: '创新意识、流程优化、知识分享', selfScore: 0 }
])

const getGradeTagType = (grade) => {
  const map = { S: 'success', A: 'primary', B: 'warning', C: 'danger', D: 'info' }
  return map[grade] || 'info'
}

const getStatusText = (status) => {
  const map = {
    pending_self: '待自评',
    pending_manager: '待上级评',
    pending_interview: '待面谈',
    completed: '已完成'
  }
  return map[status] || '未知'
}

const getStatusTagType = (status) => {
  const map = {
    pending_self: 'warning',
    pending_manager: 'primary',
    pending_interview: 'success',
    completed: 'info'
  }
  return map[status] || 'info'
}

const handleSearch = () => {
  ElMessage.success('搜索功能演示')
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
}

const handleAdd = () => {
  ElMessage.info('新增绩效功能演示')
}

const handleImport = () => {
  ElMessage.info('导入功能演示')
}

const handleExport = () => {
  ElMessage.info('导出功能演示')
}

const handleIndicators = () => {
  ElMessage.info('指标配置功能演示')
}

const handleAnalytics = () => {
  ElMessage.info('绩效分析功能演示')
}

const handleCreateCycle = () => {
  ElMessage.info('创建考核周期功能演示')
}

const handleView = (row) => {
  ElMessage.info(`查看 ${row.name} 的绩效详情`)
}

const handleSelfReview = (row) => {
  currentReview.name = row.name
  currentReview.department = row.department
  indicatorList.value.forEach(item => { item.selfScore = 0 })
  reviewForm.selfSummary = ''
  reviewForm.improvementPlan = ''
  reviewDialogVisible.value = true
}

const handleManagerReview = (row) => {
  ElMessage.info(`为 ${row.name} 进行上级评分`)
}

const handleInterview = (row) => {
  ElMessage.info(`与 ${row.name} 进行绩效面谈`)
}

const submitSelfReview = () => {
  ElMessage.success('自评提交成功')
  reviewDialogVisible.value = false
}
</script>

<style scoped lang="scss">
.performance-container {
  .cycle-selector {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
    border-radius: 8px;
    margin-bottom: 20px;

    .cycle-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .label {
        font-size: 14px;
        color: #606266;
      }

      .cycle-date {
        font-size: 13px;
        color: #909399;
      }
    }
  }

  .stat-row {
    margin-bottom: 24px;
  }

  .stat-card {
    display: flex;
    align-items: center;
    gap: 16px;
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;

      &.s-grade { background: linear-gradient(135deg, #67C23A, #85ce61); }
      &.a-grade { background: linear-gradient(135deg, #409EFF, #66b1ff); }
      &.b-grade { background: linear-gradient(135deg, #E6A23C, #ebb563); }
      &.c-grade { background: linear-gradient(135deg, #F56C6C, #f78989); }
    }

    .stat-info {
      .stat-num {
        font-size: 22px;
        font-weight: 600;
        color: #303133;
      }

      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .search-bar {
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid #ebeef5;
  }

  .action-bar {
    margin-bottom: 16px;
  }

  .final-score {
    font-weight: 600;
    color: #409EFF;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .review-header {
    display: flex;
    justify-content: space-around;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 6px;
    margin-bottom: 20px;
    font-size: 14px;
    color: #606266;
  }
}
</style>
