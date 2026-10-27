<template>
  <div class="page-stack">
    <!-- 页头 -->
    <PageHeader
      title="用户管理"
      description="管理系统登录账号：建号、分配角色、绑定员工、启停用与密码重置"
      icon="User"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增用户</el-button>
      </template>
    </PageHeader>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="用户名 / 昵称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="全部角色" clearable style="width: 140px">
            <el-option label="管理员" value="admin" />
            <el-option label="部门经理" value="manager" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-submit">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 用户表格 -->
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="nickname" label="昵称" min-width="120">
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="empId" label="绑定员工" width="120" align="center">
          <template #default="{ row }">{{ empNameMap[row.empId] || (row.empId ? 'ID:' + row.empId : '-') }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination page-pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="登录账号，创建后不可改" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="留空默认 123456" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="展示用名称" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="部门经理" value="manager" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定员工">
          <el-select v-model="form.empId" clearable filterable placeholder="不绑定" style="width: 100%">
            <el-option v-for="emp in empOptions" :key="emp.id" :label="emp.name" :value="emp.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getUserListApi, createUserApi, updateUserApi, deleteUserApi, resetUserPasswordApi } from '@/api/user'
import { getEmployeeListApi } from '@/api/employee'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const loading = ref(false)
const tableData = ref([])
const empOptions = ref([])
const empNameMap = ref({})
const searchForm = reactive({ keyword: '', role: '' })
const pagination = reactive({ current: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, username: '', password: '', nickname: '', role: 'user', empId: null, status: 1 })

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const roleLabel = (role) => ({ admin: '管理员', manager: '部门经理', user: '普通用户' }[role] || role)
const roleTagType = (role) => ({ admin: 'danger', manager: 'warning', user: 'info' }[role] || 'info')

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: pagination.current, size: pagination.size }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.role) params.role = searchForm.role
    const res = await getUserListApi(params)
    if (res.code === 200) {
      tableData.value = res.data.records || []
      pagination.total = Number(res.data.total) || 0
    }
  } finally {
    loading.value = false
  }
}

const loadEmpOptions = async () => {
  const res = await getEmployeeListApi()
  if (res.code === 200) {
    empOptions.value = res.data || []
    const map = {}
    for (const e of empOptions.value) map[e.id] = e.name
    empNameMap.value = map
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.role = ''
  handleSearch()
}

const openCreateDialog = () => {
  isEdit.value = false
  Object.assign(form, { id: null, username: '', password: '', nickname: '', role: 'user', empId: null, status: 1 })
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    nickname: row.nickname || '',
    role: row.role,
    empId: row.empId,
    status: row.status
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const res = isEdit.value ? await updateUserApi(form) : await createUserApi(form)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '用户已更新' : '用户创建成功')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row) => {
  const target = row.status === 1 ? 0 : 1
  if (target === 0 && row.username === userStore.username) {
    ElMessage.warning('不能禁用当前登录的账号')
    return
  }
  const res = await updateUserApi({ id: row.id, status: target })
  if (res.code === 200) {
    ElMessage.success(target === 1 ? '已启用' : '已禁用')
    loadData()
  } else {
    ElMessage.error(res.message || '操作失败')
  }
}

const handleResetPassword = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `为用户「${row.username}」设置新密码，留空则重置为 123456`,
      '重置密码',
      { confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '新密码（可留空）', inputValue: '' }
    )
    const res = await resetUserPasswordApi(row.id, value || undefined)
    if (res.code === 200) {
      ElMessage.success('密码已重置')
    } else {
      ElMessage.error(res.message || '重置失败')
    }
  } catch {
    // 用户取消
  }
}

const handleDelete = async (row) => {
  if (row.username === userStore.username) {
    ElMessage.warning('不能删除当前登录的账号')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？删除后无法登录。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    const res = await deleteUserApi(row.id)
    if (res.code === 200) {
      ElMessage.success('用户已删除')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadData()
  loadEmpOptions()
})
</script>
