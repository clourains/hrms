<template>
  <div class="page-container">
    <!-- 招聘概览 -->
    <el-row :gutter="12" class="stat-row">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.label">
        <div class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-sub">{{ card.sub }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="hover">
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="关键字">
            <el-input
              v-model="searchForm.keyword"
              placeholder="岗位 / 需求编号 / 提出人"
              clearable
              style="width: 220px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="部门">
            <el-select v-model="searchForm.deptId" placeholder="全部部门" clearable style="width: 160px">
              <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 130px">
              <el-option label="招聘中" :value="1" />
              <el-option label="已暂停" :value="2" />
              <el-option label="已完成" :value="3" />
              <el-option label="已关闭" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增需求</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="reqNo" label="需求编号" width="150" />
        <el-table-column prop="position" label="招聘岗位" min-width="140" show-overflow-tooltip />
        <el-table-column prop="deptName" label="需求部门" width="120" show-overflow-tooltip />
        <el-table-column label="招聘进度" width="215">
          <template #default="{ row }">
            <div class="progress-cell" @click="goCandidates(row)">
              <div class="progress-head">
                <span class="progress-text">已录用 <b>{{ row.hiredCount || 0 }}</b> / {{ row.headcount }} 人</span>
                <span class="progress-pct" :style="{ color: progressColor(row) }">{{ progressOf(row) }}%</span>
              </div>
              <el-progress
                :percentage="progressOf(row)"
                :stroke-width="8"
                :color="progressColor(row)"
                :show-text="false"
              />
              <div class="funnel-mini">
                <span class="mini-item">投递 {{ row.candidateCount || 0 }}</span>
                <span class="mini-sep">·</span>
                <span class="mini-item mini-active">在用 {{ row.activeCount || 0 }}</span>
                <span class="mini-sep">·</span>
                <span class="mini-item mini-rejected">淘汰 {{ row.rejectedCount || 0 }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="urgency" label="紧急度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="urgencyMap[row.urgency]?.type || 'info'" size="small">
              {{ urgencyMap[row.urgency]?.text || '一般' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="workLocation" label="工作地点" width="130" show-overflow-tooltip />
        <el-table-column prop="deadline" label="期望到岗" width="115" />
        <el-table-column prop="status" label="状态" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleStatus(row, cmd)">
              <el-button type="primary" link>
                状态<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="1" :disabled="row.status === 1">恢复招聘</el-dropdown-item>
                  <el-dropdown-item :command="2" :disabled="row.status === 2">暂停招聘</el-dropdown-item>
                  <el-dropdown-item :command="3" :disabled="row.status === 3">标记完成</el-dropdown-item>
                  <el-dropdown-item :command="4" :disabled="row.status === 4">关闭需求</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSizeChange"
        @current-change="loadData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑招聘需求' : '新增招聘需求'" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="招聘岗位" prop="position">
              <el-input v-model="form.position" placeholder="如 外贸业务员" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需求部门" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择部门" style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="招聘人数" prop="headcount">
              <el-input-number v-model="form.headcount" :min="1" :max="99" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="紧急程度" prop="urgency">
              <el-select v-model="form.urgency" style="width: 100%">
                <el-option label="一般" :value="1" />
                <el-option label="紧急" :value="2" />
                <el-option label="非常紧急" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="期望到岗" prop="deadline">
              <el-date-picker
                v-model="form.deadline"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="薪资下限">
              <el-input-number v-model="form.salaryMin" :min="0" :max="999" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="薪资上限">
              <el-input-number v-model="form.salaryMax" :min="0" :max="999" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="经验要求" prop="experience">
              <el-select v-model="form.experience" placeholder="请选择" style="width: 100%">
                <el-option v-for="e in experienceOptions" :key="e" :label="e" :value="e" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="学历要求" prop="education">
              <el-select v-model="form.education" placeholder="请选择" style="width: 100%">
                <el-option v-for="e in educationOptions" :key="e" :label="e" :value="e" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作地点" prop="workLocation">
              <el-input v-model="form.workLocation" placeholder="如 深圳 / 迪拜" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="提出人">
              <el-input v-model="form.applicant" placeholder="如 王市场" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="岗位职责">
          <el-input v-model="form.jobDuty" type="textarea" :rows="3" placeholder="一行一条，便于候选人了解工作内容" />
        </el-form-item>
        <el-form-item label="任职要求">
          <el-input v-model="form.requirement" type="textarea" :rows="3" placeholder="如：英语可作为工作语言、有外贸客户开发经验" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, ArrowDown } from '@element-plus/icons-vue'
import {
  getRequisitionPageApi,
  getRecruitStatisticsApi,
  addRequisitionApi,
  updateRequisitionApi,
  deleteRequisitionApi,
  changeRequisitionStatusApi
} from '@/api/recruit'
import { getDepartmentListApi } from '@/api/department'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const deptOptions = ref([])
const stats = ref({})

const searchForm = reactive({ keyword: '', deptId: null, status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const statusMap = {
  1: { text: '招聘中', type: 'success' },
  2: { text: '已暂停', type: 'info' },
  3: { text: '已完成', type: 'primary' },
  4: { text: '已关闭', type: 'danger' }
}

const urgencyMap = {
  1: { text: '一般', type: 'info' },
  2: { text: '紧急', type: 'warning' },
  3: { text: '非常紧急', type: 'danger' }
}

const educationOptions = ['不限', '大专', '本科', '硕士', '博士']
const experienceOptions = ['不限', '应届生', '1-3年', '3-5年', '5-10年', '10年以上']

const defaultForm = {
  id: null,
  position: '',
  deptId: null,
  headcount: 1,
  urgency: 1,
  salaryMin: null,
  salaryMax: null,
  education: '本科',
  experience: '不限',
  workLocation: '',
  deadline: '',
  applicant: '',
  jobDuty: '',
  requirement: '',
  remark: ''
}
const form = reactive({ ...defaultForm })

// 新增与编辑同一套必填口径，避免「新增必填 → 编辑清空」绕过校验
const rules = {
  position: [{ required: true, message: '请输入招聘岗位', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择需求部门', trigger: 'change' }],
  headcount: [{ required: true, message: '请输入招聘人数', trigger: 'blur' }],
  urgency: [{ required: true, message: '请选择紧急程度', trigger: 'change' }],
  education: [{ required: true, message: '请选择学历要求', trigger: 'change' }],
  experience: [{ required: true, message: '请选择经验要求', trigger: 'change' }],
  workLocation: [{ required: true, message: '请输入工作地点', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择期望到岗日期', trigger: 'change' }]
}

const statCards = computed(() => [
  { label: '在招需求', value: stats.value.openRequisition ?? 0, sub: `累计 ${stats.value.totalRequisition ?? 0} 条`, color: '#409EFF' },
  { label: '计划招聘', value: stats.value.headcount ?? 0, sub: '在招需求人数合计', color: '#67C23A' },
  { label: '已录用', value: stats.value.hiredCount ?? 0, sub: '在招需求已招人数', color: '#E6A23C' },
  { label: '待招缺口', value: stats.value.gap ?? 0, sub: '计划 - 已录用', color: '#F56C6C' }
])

const progressOf = (row) => {
  const total = row.headcount || 0
  if (total <= 0) return 0
  return Math.min(100, Math.round(((row.hiredCount || 0) / total) * 100))
}

const progressColor = (row) => {
  const p = progressOf(row)
  if (p >= 100) return '#67C23A'
  if (p >= 50) return '#409EFF'
  return '#E6A23C'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRequisitionPageApi({
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  const res = await getRecruitStatisticsApi()
  stats.value = res.data || {}
}

const loadDepts = async () => {
  const res = await getDepartmentListApi()
  deptOptions.value = res.data || []
}

const handleSearch = () => { pagination.pageNum = 1; loadData() }
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.deptId = null
  searchForm.status = null
  handleSearch()
}
const handleSizeChange = () => { pagination.pageNum = 1; loadData() }

const handleAdd = () => {
  Object.assign(form, defaultForm)
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, defaultForm, row)
  isEdit.value = true
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (form.salaryMin !== null && form.salaryMax !== null && form.salaryMin > form.salaryMax) {
    ElMessage.warning('薪资下限不能高于上限')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateRequisitionApi(form)
      ElMessage.success('更新成功')
    } else {
      await addRequisitionApi(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
    loadStats()
  } finally {
    submitting.value = false
  }
}

const handleStatus = async (row, status) => {
  const label = statusMap[status]?.text || '变更'
  await ElMessageBox.confirm(`确定把「${row.position}」的状态改为「${label}」吗？`, '提示', { type: 'warning' })
  await changeRequisitionStatusApi(row.id, status)
  ElMessage.success('状态已更新')
  loadData()
  loadStats()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(
    `确定删除招聘需求「${row.position}」吗？该需求下若还有候选人不允许删除。`,
    '提示',
    { type: 'warning' }
  )
  await deleteRequisitionApi(row.id)
  ElMessage.success('删除成功')
  loadData()
  loadStats()
}

const goCandidates = (row) => {
  router.push({ path: '/recruit/candidate', query: { requisitionId: row.id } })
}

onMounted(async () => {
  // 智能洞察跳转：?deptId=x&status=y 自动带入部门与需求状态筛选
  if (route.query.deptId) {
    searchForm.deptId = Number(route.query.deptId)
  }
  if (route.query.status) {
    searchForm.status = Number(route.query.status)
  }
  await loadDepts()
  await Promise.all([loadData(), loadStats()])
})
</script>

<style scoped>
.page-container { width: 100%; }
.stat-row { margin-bottom: 12px; }
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  margin-bottom: 12px;
}
.stat-label { font-size: 13px; color: #909399; }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.4; }
.stat-sub { font-size: 12px; color: #b1b3b8; }
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.progress-cell { line-height: 1.4; cursor: pointer; }
.progress-cell:hover .progress-text { color: #409eff; }
.progress-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px; }
.progress-text { font-size: 12px; color: #606266; }
.progress-text b { color: #303133; font-size: 13px; }
.progress-pct { font-size: 12px; font-weight: 600; }
.funnel-mini { display: flex; align-items: center; gap: 4px; margin-top: 3px; font-size: 11px; color: #909399; }
.funnel-mini .mini-active { color: #409eff; }
.funnel-mini .mini-rejected { color: #f56c6c; }
.funnel-mini .mini-sep { color: #dcdfe6; }
</style>
