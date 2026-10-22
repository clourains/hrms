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

const tableData = ref([
  { id: 1, employeeNo: 'E001', name: '张三', avatar: '', gender: 1, departmentId: 1, departmentName: '技术部', position: '高级工程师', phone: '13800138001', email: 'zhangsan@example.com', entryDate: '2022-01-15', status: 1, birthday: '1990-05-20', address: '北京市朝阳区', remark: '' },
  { id: 2, employeeNo: 'E002', name: '李四', avatar: '', gender: 2, departmentId: 1, departmentName: '技术部', position: '前端工程师', phone: '13800138002', email: 'lisi@example.com', entryDate: '2022-03-20', status: 1, birthday: '1992-08-15', address: '北京市海淀区', remark: '' },
  { id: 3, employeeNo: 'E003', name: '王五', avatar: '', gender: 1, departmentId: 2, departmentName: '产品部', position: '产品经理', phone: '13800138003', email: 'wangwu@example.com', entryDate: '2021-06-10', status: 1, birthday: '1988-12-03', address: '北京市西城区', remark: '' },
  { id: 4, employeeNo: 'E004', name: '赵六', avatar: '', gender: 1, departmentId: 3, departmentName: '市场部', position: '市场专员', phone: '13800138004', email: 'zhaoliu@example.com', entryDate: '2023-02-01', status: 2, birthday: '1995-07-22', address: '北京市东城区', remark: '试用期' },
  { id: 5, employeeNo: 'E005', name: '钱七', avatar: '', gender: 2, departmentId: 4, departmentName: '运营部', position: '运营主管', phone: '13800138005', email: 'qianqi@example.com', entryDate: '2020-09-01', status: 1, birthday: '1991-04-18', address: '北京市丰台区', remark: '' },
  { id: 6, employeeNo: 'E006', name: '孙八', avatar: '', gender: 1, departmentId: 5, departmentName: '人事部', position: 'HR专员', phone: '13800138006', email: 'sunba@example.com', entryDate: '2022-08-15', status: 1, birthday: '1994-11-30', address: '北京市石景山区', remark: '' },
  { id: 7, employeeNo: 'E007', name: '周九', avatar: '', gender: 2, departmentId: 6, departmentName: '财务部', position: '会计', phone: '13800138007', email: 'zhoujiu@example.com', entryDate: '2021-04-20', status: 1, birthday: '1990-02-14', address: '北京市通州区', remark: '' },
  { id: 8, employeeNo: 'E008', name: '吴十', avatar: '', gender: 1, departmentId: 7, departmentName: '行政部', position: '行政专员', phone: '13800138008', email: 'wushi@example.com', entryDate: '2023-01-10', status: 2, birthday: '1996-06-05', address: '北京市顺义区', remark: '试用期' },
  { id: 9, employeeNo: 'E009', name: '郑十一', avatar: '', gender: 1, departmentId: 1, departmentName: '技术部', position: '后端工程师', phone: '13800138009', email: 'zheng11@example.com', entryDate: '2022-05-18', status: 1, birthday: '1993-09-12', address: '北京市昌平区', remark: '' },
  { id: 10, employeeNo: 'E010', name: '冯十二', avatar: '', gender: 2, departmentId: 8, departmentName: '销售部', position: '销售经理', phone: '13800138010', email: 'feng12@example.com', entryDate: '2019-11-01', status: 0, birthday: '1987-03-25', address: '北京市大兴区', remark: '已离职' }
])

pagination.total = tableData.value.length

const departmentOptions = ref([
  { id: 1, name: '技术部' },
  { id: 2, name: '产品部' },
  { id: 3, name: '市场部' },
  { id: 4, name: '运营部' },
  { id: 5, name: '人事部' },
  { id: 6, name: '财务部' },
  { id: 7, name: '行政部' },
  { id: 8, name: '销售部' }
])

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

const handleSearch = () => {
  pagination.currentPage = 1
  ElMessage.success('搜索功能演示')
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  pagination.currentPage = 1
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
  }).then(() => {
    const index = tableData.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      tableData.value.splice(index, 1)
      pagination.total--
    }
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const handleBatchDelete = () => {
  ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条记录吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    tableData.value = tableData.value.filter(item => !selectedIds.value.includes(item.id))
    pagination.total = tableData.value.length
    selectedIds.value = []
    ElMessage.success('批量删除成功')
  }).catch(() => {})
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (valid) {
      submitLoading.value = true
      setTimeout(() => {
        if (formData.id) {
          // 编辑
          const index = tableData.value.findIndex(item => item.id === formData.id)
          if (index > -1) {
            const dept = departmentOptions.value.find(d => d.id === formData.departmentId)
            formData.departmentName = dept?.name || ''
            tableData.value[index] = { ...formData }
          }
          ElMessage.success('编辑成功')
        } else {
          // 新增
          const dept = departmentOptions.value.find(d => d.id === formData.departmentId)
          const newEmployee = {
            ...formData,
            id: Date.now(),
            departmentName: dept?.name || ''
          }
          tableData.value.unshift(newEmployee)
          pagination.total++
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        submitLoading.value = false
      }, 500)
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
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
}

onMounted(() => {
  // 实际项目中调用接口获取数据
  // fetchEmployeeList()
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
