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
              <el-option v-for="p in periodOptions" :key="p.value" :label="p.label" :value="p.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="员工姓名">
            <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="部门">
            <el-select v-model="searchForm.departmentId" placeholder="请选择部门" clearable style="width: 160px">
              <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
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
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
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

    <!-- 新增绩效弹窗 -->
    <el-dialog v-model="addDialogVisible" title="新增绩效" width="600px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="120px">
        <el-form-item label="员工" prop="empId">
          <el-select v-model="addForm.empId" filterable placeholder="请选择员工" style="width: 100%">
            <el-option
              v-for="emp in employeeOptions"
              :key="emp.id"
              :label="emp.name + '（' + emp.employeeNo + '）'"
              :value="emp.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="考核周期" prop="period">
          <el-select v-model="addForm.period" placeholder="请选择考核周期" style="width: 100%">
            <el-option v-for="p in periodOptions" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作质量" prop="workQuality">
          <el-input-number v-model="addForm.workQuality" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工作效率" prop="workEfficiency">
          <el-input-number v-model="addForm.workEfficiency" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工作态度" prop="workAttitude">
          <el-input-number v-model="addForm.workAttitude" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="团队协作" prop="teamwork">
          <el-input-number v-model="addForm.teamwork" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="创新改进" prop="innovation">
          <el-input-number v-model="addForm.innovation" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="addForm.comment" type="textarea" :rows="2" placeholder="请输入评语" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Refresh, Plus, Upload, Download, Setting, TrendCharts,
  Trophy, Medal, Star, Warning
} from '@element-plus/icons-vue'
import { getPerformanceListApi, getPerformanceStatsApi, addPerformanceApi } from '@/api/performance'
import { getEmployeeListApi } from '@/api/employee'
import { getDepartmentListApi } from '@/api/department'

const loading = ref(false)
const reviewDialogVisible = ref(false)
const addDialogVisible = ref(false)
const addFormRef = ref(null)
const submitLoading = ref(false)

const pad = (n) => String(n).padStart(2, '0')

// P042：考核周期改为按自然月生成（与后端 period 字段 yyyy-MM 格式一致），不再写死
const buildPeriodOptions = () => {
  const options = []
  const now = new Date()
  for (let i = 0; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
    options.push({ label: `${d.getFullYear()}年${d.getMonth() + 1}月`, value })
  }
  return options
}
const periodOptions = ref(buildPeriodOptions())

