<template>
  <div class="employee-container">
    <el-card shadow="hover">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="姓名">
            <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="工号">
            <el-input v-model="searchForm.employeeNo" placeholder="请输入工号" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="部门">
            <el-select v-model="searchForm.departmentId" placeholder="请选择部门" clearable style="width: 180px">
              <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="职位">
            <el-input v-model="searchForm.position" placeholder="请输入职位" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 140px">
              <el-option label="在职" value="1" />
              <el-option label="离职" value="0" />
              <el-option label="试用期" value="2" />
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增员工</el-button>
        <el-button type="success" :icon="Upload" @click="handleImport">导入</el-button>
        <el-button type="warning" :icon="Download" @click="handleExport">导出</el-button>
        <el-button type="danger" :icon="Delete" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="employeeNo" label="工号" width="100" align="center" />
        <el-table-column prop="name" label="姓名" width="100" align="center" />
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatar">
              {{ row.name ? row.name.charAt(0) : '' }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="70" align="center">
          <template #default="{ row }">
            {{ row.gender === 1 ? '男' : '女' }}
          </template>
        </el-table-column>
        <el-table-column prop="departmentName" label="部门" width="120" align="center" />
        <el-table-column prop="position" label="职位" width="120" align="center" />
        <el-table-column prop="phone" label="手机号" width="130" align="center" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="entryDate" label="入职日期" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="handleView(row)">查看</el-button>
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工号" prop="employeeNo">
              <el-input v-model="formData.employeeNo" placeholder="请输入工号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="formData.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="formData.gender">
                <el-radio :label="1">男</el-radio>
                <el-radio :label="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthday">
              <el-date-picker
                v-model="formData.birthday"
                type="date"
                placeholder="请选择出生日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门" prop="departmentId">
              <el-select v-model="formData.departmentId" placeholder="请选择部门" style="width: 100%">
                <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位" prop="position">
              <el-input v-model="formData.position" placeholder="请输入职位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职日期" prop="entryDate">
              <el-date-picker
                v-model="formData.entryDate"
                type="date"
                placeholder="请选择入职日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="formData.status" placeholder="请选择状态" style="width: 100%">
                <el-option label="在职" :value="1" />
                <el-option label="离职" :value="0" />
                <el-option label="试用期" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="formData.status === 0" :span="12">
            <el-form-item label="离职日期" prop="leaveDate">
              <el-date-picker v-model="formData.leaveDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择离职日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址" prop="address">
              <el-input v-model="formData.address" placeholder="请输入地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="员工详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工号">{{ formData.employeeNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ formData.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ formData.gender === 1 ? '男' : '女' }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ formData.birthday }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ formData.departmentName }}</el-descriptions-item>
        <el-descriptions-item label="职位">{{ formData.position }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ formData.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ formData.email }}</el-descriptions-item>
        <el-descriptions-item label="入职日期">{{ formData.entryDate }}</el-descriptions-item>
        <el-descriptions-item v-if="formData.leaveDate" label="离职日期">{{ formData.leaveDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(formData.status)" size="small">
            {{ getStatusText(formData.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ formData.address }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ formData.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Upload, Download, Delete, View, Edit } from '@element-plus/icons-vue'
import { getEmployeePageApi, addEmployeeApi, updateEmployeeApi, deleteEmployeeApi, batchDeleteEmployeeApi } from '@/api/employee'
import { getDepartmentListApi } from '@/api/department'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const selectedIds = ref([])

const searchForm = reactive({
  name: '',
  employeeNo: '',
  departmentId: '',
  position: '',
  status: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// P042：员工列表改为真实接口分页加载，mock 数据已移除
const tableData = ref([])

const departmentOptions = ref([])
let deptNameMap = {}

const formData = reactive({
  id: null,
  employeeNo: '',
  name: '',
  gender: 1,
  birthday: '',
  departmentId: '',
  departmentName: '',
  position: '',
  phone: '',
  email: '',
  entryDate: '',
  leaveDate: '',
  status: 1,
  address: '',
  remark: ''
})

const formRules = {
  employeeNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  departmentId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  position: [{ required: true, message: '请输入职位', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  entryDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const getStatusText = (status) => {
  const map = { 0: '离职', 1: '在职', 2: '试用期' }
  return map[status] || '未知'
}

const getStatusTagType = (status) => {
  const map = { 0: 'info', 1: 'success', 2: 'warning' }
  return map[status] || 'info'
}

/** 日期格式化：Date 对象或 ISO 字符串统一为 yyyy-MM-dd（后端 LocalDate 格式） */
const fmtDate = (value) => {
  if (!value) return null
  if (typeof value === 'string') return value.slice(0, 10)
  const d = new Date(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 后端实体字段 empNo/deptId 与页面展示字段 employeeNo/departmentName 做归一化 */
const normalizeEmployee = (emp) => ({
  id: emp.id,
  employeeNo: emp.empNo,
  name: emp.name,
  avatar: emp.avatar || '',
  gender: emp.gender,
  departmentId: emp.deptId,
  departmentName: deptNameMap[emp.deptId] || '-',
  position: emp.position,
  phone: emp.phone,
  email: emp.email,
  entryDate: fmtDate(emp.entryDate) || '',
  leaveDate: fmtDate(emp.leaveDate) || '',
  status: emp.status,
  birthday: fmtDate(emp.birthday) || '',
  address: emp.address || '',
  remark: ''
})

const loadDepartments = async () => {
  try {
    const res = await getDepartmentListApi()
    const depts = res.data || []
    departmentOptions.value = depts.map(d => ({ id: d.id, name: d.deptName }))
    deptNameMap = {}
    depts.forEach(d => { deptNameMap[d.id] = d.deptName })
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    // 后端 EmployeeQueryDTO 仅支持 name/deptId/position/status 条件
    const payload = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize
    }
    if (searchForm.name) payload.name = searchForm.name
    if (searchForm.departmentId) payload.deptId = searchForm.departmentId
    if (searchForm.position) payload.position = searchForm.position
    if (searchForm.status !== '') payload.status = Number(searchForm.status)
    const res = await getEmployeePageApi(payload)
    const page = res.data || {}
    tableData.value = (page.list || []).map(normalizeEmployee)
    pagination.total = page.total || 0
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  fetchList()
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  pagination.currentPage = 1
  fetchList()
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleAdd = () => {
  dialogTitle.value = '新增员工'
  Object.keys(formData).forEach(key => {
    formData[key] = key === 'gender' ? 1 : key === 'status' ? 1 : ''
  })
  formData.id = null
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑员工'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleView = (row) => {
  Object.assign(formData, row)
  detailVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除员工 "${row.name}" 吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteEmployeeApi(row.id)
      ElMessage.success('删除成功')
      await fetchList()
    } catch (e) {
      // 错误提示由 request 拦截器统一处理
    }
  }).catch(() => {})
}

const handleBatchDelete = () => {
  ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条记录吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await batchDeleteEmployeeApi(selectedIds.value)
      ElMessage.success('批量删除成功')
      selectedIds.value = []
      await fetchList()
    } catch (e) {
      // 错误提示由 request 拦截器统一处理
    }
  }).catch(() => {})
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      // 映射回后端字段（empNo/deptId）；备注为纯前端展示字段，后端无对应字段不上送
      const payload = {
        empNo: formData.employeeNo,
        name: formData.name,
        gender: formData.gender,
        birthday: fmtDate(formData.birthday),
        deptId: formData.departmentId,
        position: formData.position,
        phone: formData.phone,
        email: formData.email,
        entryDate: fmtDate(formData.entryDate),
        // 离职日期仅离职状态时上送（M1-5 入离职趋势统计来源）
        leaveDate: formData.status === 0 ? fmtDate(formData.leaveDate) : null,
        status: formData.status,
        address: formData.address
      }
      if (formData.id) {
        payload.id = formData.id
        await updateEmployeeApi(payload)
        ElMessage.success('编辑成功')
      } else {
        await addEmployeeApi(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      await fetchList()
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

const handleSizeChange = (size) => {
  pagination.pageSize = size
  fetchList()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  fetchList()
}

onMounted(async () => {
  await loadDepartments()
  fetchList()
})
</script>

<style scoped lang="scss">
.employee-container {
  .search-bar {
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid #ebeef5;
  }

  .action-bar {
    margin-bottom: 16px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
