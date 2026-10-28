<template>
  <div class="page-container">
    <!-- 登记概览 -->
    <el-row :gutter="12">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.label">
        <div class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-sub">{{ card.sub }}</div>
        </div>
      </el-col>
    </el-row>

    <el-alert
      class="flow-alert"
      type="info"
      :closable="false"
      show-icon
      title="入职登记流程：登记新员工资料 → 关联员工档案（关联已有 / 一键新建档案并生成试用期）→ 转正；离职与辞退在「离职管理」「辞退管理」中办理。"
    />

    <el-card shadow="hover">
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="关键字">
            <el-input
              v-model="searchForm.keyword"
              placeholder="姓名 / 岗位 / 登记单号"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="档案关联">
            <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
              <el-option label="待关联" :value="0" />
              <el-option label="已关联" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="部门">
            <el-select v-model="searchForm.deptId" placeholder="全部部门" clearable filterable style="width: 160px">
              <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="入职日期">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 250px"
              @change="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <div class="head-actions">
          <el-button :icon="UserFilled" @click="goEmployee">员工档案</el-button>
          <el-button type="primary" :icon="Plus" @click="handleAdd">入职登记</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="onboardNo" label="登记单号" width="170" show-overflow-tooltip />
        <el-table-column label="姓名" width="130">
          <template #default="{ row }">
            <div class="emp-cell">
              <span class="emp-name">{{ row.name }}</span>
              <span class="emp-no">{{ row.phone || '未填手机号' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="部门" width="110" show-overflow-tooltip />
        <el-table-column prop="position" label="岗位" min-width="130" show-overflow-tooltip />
        <el-table-column prop="entryDate" label="入职日期" width="110" align="center" />
        <el-table-column label="试用期" width="90" align="center">
          <template #default="{ row }">{{ row.probationMonths || 3 }} 个月</template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="105" align="center">
          <template #default="{ row }">{{ row.source || '—' }}</template>
        </el-table-column>
        <el-table-column label="员工档案" width="160" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">
              已关联{{ empNoOf(row.employeeId) ? ' · ' + empNoOf(row.employeeId) : '' }}
            </el-tag>
            <el-tag v-else type="warning" size="small">待关联</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status !== 1">
              <el-button type="primary" link @click="openLink(row)">关联档案</el-button>
              <el-button type="success" link @click="handleCreateProfile(row)">新建档案</el-button>
              <el-button link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            </template>
            <el-button v-else type="primary" link :icon="UserFilled" @click="openProfile(row)">查看档案</el-button>
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

    <!-- 登记 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑入职登记' : '入职登记'" width="660px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio :label="1">男</el-radio>
                <el-radio :label="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="建档时需要" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源">
              <el-select v-model="form.source" placeholder="请选择" style="width: 100%">
                <el-option label="招聘录用" value="招聘录用" />
                <el-option label="内推" value="内推" />
                <el-option label="校招" value="校招" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择部门" filterable style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位" prop="position">
              <el-input v-model="form.position" placeholder="如 外贸业务员" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="入职日期" prop="entryDate">
              <el-date-picker v-model="form.entryDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="试用期">
              <el-input-number v-model="form.probationMonths" :min="1" :max="12" style="width: 100%" />
              <span class="unit-hint">个月</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="资料收集情况、特殊说明等" />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="登记后档案仍是「待关联」，可再选择关联已有档案或一键新建档案；新建档案会自动生成员工工号与试用期记录。"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 关联已有档案 -->
    <el-dialog v-model="linkVisible" title="关联已有员工档案" width="560px" destroy-on-close>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="适合信息已在员工管理中维护的老员工/补录场景；同一档案不能重复关联多条入职登记。"
        class="link-alert"
      />
      <el-form label-width="80px">
        <el-form-item label="员工档案">
          <el-select v-model="linkEmployeeId" filterable placeholder="输入姓名或工号搜索" style="width: 100%">
            <el-option
              v-for="e in linkableEmployees"
              :key="e.id"
              :label="`${e.name} · ${e.empNo || '无工号'} · ${e.position || '无岗位'}`"
              :value="e.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="link-tip">
        没有对应档案？关闭本弹窗，点「新建档案」按登记信息自动建档。
      </div>
      <template #footer>
        <el-button @click="linkVisible = false">取消</el-button>
        <el-button type="primary" :loading="linking" @click="handleLink">确认关联</el-button>
      </template>
    </el-dialog>

    <!-- 关联的员工档案详情 -->
    <el-dialog v-model="detailVisible" title="员工档案" width="680px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工号">{{ detail.empNo || detail.employeeNo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ detail.gender === 1 ? '男' : detail.gender === 2 ? '女' : '—' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="empStatusOf(detail.status).type" size="small">{{ empStatusOf(detail.status).text }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="部门">{{ detailDeptName }}</el-descriptions-item>
            <el-descriptions-item label="岗位">{{ detail.position || '—' }}</el-descriptions-item>
            <el-descriptions-item label="入职日期">{{ detail.entryDate || '—' }}</el-descriptions-item>
            <el-descriptions-item label="离职日期">{{ detail.leaveDate || '—' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ detail.phone || '—' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
            <el-descriptions-item label="出生日期">{{ detail.birthday || '—' }}</el-descriptions-item>
            <el-descriptions-item label="学历">{{ detail.education || '—' }}</el-descriptions-item>
            <el-descriptions-item label="关联登记单号" :span="2">{{ detailOnboardNo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="登记来源" :span="2">{{ detailSource || '—' }}</el-descriptions-item>
            <el-descriptions-item label="地址" :span="2">{{ detail.address || '—' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <el-empty v-else-if="!detailLoading" description="未找到该员工档案，可能已被删除" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" :icon="UserFilled" @click="goEmployee">打开员工管理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, UserFilled, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getEmployeeListApi, getEmployeeDetailApi } from '@/api/employee'
import { getDepartmentListApi } from '@/api/department'
import {
  getOnboardPageApi,
  getOnboardStatisticsApi,
  addOnboardApi,
  updateOnboardApi,
  linkOnboardApi,
  createProfileApi,
  deleteOnboardApi
} from '@/api/employeeOnboard'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const linking = ref(false)
const tableData = ref([])
const stats = ref({})
const deptOptions = ref([])
const employees = ref([])
const employeeMap = ref({})
const dateRange = ref([])
const dialogVisible = ref(false)
const linkVisible = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const detailRow = ref(null)
const isEdit = ref(false)
const formRef = ref(null)
const linkEmployeeId = ref(null)
const currentRow = ref(null)

const searchForm = reactive({ keyword: '', status: null, deptId: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const defaultForm = {
  id: null,
  name: '',
  gender: 1,
  phone: '',
  deptId: null,
  position: '',
  entryDate: '',
  probationMonths: 3,
  source: '招聘录用',
  remark: ''
}
const form = reactive({ ...defaultForm })

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  position: [{ required: true, message: '请输入岗位', trigger: 'blur' }],
  entryDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }]
}

const statCards = computed(() => [
  { label: '本月登记', value: stats.value.thisMonth ?? 0, sub: '入职日期在本月', color: '#409EFF' },
  { label: '待关联档案', value: stats.value.pending ?? 0, sub: '还未落员工档案', color: '#E6A23C' },
  { label: '已关联档案', value: stats.value.linked ?? 0, sub: '已建立员工档案', color: '#67C23A' },
  { label: '待入职', value: stats.value.upcoming ?? 0, sub: '入职日期在今天之后', color: '#F56C6C' }
])

const linkableEmployees = computed(() => {
  const linked = new Set(tableData.value.filter((r) => r.status === 1 && r.employeeId).map((r) => r.employeeId))
  return employees.value.filter((e) => e.status !== 0 && !linked.has(e.id))
})

const empNoOf = (employeeId) => employeeMap.value[employeeId]?.empNo || ''

const empStatusOf = (status) => {
  if (status === 0) return { text: '离职', type: 'info' }
  if (status === 2) return { text: '试用期', type: 'warning' }
  return { text: '在职', type: 'success' }
}

// 档案里的部门名优先用档案自带的，其次用登记时的部门快照
const detailDeptName = computed(() => {
  if (!detail.value) return '—'
  return detail.value.deptName || deptMap.value[detail.value.deptId] || detailRow.value?.deptName || '—'
})

const deptMap = computed(() => {
  const map = {}
  deptOptions.value.forEach((d) => { map[d.id] = d.deptName })
  return map
})

const detailOnboardNo = computed(() => detailRow.value?.onboardNo || '')
const detailSource = computed(() => detailRow.value?.source || '')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getOnboardPageApi({
      ...searchForm,
      startDate: dateRange.value?.[0] || null,
      endDate: dateRange.value?.[1] || null,
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
  try {
    const res = await getOnboardStatisticsApi()
    stats.value = res.data || {}
  } catch (e) {
    stats.value = {}
  }
}

const loadBaseData = async () => {
  try {
    const [deptRes, empRes] = await Promise.all([getDepartmentListApi(), getEmployeeListApi()])
    deptOptions.value = deptRes.data || []
    employees.value = empRes.data || []
    const map = {}
    employees.value.forEach((e) => { map[e.id] = e })
    employeeMap.value = map
  } catch (e) {
    deptOptions.value = []
    employees.value = []
    employeeMap.value = {}
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = null
  searchForm.deptId = null
  dateRange.value = []
  handleSearch()
}

const handleSizeChange = () => {
  pagination.pageNum = 1
  loadData()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, defaultForm)
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    gender: row.gender || 1,
    phone: row.phone || '',
    deptId: row.deptId,
    position: row.position,
    entryDate: row.entryDate,
    probationMonths: row.probationMonths || 3,
    source: row.source || '招聘录用',
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await updateOnboardApi(form)
        ElMessage.success('登记信息已更新')
      } else {
        await addOnboardApi(form)
        ElMessage.success('登记成功，可继续关联员工档案')
      }
      dialogVisible.value = false
      loadData()
      loadStats()
    } catch (e) {
      ElMessage.error(e?.response?.data?.message || e.message || '保存失败')
    } finally {
      submitting.value = false
    }
  })
}

const openLink = (row) => {
  currentRow.value = row
  linkEmployeeId.value = null
  linkVisible.value = true
}

const handleLink = async () => {
  if (!linkEmployeeId.value) {
    ElMessage.warning('请选择要关联的员工档案')
    return
  }
  linking.value = true
  try {
    await linkOnboardApi(currentRow.value.id, linkEmployeeId.value)
    ElMessage.success('已关联员工档案')
    linkVisible.value = false
    loadData()
    loadStats()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '关联失败')
  } finally {
    linking.value = false
  }
}

const handleCreateProfile = async (row) => {
  if (!row.gender || !row.phone) {
    ElMessage.warning('新建档案需要先补全性别和手机号，请点「编辑」完善后再建档')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将按登记信息为「${row.name}」生成员工档案（工号自动生成），并自动建立 ${row.probationMonths || 3} 个月试用期记录。是否继续？`,
      '新建员工档案',
      { type: 'warning', confirmButtonText: '确认建档', cancelButtonText: '取消' }
    )
    const res = await createProfileApi(row.id)
    const empNo = empNoOf(res.data.employeeId) || ''
    ElMessage.success(`员工档案已生成${empNo ? '（工号 ' + empNo + '）' : ''}，并已建立试用期`)
    loadData()
    loadStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || e.message || '建档失败')
  }
}

const handleDelete = async (row) => {
  try {
    const tip = row.status === 1
      ? '该登记已关联员工档案，删除只会移除登记台账，不会删除员工档案。是否继续？'
      : '删除后不可恢复。是否继续？'
    await ElMessageBox.confirm(tip, '删除入职登记', { type: 'warning' })
    await deleteOnboardApi(row.id)
    ElMessage.success('登记已删除')
    loadData()
    loadStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || e.message || '删除失败')
  }
}

const goEmployee = () => router.push('/employee')

// 查看具体某个员工档案，而不是只跳到员工列表
const openProfile = async (row) => {
  if (!row.employeeId) return
  detailRow.value = row
  detail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getEmployeeDetailApi(row.employeeId)
    detail.value = res.data || null
  } catch (e) {
    detail.value = null
    ElMessage.error(e?.response?.data?.message || e.message || '员工档案加载失败')
  } finally {
    detailLoading.value = false
  }
}

loadBaseData()
loadData()
loadStats()
</script>

<style scoped>
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
.flow-alert { margin-bottom: 12px; }
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; }
.head-actions { display: flex; gap: 8px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.emp-cell { display: flex; flex-direction: column; line-height: 1.3; }
.emp-name { font-weight: 600; color: #303133; }
.emp-no { font-size: 12px; color: #909399; }
.unit-hint { margin-left: 8px; color: #909399; font-size: 12px; }
.link-alert { margin-bottom: 12px; }
.link-tip { font-size: 12px; color: #909399; padding-left: 80px; }
</style>