const searchForm = reactive({
  cycleId: periodOptions.value[0].value,
  name: '',
  departmentId: '',
  grade: '',
  status: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 当前选中周期信息（展示用）
const currentCycle = computed(() => {
  const period = searchForm.cycleId || periodOptions.value[0].value
  const [year, month] = period.split('-').map(Number)
  const now = new Date()
  const isOngoing = year === now.getFullYear() && month === now.getMonth() + 1
  return {
    name: `${year}年${month}月`,
    startDate: `${period}-01`,
    endDate: `${period}-${new Date(year, month, 0).getDate()}`,
    status: isOngoing ? 'ongoing' : 'ended'
  }
})

// P042：等级统计改为真实接口数据（后端只评定 A/B/C/D，S 由总数减去 A/B/C/D 推得）
const stats = reactive({
  gradeS: 0,
  gradeA: 0,
  gradeB: 0,
  gradeC: 0
})

// P042：绩效列表改为真实接口加载（后端按周期查询全量，前端做条件过滤+分页）
const allRows = ref([])
const tableData = ref([])
const departmentOptions = ref([])
const employeeOptions = ref([])
let deptNameMap = {}
let empMap = {}

const currentReview = reactive({
  name: '',
  department: ''
})

const reviewForm = reactive({
  selfSummary: '',
  improvementPlan: ''
})

// 指标为纯前端展示数据（后端暂无指标配置接口），不参与提交逻辑
const indicatorList = ref([
  { id: 1, name: '工作业绩', weight: 40, description: '工作目标完成情况、工作质量和效率', selfScore: 0 },
  { id: 2, name: '工作能力', weight: 25, description: '专业技能、学习能力、问题解决能力', selfScore: 0 },
  { id: 3, name: '工作态度', weight: 20, description: '责任心、积极性、团队协作', selfScore: 0 },
  { id: 4, name: '创新改进', weight: 15, description: '创新意识、流程优化、知识分享', selfScore: 0 }
])

const addForm = reactive({
  empId: null,
  period: periodOptions.value[0].value,
  workQuality: 0,
  workEfficiency: 0,
  workAttitude: 0,
  teamwork: 0,
  innovation: 0,
  comment: ''
})

const addRules = {
  empId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  period: [{ required: true, message: '请选择考核周期', trigger: 'change' }]
}

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

const normalizeRow = (item) => {
  const emp = empMap[item.empId] || {}
  return {
    id: item.id,
    employeeNo: emp.empNo || '-',
    name: emp.name || '-',
    department: emp.departmentName || '-',
    position: emp.position || '-',
    // 后端无自评/上级评分字段，两列只能显示未提交
    selfScore: null,
    managerScore: null,
    finalScore: item.totalScore != null ? Number(item.totalScore) : null,
    grade: item.grade || null,
    // 后端无流程状态字段，已有成绩的记录统一按已完成展示
    status: 'completed'
  }
}

const loadBase = async () => {
  try {
    const [empRes, deptRes] = await Promise.all([getEmployeeListApi(), getDepartmentListApi()])
    const depts = deptRes.data || []
    deptNameMap = {}
    depts.forEach(d => { deptNameMap[d.id] = d.deptName })
    departmentOptions.value = depts.map(d => ({ id: d.id, name: d.deptName }))
    const emps = empRes.data || []
    empMap = {}
    emps.forEach(e => {
      empMap[e.id] = { ...e, departmentName: deptNameMap[e.deptId] || '-' }
    })
    employeeOptions.value = emps.map(e => ({ id: e.id, employeeNo: e.empNo, name: e.name }))
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      getPerformanceListApi({ period: searchForm.cycleId }),
      getPerformanceStatsApi({ period: searchForm.cycleId })
    ])
    allRows.value = (listRes.data || []).map(normalizeRow)
    const s = statsRes.data || {}
    const abcd = (s.gradeACount || 0) + (s.gradeBCount || 0) + (s.gradeCCount || 0) + (s.gradeDCount || 0)
    stats.gradeS = Math.max((s.totalCount || 0) - abcd, 0)
    stats.gradeA = s.gradeACount || 0
    stats.gradeB = s.gradeBCount || 0
    stats.gradeC = (s.gradeCCount || 0) + (s.gradeDCount || 0)
    refreshView()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}

/** 前端条件过滤 + 分页切片（后端列表接口仅支持按周期查询） */
const refreshView = () => {
  let rows = allRows.value
  if (searchForm.name) rows = rows.filter(r => r.name.includes(searchForm.name))
  if (searchForm.departmentId) rows = rows.filter(r => r.department === (deptNameMap[searchForm.departmentId] || ''))
  if (searchForm.grade) rows = rows.filter(r => r.grade === searchForm.grade)
  if (searchForm.status) rows = rows.filter(r => r.status === searchForm.status)
  pagination.total = rows.length
  const startIdx = (pagination.currentPage - 1) * pagination.pageSize
  tableData.value = rows.slice(startIdx, startIdx + pagination.pageSize)
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  searchForm.cycleId = periodOptions.value[0].value
  pagination.currentPage = 1
  loadData()
}

const handleAdd = () => {
  Object.assign(addForm, {
    empId: null,
    period: searchForm.cycleId || periodOptions.value[0].value,
    workQuality: 0,
    workEfficiency: 0,
    workAttitude: 0,
    teamwork: 0,
    innovation: 0,
    comment: ''
  })
  addDialogVisible.value = true
}

const submitAdd = async () => {
  if (!addFormRef.value) return
  await addFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      // 总分与绩效等级由后端计算
      await addPerformanceApi({
        empId: addForm.empId,
        period: addForm.period,
        workQuality: addForm.workQuality,
        workEfficiency: addForm.workEfficiency,
        workAttitude: addForm.workAttitude,
        teamwork: addForm.teamwork,
        innovation: addForm.innovation,
        comment: addForm.comment
      })
      ElMessage.success('新增成功')
      addDialogVisible.value = false
      await loadData()
    } finally {
      submitLoading.value = false
    }
  })
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

// 后端暂无考核周期管理接口，保留演示逻辑
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

const handleSizeChange = () => {
  pagination.currentPage = 1
  refreshView()
}

const handleCurrentChange = () => {
  refreshView()
}

onMounted(async () => {
  await loadBase()
  loadData()
})
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
