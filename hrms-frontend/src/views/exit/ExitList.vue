<template>
  <div class="page-container">
    <!-- 概览 -->
    <el-row :gutter="12">
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
              placeholder="姓名 / 工号 / 单据号"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item :label="kindLabel + '类型'">
            <el-select v-model="searchForm.exitType" placeholder="全部类型" clearable style="width: 140px">
              <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 130px">
              <el-option v-for="(s, key) in statusMap" :key="key" :label="s.text" :value="Number(key)" />
            </el-select>
          </el-form-item>
          <el-form-item label="预计生效">
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">发起{{ kindLabel }}</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="exitNo" label="单据号" width="180" show-overflow-tooltip />
        <el-table-column label="员工" min-width="140">
          <template #default="{ row }">
            <div class="emp-cell">
              <span class="emp-name">{{ row.employeeName }}</span>
              <span class="emp-no">{{ row.empNo || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="部门" width="110" show-overflow-tooltip />
        <el-table-column prop="position" label="岗位" width="130" show-overflow-tooltip />
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.exitKind === 2 ? 'danger' : 'warning'" size="small" effect="plain">
              {{ typeText(row.exitType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expectLeaveDate" label="预计生效" width="110" align="center" />
        <el-table-column label="实际生效" width="110" align="center">
          <template #default="{ row }">{{ row.actualLeaveDate || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="isDismiss" label="补偿金额" width="110" align="right">
          <template #default="{ row }">{{ money(row.compensation) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批" width="140">
          <template #default="{ row }">
            <span class="approve-text">{{ row.approveBy || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" type="success" link @click="handleApprove(row)">批准</el-button>
            <el-button v-if="row.status === 1" type="warning" link @click="handleEffect(row)">办理生效</el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleMore(row, cmd)">
              <el-button link>
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status === 0" command="reject">驳回</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 0 || row.status === 1" command="cancel">撤销</el-dropdown-item>
                  <el-dropdown-item v-if="row.status !== 3" command="delete">删除</el-dropdown-item>
                  <el-dropdown-item command="detail">查看详情</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <!-- 发起/编辑单据 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? `编辑${kindLabel}单据` : `发起${kindLabel}`"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="员工" prop="employeeId">
          <el-select
            v-model="form.employeeId"
            filterable
            :disabled="isEdit"
            placeholder="输入姓名或工号搜索"
            style="width: 100%"
          >
            <el-option v-for="e in employeeOptions" :key="e.id" :label="`${e.name} · ${e.empNo || '无工号'} · ${deptNameOf(e.deptId)}`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="kindLabel + '类型'" prop="exitType">
              <el-select v-model="form.exitType" style="width: 100%">
                <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计生效" prop="expectLeaveDate">
              <el-date-picker
                v-model="form.expectLeaveDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工作交接人">
              <el-input v-model="form.handoverTo" placeholder="如 张主管" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="isDismiss">
            <el-form-item label="补偿金额">
              <el-input-number v-model="form.compensation" :min="0" :precision="2" :step="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="kindLabel + '原因'" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" :placeholder="isDismiss ? '请填写辞退依据与沟通情况，作为留档记录' : '请填写离职原因'" />
        </el-form-item>
        <el-alert
          v-if="!isEdit"
          type="info"
          :closable="false"
          show-icon
          title="提交后进入待审批，审批通过后需再点「办理生效」，员工档案才会置为离职并停用登录账号。"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" :title="`${kindLabel}单据详情`" width="620px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="单据号">{{ detail.exitNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[detail.status]?.type || 'info'" size="small">
            {{ statusMap[detail.status]?.text || '未知' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="员工">{{ detail.employeeName }}（{{ detail.empNo || '-' }}）</el-descriptions-item>
        <el-descriptions-item :label="kindLabel + '类型'">{{ typeText(detail.exitType) }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detail.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ detail.position || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发起日期">{{ detail.applyDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计生效">{{ detail.expectLeaveDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际生效">{{ detail.actualLeaveDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="交接人">{{ detail.handoverTo || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="isDismiss" label="补偿金额">{{ money(detail.compensation) }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detail.approveBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批时间" :span="2">{{ detail.approveTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2">{{ detail.approveRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="kindLabel + '原因'" :span="2">{{ detail.reason || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Edit, ArrowDown } from '@element-plus/icons-vue'
import { getEmployeeListApi } from '@/api/employee'
import { getDepartmentListApi } from '@/api/department'
import {
  getExitPageApi,
  getExitStatisticsApi,
  addExitApi,
  updateExitApi,
  approveExitApi,
  rejectExitApi,
  effectExitApi,
  cancelExitApi,
  deleteExitApi
} from '@/api/employeeExit'

// kind: 1 离职、2 辞退（由路由 props 传入，两个页面共用同一套交互）
const props = defineProps({
  kind: { type: Number, default: 1 }
})

const isDismiss = computed(() => props.kind === 2)
const kindLabel = computed(() => (isDismiss.value ? '辞退' : '离职'))

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const detail = ref({})
const employeeOptions = ref([])
const deptMap = ref({})
const stats = ref({})
const dateRange = ref([])

const searchForm = reactive({ keyword: '', exitType: null, status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const typeOptions = computed(() =>
  isDismiss.value
    ? [
        { value: 4, label: '辞退' },
        { value: 5, label: '裁员' },
        { value: 6, label: '开除' }
      ]
    : [
        { value: 1, label: '主动离职' },
        { value: 2, label: '合同到期' },
        { value: 3, label: '退休' }
      ]
)

const statusMap = {
  0: { text: '待审批', type: 'warning' },
  1: { text: '已批准', type: 'primary' },
  2: { text: '已驳回', type: 'danger' },
  3: { text: '已生效', type: 'success' },
  4: { text: '已撤销', type: 'info' }
}

const defaultForm = {
  id: null,
  employeeId: null,
  exitType: null,
  expectLeaveDate: '',
  reason: '',
  handoverTo: '',
  compensation: 0
}
const form = reactive({ ...defaultForm })

const rules = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  exitType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  expectLeaveDate: [{ required: true, message: '请选择预计生效日期', trigger: 'change' }],
  reason: [{ required: true, message: '请填写原因', trigger: 'blur' }]
}

const statCards = computed(() =>
  isDismiss.value
    ? [
        { label: '待审批', value: stats.value.pending ?? 0, sub: '需尽快处理', color: '#E6A23C' },
        { label: '待生效', value: stats.value.approved ?? 0, sub: '已批准未办理', color: '#409EFF' },
        { label: '本月辞退', value: stats.value.dismissedThisMonth ?? 0, sub: '已办理生效', color: '#F56C6C' },
        { label: '本月补偿', value: money(stats.value.compensationThisMonth), sub: '辞退/裁员补偿合计', color: '#909399' }
      ]
    : [
        { label: '待审批', value: stats.value.pending ?? 0, sub: '需尽快处理', color: '#E6A23C' },
        { label: '待生效', value: stats.value.approved ?? 0, sub: '已批准未办理', color: '#409EFF' },
        { label: '本月离职', value: stats.value.resignedThisMonth ?? 0, sub: '已办理生效', color: '#F56C6C' },
        { label: '累计离职', value: stats.value.effective ?? 0, sub: '全部已生效单据', color: '#67C23A' }
      ]
)

const typeText = (type) => {
  const all = [
    { value: 1, label: '主动离职' },
    { value: 2, label: '合同到期' },
    { value: 3, label: '退休' },
    { value: 4, label: '辞退' },
    { value: 5, label: '裁员' },
    { value: 6, label: '开除' }
  ]
  return all.find((t) => t.value === type)?.label || '—'
}

const money = (v) => `¥ ${Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

const loadData = async () => {
  loading.value = true
  try {
    const res = await getExitPageApi({
      ...searchForm,
      exitKind: props.kind,
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
    const res = await getExitStatisticsApi(props.kind)
    stats.value = res.data || {}
  } catch (e) {
    stats.value = {}
  }
}

const loadEmployees = async () => {
  try {
    const res = await getEmployeeListApi()
    // 只允许对在职/试用期员工发起单据，离职员工不再产生新的入离流程
    employeeOptions.value = (res.data || []).filter((e) => e.status !== 0)
  } catch (e) {
    employeeOptions.value = []
  }
}

const loadDepts = async () => {
  try {
    const res = await getDepartmentListApi()
    const map = {}
    ;(res.data || []).forEach((d) => { map[d.id] = d.deptName })
    deptMap.value = map
  } catch (e) {
    deptMap.value = {}
  }
}

const deptNameOf = (id) => (id ? deptMap.value[id] || '未分配' : '未分配')

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.exitType = null
  searchForm.status = null
  dateRange.value = []
  handleSearch()
}

const handleSizeChange = () => {
  pagination.pageNum = 1
  loadData()
}

const resetForm = () => {
  Object.assign(form, defaultForm)
  form.exitType = typeOptions.value[0].value
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
  if (!employeeOptions.value.length) loadEmployees()
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    employeeId: row.employeeId,
    exitType: row.exitType,
    expectLeaveDate: row.expectLeaveDate,
    reason: row.reason || '',
    handoverTo: row.handoverTo || '',
    compensation: Number(row.compensation || 0)
  })
  if (!employeeOptions.value.length) loadEmployees()
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        ...form,
        exitKind: props.kind,
        compensation: isDismiss.value ? form.compensation : 0
      }
      if (isEdit.value) {
        await updateExitApi(payload)
        ElMessage.success('单据已更新')
      } else {
        await addExitApi(payload)
        ElMessage.success('单据已提交，等待审批')
      }
      dialogVisible.value = false
      loadData()
      loadStats()
    } catch (e) {
      ElMessage.error(e?.response?.data?.message || e.message || '提交失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleApprove = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('可填写审批意见（选填）', `批准${kindLabel.value}单据：${row.employeeName}`, {
      confirmButtonText: '批准',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '同意该员工按计划生效'
    })
    await approveExitApi(row.id, { remark: value || '' })
    ElMessage.success('已批准，待办理生效')
    loadData()
    loadStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || e.message || '操作失败')
  }
}

const handleEffect = async (row) => {
  try {
    await ElMessageBox.confirm(
      `生效后：员工「${row.employeeName}」档案将被置为离职、写入离职日期，并停用其登录账号（保留全部历史数据）。是否继续？`,
      '办理生效',
      { type: 'warning', confirmButtonText: '确认生效', cancelButtonText: '取消' }
    )
    await effectExitApi(row.id)
    ElMessage.success('已生效，员工档案与登录账号已同步')
    loadData()
    loadStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || e.message || '操作失败')
  }
}

const handleMore = async (row, cmd) => {
  try {
    if (cmd === 'detail') {
      detail.value = row
      detailVisible.value = true
      return
    }
    if (cmd === 'reject') {
      const { value } = await ElMessageBox.prompt('驳回必须填写原因', '驳回单据', {
        confirmButtonText: '驳回',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputValidator: (v) => (v && v.trim() ? true : '请填写驳回原因')
      })
      await rejectExitApi(row.id, { remark: value })
      ElMessage.success('已驳回')
    }
    if (cmd === 'cancel') {
      await ElMessageBox.confirm('撤销后该单据不再流转，可重新发起。是否继续？', '撤销单据', { type: 'warning' })
      await cancelExitApi(row.id)
      ElMessage.success('单据已撤销')
    }
    if (cmd === 'delete') {
      await ElMessageBox.confirm('删除后不可恢复。是否继续？', '删除单据', { type: 'warning' })
      await deleteExitApi(row.id)
      ElMessage.success('单据已删除')
    }
    loadData()
    loadStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || e.message || '操作失败')
  }
}

loadData()
loadStats()
loadEmployees()
loadDepts()
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
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.emp-cell { display: flex; flex-direction: column; line-height: 1.3; }
.emp-name { font-weight: 600; color: #303133; }
.emp-no { font-size: 12px; color: #909399; }
.approve-text { font-size: 12px; color: #606266; }
</style>
