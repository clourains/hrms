<template>
  <div class="page-container">
    <!-- 试用期概览 -->
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
              placeholder="员工姓名 / 工号"
              clearable
              style="width: 190px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
              <el-option label="试用中" :value="1" />
              <el-option label="已转正" :value="2" />
              <el-option label="未通过" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="到期提醒">
            <el-switch v-model="onlyExpiring" active-text="30天内到期" @change="handleSearch" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" :icon="Plus" @click="handleAdd">登记试用期</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="员工" width="140">
          <template #default="{ row }">
            <div class="emp-cell">
              <span class="emp-name">{{ row.employeeName || '-' }}</span>
              <span class="emp-no">{{ row.empNo || '' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="部门" width="105" show-overflow-tooltip />
        <el-table-column prop="position" label="职位" width="145" show-overflow-tooltip />
        <el-table-column prop="probationStart" label="试用开始" width="110" />
        <el-table-column prop="probationEnd" label="试用结束" width="110" />
        <el-table-column label="剩余天数" width="120" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.status === 1"
              :type="daysType(row.daysLeft)"
              size="small"
            >{{ daysText(row.daysLeft) }}</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="mentor" label="导师" width="90">
          <template #default="{ row }">{{ row.mentor || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分 / 评价" min-width="185" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.score !== null && row.score !== undefined" class="score">{{ row.score }} 分</span>
            <span class="eval">{{ row.evaluation || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="135" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button type="success" link @click="openConfirm(row)">转正</el-button>
              <el-dropdown trigger="click" @command="(cmd) => handleMore(row, cmd)">
                <el-button type="primary" link>
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="extend">延长试用期</el-dropdown-item>
                    <el-dropdown-item command="fail">不予转正</el-dropdown-item>
                    <el-dropdown-item command="edit">编辑记录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </template>
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

    <!-- 登记 / 编辑试用期 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑试用期记录' : '登记试用期'"
      width="620px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="员工" prop="employeeId">
          <el-select
            v-model="form.employeeId"
            placeholder="请选择员工"
            filterable
            :disabled="isEdit"
            style="width: 100%"
            @change="onEmployeeChange"
          >
            <el-option
              v-for="e in employeeOptions"
              :key="e.id"
              :label="`${e.name}（${e.empNo}）`"
              :value="e.id"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属部门" prop="deptId">
              <el-select v-model="form.deptId" style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位" prop="position">
              <el-input v-model="form.position" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="试用开始" prop="probationStart">
              <el-date-picker
                v-model="form.probationStart"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                @change="autoEndDate"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="试用结束" prop="probationEnd">
              <el-date-picker
                v-model="form.probationEnd"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="带教导师">
          <el-input v-model="form.mentor" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 转正 -->
    <el-dialog v-model="confirmVisible" title="办理转正" width="480px" destroy-on-close>
      <el-alert
        type="success"
        :closable="false"
        show-icon
        :title="`确认后员工「${currentRow?.employeeName}」状态将变为在职，并记录转正日期。`"
        style="margin-bottom: 14px"
      />
      <el-form label-width="90px">
        <el-form-item label="转正评分">
          <el-input-number v-model="confirmForm.score" :min="0" :max="100" style="width: 160px" />
          <span class="tip">（必填 0~100）</span>
        </el-form-item>
        <el-form-item label="转正评价">
          <el-input v-model="confirmForm.evaluation" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmVisible = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="submitConfirm">确认转正</el-button>
      </template>
    </el-dialog>

    <!-- 延长试用期 -->
    <el-dialog v-model="extendVisible" title="延长试用期" width="460px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="原结束日期">
          <span>{{ currentRow?.probationEnd }}</span>
        </el-form-item>
        <el-form-item label="新结束日期">
          <el-date-picker
            v-model="extendForm.newEnd"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="延期原因">
          <el-input v-model="extendForm.evaluation" type="textarea" :rows="2" placeholder="选填，建议写明原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="extendVisible = false">取消</el-button>
        <el-button type="warning" :loading="submitting" @click="submitExtend">确认延期</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, ArrowDown } from '@element-plus/icons-vue'
import {
  getProbationPageApi,
  getProbationStatisticsApi,
  addProbationApi,
  updateProbationApi,
  deleteProbationApi,
  confirmProbationApi,
  failProbationApi,
  extendProbationApi
} from '@/api/recruit'
import { getEmployeeListApi } from '@/api/employee'
import { getDepartmentListApi } from '@/api/department'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const employeeOptions = ref([])
const deptOptions = ref([])
const stats = ref({})
const onlyExpiring = ref(false)

const dialogVisible = ref(false)
const confirmVisible = ref(false)
const extendVisible = ref(false)
const isEdit = ref(false)
const currentRow = ref(null)
const formRef = ref(null)

const searchForm = reactive({ keyword: '', status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const statusMap = {
  1: { text: '试用中', type: 'warning' },
  2: { text: '已转正', type: 'success' },
  3: { text: '未通过', type: 'danger' },
  4: { text: '已延期', type: 'info' }
}

const defaultForm = {
  id: null,
  employeeId: null,
  deptId: null,
  position: '',
  probationStart: '',
  probationEnd: '',
  mentor: ''
}
const form = reactive({ ...defaultForm })

const rules = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  position: [{ required: true, message: '请填写职位', trigger: 'blur' }],
  probationStart: [{ required: true, message: '请选择试用开始日期', trigger: 'change' }],
  probationEnd: [{ required: true, message: '请选择试用结束日期', trigger: 'change' }]
}

const confirmForm = reactive({ score: 80, evaluation: '' })
const extendForm = reactive({ newEnd: '', evaluation: '' })

const statCards = computed(() => [
  { label: '试用中', value: stats.value.probationing ?? 0, sub: '待办理转正', color: '#E6A23C' },
  { label: '30 天内到期', value: stats.value.expiringIn30 ?? 0, sub: '需要安排评估', color: '#409EFF' },
  { label: '已逾期', value: stats.value.overdue ?? 0, sub: '超过试用期未办理', color: '#F56C6C' },
  { label: '本月已转正', value: stats.value.regularThisMonth ?? 0, sub: `累计未通过 ${stats.value.failedTotal ?? 0} 人`, color: '#67C23A' }
])

const daysText = (days) => {
  if (days === null || days === undefined) return '-'
  if (days < 0) return `已逾期 ${Math.abs(days)} 天`
  if (days === 0) return '今天到期'
  return `${days} 天后到期`
}

const daysType = (days) => {
  if (days === null || days === undefined) return 'info'
  if (days < 0) return 'danger'
  if (days <= 7) return 'danger'
  if (days <= 30) return 'warning'
  return 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getProbationPageApi({
      ...searchForm,
      expireDays: onlyExpiring.value ? 30 : null,
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
  const res = await getProbationStatisticsApi()
  stats.value = res.data || {}
}

const loadOptions = async () => {
  const [empRes, deptRes] = await Promise.all([getEmployeeListApi(), getDepartmentListApi()])
  employeeOptions.value = empRes.data || []
  deptOptions.value = deptRes.data || []
}

const handleSearch = () => { pagination.pageNum = 1; loadData() }
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = null
  onlyExpiring.value = false
  handleSearch()
}
const handleSizeChange = () => { pagination.pageNum = 1; loadData() }

const handleAdd = () => {
  Object.assign(form, defaultForm)
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, defaultForm, {
    id: row.id,
    employeeId: row.employeeId,
    deptId: row.deptId,
    position: row.position,
    probationStart: row.probationStart,
    probationEnd: row.probationEnd,
    mentor: row.mentor
  })
  isEdit.value = true
  dialogVisible.value = true
}

const onEmployeeChange = (empId) => {
  const emp = employeeOptions.value.find((e) => e.id === empId)
  if (!emp) return
  if (!form.deptId) form.deptId = emp.deptId
  if (!form.position) form.position = emp.position
  if (!form.probationStart && emp.entryDate) {
    form.probationStart = emp.entryDate
    autoEndDate(emp.entryDate)
  }
}

const autoEndDate = (start) => {
  if (!start) return
  const d = new Date(start)
  d.setMonth(d.getMonth() + 3)
  form.probationEnd = d.toISOString().slice(0, 10)
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (form.probationEnd < form.probationStart) {
    ElMessage.warning('试用期结束日期不能早于开始日期')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateProbationApi(form)
      ElMessage.success('更新成功')
    } else {
      await addProbationApi(form)
      ElMessage.success('登记成功')
    }
    dialogVisible.value = false
    loadData()
    loadStats()
  } finally {
    submitting.value = false
  }
}

const openConfirm = (row) => {
  currentRow.value = row
  confirmForm.score = row.score ?? 80
  confirmForm.evaluation = ''
  confirmVisible.value = true
}

/** 操作列收起的三项：延长试用期 / 不予转正 / 编辑记录 */
const handleMore = (row, command) => {
  if (command === 'extend') openExtend(row)
  else if (command === 'fail') handleFail(row)
  else if (command === 'edit') handleEdit(row)
}

const submitConfirm = async () => {
  if (confirmForm.score === null || confirmForm.score === undefined) {
    ElMessage.warning('请填写转正评分')
    return
  }
  submitting.value = true
  try {
    await confirmProbationApi(currentRow.value.id, { ...confirmForm })
    ElMessage.success('已办理转正，员工状态已更新为在职')
    confirmVisible.value = false
    loadData()
    loadStats()
  } finally {
    submitting.value = false
  }
}

const handleFail = async (row) => {
  const { value } = await ElMessageBox.prompt(
    `确定对「${row.employeeName}」办理不予转正吗？员工状态会变为离职。请填写原因：`,
    '不予转正',
    { type: 'warning', inputPlaceholder: '如：试用期考核不达标', inputValidator: (v) => (v && v.trim() ? true : '原因必填') }
  )
  await failProbationApi(row.id, { evaluation: value })
  ElMessage.success('已记录为未通过')
  loadData()
  loadStats()
}

const openExtend = (row) => {
  currentRow.value = row
  extendForm.newEnd = ''
  extendForm.evaluation = ''
  extendVisible.value = true
}

const submitExtend = async () => {
  if (!extendForm.newEnd) {
    ElMessage.warning('请选择新的结束日期')
    return
  }
  submitting.value = true
  try {
    await extendProbationApi(currentRow.value.id, { ...extendForm })
    ElMessage.success('试用期已延长')
    extendVisible.value = false
    loadData()
    loadStats()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除「${row.employeeName}」的试用期记录吗？`, '提示', { type: 'warning' })
  await deleteProbationApi(row.id)
  ElMessage.success('删除成功')
  loadData()
  loadStats()
}

onMounted(async () => {
  await loadOptions()
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
.emp-cell { display: flex; flex-direction: column; line-height: 1.4; }
.emp-name { font-weight: 600; }
.emp-no { font-size: 12px; color: #909399; }
.score { color: #409eff; margin-right: 6px; }
.eval { color: #606266; }
.muted { color: #c0c4cc; }
.tip { margin-left: 8px; font-size: 12px; color: #b1b3b8; }
</style>
