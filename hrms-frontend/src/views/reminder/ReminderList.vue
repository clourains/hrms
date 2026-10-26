<template>
  <div class="reminder-container page-stack">
    <!-- 页头 -->
    <PageHeader
      title="到期提醒"
      description="扫描合同与签证临期情况，及时安排续签与换证"
      icon="AlarmClock"
    >
      <template #actions>
        <el-badge :value="contractAlerts.length" type="danger" :hidden="contractAlerts.length === 0">
          <el-tag type="info">合同临期</el-tag>
        </el-badge>
        <el-badge :value="visaAlerts.length" type="danger" :hidden="visaAlerts.length === 0">
          <el-tag type="info">签证临期</el-tag>
        </el-badge>
      </template>
    </PageHeader>

    <!-- 筛选区 -->
    <div class="search-card">
      <el-form :inline="true">
        <el-form-item label="提醒范围">
          <el-radio-group v-model="days" size="small" @change="loadAlerts">
            <el-radio-button :label="30">30 天内</el-radio-button>
            <el-radio-button :label="60">60 天内</el-radio-button>
            <el-radio-button :label="90">90 天内</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item class="search-submit">
          <el-button :icon="Refresh" @click="loadAlerts">刷新</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 提醒列表 -->
    <div class="table-card">
      <el-tabs v-model="activeTab">
        <!-- 合同到期 -->
        <el-tab-pane name="contract">
          <template #label>
            合同到期
            <el-badge v-if="contractAlerts.length" :value="contractAlerts.length" style="margin-left: 4px" />
          </template>
          <div class="tab-toolbar">
            <el-button type="primary" :icon="Plus" @click="handleAddContract">新增合同</el-button>
          </div>
          <el-table :data="contractAlerts" border stripe v-loading="loading">
            <el-table-column prop="empName" label="员工" width="100" align="center" />
            <el-table-column prop="deptName" label="部门" width="120" align="center" />
            <el-table-column prop="contractNo" label="合同编号" width="140" align="center" />
            <el-table-column prop="contractType" label="类型" width="110" align="center" />
            <el-table-column prop="startDate" label="开始日期" width="110" align="center" />
            <el-table-column prop="endDate" label="到期日期" width="110" align="center" />
            <el-table-column label="剩余天数" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="urgencyType(row.daysLeft)" size="small">
                  {{ row.daysLeft }} 天
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="center">
              <template #default="{ row }">
                <el-button type="primary" link :icon="Edit" @click="handleEditContract(row)">编辑</el-button>
                <el-button type="danger" link :icon="Delete" @click="handleDeleteContract(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && contractAlerts.length === 0" description="范围内没有临期合同" :image-size="80" />
        </el-tab-pane>

        <!-- 签证到期 -->
        <el-tab-pane name="visa">
          <template #label>
            签证到期
            <el-badge v-if="visaAlerts.length" :value="visaAlerts.length" style="margin-left: 4px" />
          </template>
          <el-table :data="visaAlerts" border stripe v-loading="loading">
            <el-table-column prop="empName" label="员工" width="100" align="center" />
            <el-table-column prop="deptName" label="部门" width="120" align="center" />
            <el-table-column prop="passportNo" label="护照号" width="150" align="center" />
            <el-table-column prop="visaType" label="签证类型" width="120" align="center" />
            <el-table-column prop="visaExpireDate" label="到期日期" width="120" align="center" />
            <el-table-column label="剩余天数" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="urgencyType(row.daysLeft)" size="small">
                  {{ row.daysLeft }} 天
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="办理入口" align="center">
              <template #default="{ row }">
                <el-button type="primary" link @click="goEmployee(row.empId)">去员工管理更新签证</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && visaAlerts.length === 0" description="范围内没有临期签证" :image-size="80" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 合同编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="员工" prop="empId">
          <el-select v-model="formData.empId" filterable placeholder="请选择员工" style="width: 100%">
            <el-option v-for="emp in employeeOptions" :key="emp.id" :label="emp.name + (emp.employeeNo ? '（' + emp.employeeNo + '）' : '')" :value="emp.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同编号" prop="contractNo">
          <el-input v-model="formData.contractNo" placeholder="如 HT-2026-001" />
        </el-form-item>
        <el-form-item label="合同类型" prop="contractType">
          <el-select v-model="formData.contractType" style="width: 100%">
            <el-option label="劳动合同" value="劳动合同" />
            <el-option label="劳务协议" value="劳务协议" />
            <el-option label="保密协议" value="保密协议" />
            <el-option label="竞业协议" value="竞业协议" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="formData.startDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择开始日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="formData.endDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择结束日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getExpiringApi, getContractListApi, addContractApi, updateContractApi, deleteContractApi } from '@/api/contract'
import { getEmployeeListApi } from '@/api/employee'

const router = useRouter()
const loading = ref(false)
const submitLoading = ref(false)
const activeTab = ref('contract')
const days = ref(60)

const contractAlerts = ref([])
const visaAlerts = ref([])
const employeeOptions = ref([])

// 合同编辑
const dialogVisible = ref(false)
const dialogTitle = ref('新增合同')
const editingId = ref(null)
const formRef = ref(null)
const formData = reactive({
  empId: null,
  contractNo: '',
  contractType: '劳动合同',
  startDate: '',
  endDate: '',
  remark: ''
})
const formRules = {
  empId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }]
}

const loadAlerts = async () => {
  loading.value = true
  try {
    const res = await getExpiringApi(days.value)
    if (res.code === 200) {
      contractAlerts.value = res.data.contractAlerts || []
      visaAlerts.value = res.data.visaAlerts || []
    }
  } finally {
    loading.value = false
  }
}

const loadEmployeeOptions = async () => {
  const res = await getEmployeeListApi({})
  if (res.code === 200) {
    employeeOptions.value = res.data || []
  }
}

const urgencyType = (daysLeft) => {
  if (daysLeft <= 7) return 'danger'
  if (daysLeft <= 30) return 'warning'
  return 'primary'
}

const handleAddContract = () => {
  dialogTitle.value = '新增合同'
  editingId.value = null
  Object.assign(formData, { empId: null, contractNo: '', contractType: '劳动合同', startDate: '', endDate: '', remark: '' })
  dialogVisible.value = true
}

const handleEditContract = (row) => {
  dialogTitle.value = '编辑合同'
  editingId.value = row.id
  Object.assign(formData, {
    empId: row.empId,
    contractNo: row.contractNo || '',
    contractType: row.contractType || '劳动合同',
    startDate: row.startDate,
    endDate: row.endDate,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    const api = editingId.value ? updateContractApi : addContractApi
    const payload = { ...formData }
    if (editingId.value) payload.id = editingId.value
    const res = await api(payload)
    if (res.code === 200) {
      ElMessage.success(res.message || '保存成功')
      dialogVisible.value = false
      loadAlerts()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const handleDeleteContract = async (row) => {
  await ElMessageBox.confirm(`确认删除员工「${row.empName}」的合同吗？`, '提示', { type: 'warning' })
  const res = await deleteContractApi(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    loadAlerts()
  } else {
    ElMessage.error(res.message || '删除失败')
  }
}

const goEmployee = (empId) => {
  router.push({ path: '/employee', query: { id: empId } })
}

onMounted(() => {
  loadAlerts()
  loadEmployeeOptions()
})
</script>

<style scoped>
.tab-toolbar {
  margin-bottom: 12px;
}
</style>
