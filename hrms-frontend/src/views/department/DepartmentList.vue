<template>
  <div class="department-container page-stack">
    <!-- 页头 -->
    <PageHeader
      title="部门管理"
      description="维护组织架构与部门信息，支持多级部门树管理"
      icon="OfficeBuilding"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="handleAddRoot">新增根部门</el-button>
        <el-button :icon="Refresh" @click="refreshData">刷新</el-button>
      </template>
    </PageHeader>

    <el-row :gutter="20">
      <!-- 左侧部门树 -->
      <el-col :span="8">
        <el-card shadow="hover" class="tree-card">
          <template #header>
            <div class="card-header">
              <span>部门结构</span>
            </div>
          </template>
          <el-input
            v-model="filterText"
            placeholder="搜索部门"
            :prefix-icon="Search"
            clearable
            class="filter-input"
          />
          <el-tree
            ref="treeRef"
            :data="departmentTree"
            :props="treeProps"
            node-key="id"
            :filter-node-method="filterNode"
            :expand-on-click-node="false"
            :default-expanded-keys="defaultExpandedKeys"
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <span class="node-label">{{ node.label }}</span>
                <span class="node-count">({{ data.employeeCount || 0 }}人)</span>
                <span class="node-actions">
                  <el-tooltip content="新增子部门" placement="top">
                    <el-icon @click.stop="handleAddChild(data)"><Plus /></el-icon>
                  </el-tooltip>
                  <el-tooltip content="编辑" placement="top">
                    <el-icon @click.stop="handleEdit(data)"><Edit /></el-icon>
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top">
                    <el-icon @click.stop="handleDelete(data)" class="delete-icon"><Delete /></el-icon>
                  </el-tooltip>
                </span>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧部门详情 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>{{ currentDept?.name || '部门详情' }}</span>
            </div>
          </template>

          <div v-if="currentDept" class="dept-detail">
            <!-- 基本信息 -->
            <el-descriptions title="基本信息" :column="2" border class="mb-20">
              <el-descriptions-item label="部门名称">{{ currentDept.name }}</el-descriptions-item>
              <el-descriptions-item label="部门编码">{{ currentDept.code || '-' }}</el-descriptions-item>
              <el-descriptions-item label="上级部门">{{ parentDeptName }}</el-descriptions-item>
              <el-descriptions-item label="部门负责人">{{ currentDept.leader || '-' }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ currentDept.phone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ currentDept.email || '-' }}</el-descriptions-item>
              <el-descriptions-item label="员工人数">{{ currentDept.employeeCount || 0 }} 人</el-descriptions-item>
              <el-descriptions-item label="排序">{{ currentDept.sort || 0 }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ currentDept.createTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="currentDept.status === 1 ? 'success' : 'info'" size="small">
                  {{ currentDept.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>

            <!-- 部门员工列表 -->
            <div class="dept-employees">
              <div class="section-title">
                <span>部门员工</span>
                <el-tag type="info" size="small">共 {{ deptEmployees.length }} 人</el-tag>
              </div>
              <el-table :data="deptEmployees" border stripe size="small">
                <el-table-column prop="employeeNo" label="工号" width="100" align="center" />
                <el-table-column prop="name" label="姓名" width="100" align="center" />
                <el-table-column prop="position" label="职位" width="120" align="center" />
                <el-table-column prop="phone" label="手机号" width="130" align="center" />
                <el-table-column prop="entryDate" label="入职日期" width="120" align="center" />
                <el-table-column prop="status" label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
                      {{ row.status === 1 ? '在职' : '试用期' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <el-empty v-else description="请选择左侧部门查看详情" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="formData.parentId"
            :data="departmentTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            placeholder="请选择上级部门（不选为根部门）"
            style="width: 100%"
            clearable
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门编码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入部门编码" />
        </el-form-item>
        <el-form-item label="部门负责人" prop="leader">
          <el-input v-model="formData.leader" placeholder="请输入部门负责人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
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
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getDepartmentTreeApi, addDepartmentApi, updateDepartmentApi, deleteDepartmentApi } from '@/api/department'
import { getEmployeeListApi } from '@/api/employee'

const treeRef = ref(null)
const filterText = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref(null)
const currentDept = ref(null)

const treeProps = {
  children: 'children',
  label: 'name'
}

const defaultExpandedKeys = ref([])

// P042：部门数据改为真实接口加载，mock 数据已移除
const departmentTree = ref([])
const deptEmployees = ref([])

// 全量员工缓存：用于统计部门人数和展示部门员工列表
let rawEmployees = []

/**
 * 后端部门实体字段为 deptName/deptCode，页面内部统一用 name/code，
 * 加载时归一化，提交时再映射回后端字段
 */
const normalizeDept = (dept) => ({
  id: dept.id,
  name: dept.deptName,
  code: dept.deptCode,
  parentId: dept.parentId,
  leader: dept.leader,
  phone: dept.phone,
  email: dept.email,
  sort: dept.sort,
  status: dept.status,
  createTime: dept.createTime ? String(dept.createTime).slice(0, 10) : '',
  employeeCount: 0,
  children: []
})

/** 按 parentId 把扁平部门列表组装成树 */
const buildTree = (nodes, parentId) =>
  nodes
    .filter(node => node.parentId === parentId)
    .map(node => ({ ...node, children: buildTree(nodes, node.id) }))

const findNode = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findNode(node.children, id)
      if (found) return found
    }
  }
  return null
}

/** 部门员工列表：从全量员工里按部门过滤 */
const loadDeptEmployees = (deptId) => {
  deptEmployees.value = rawEmployees
    .filter(emp => emp.deptId === deptId)
    .map(emp => ({
      employeeNo: emp.empNo,
      name: emp.name,
      position: emp.position,
      phone: emp.phone,
      entryDate: emp.entryDate ? String(emp.entryDate).slice(0, 10) : '',
      status: emp.status
    }))
}

/** 加载部门树 + 员工人数统计；keepId 用于刷新后保持选中节点 */
const loadTree = async (keepId) => {
  try {
    const [deptRes, empRes] = await Promise.all([
      getDepartmentTreeApi(),
      getEmployeeListApi()
    ])
    rawEmployees = empRes.data || []
    const depts = (deptRes.data || []).map(normalizeDept)
    const countByDept = {}
    rawEmployees.forEach(emp => {
      if (emp.deptId != null) {
        countByDept[emp.deptId] = (countByDept[emp.deptId] || 0) + 1
      }
    })
    depts.forEach(dept => {
      dept.employeeCount = countByDept[dept.id] || 0
    })
    departmentTree.value = buildTree(depts, 0)
    const target = keepId ? findNode(departmentTree.value, keepId) : null
    currentDept.value = target || departmentTree.value[0] || null
    if (currentDept.value) {
      defaultExpandedKeys.value = [currentDept.value.id]
      loadDeptEmployees(currentDept.value.id)
    } else {
      deptEmployees.value = []
    }
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

const formData = reactive({
  id: null,
  parentId: null,
  name: '',
  code: '',
  leader: '',
  phone: '',
  email: '',
  sort: 0,
  status: 1
})

const formRules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入部门编码', trigger: 'blur' }]
}

const parentDeptName = computed(() => {
  if (!currentDept.value?.parentId) return '无'
  const findParent = (nodes, parentId) => {
    for (const node of nodes) {
      if (node.id === parentId) return node.name
      if (node.children) {
        const result = findParent(node.children, parentId)
        if (result) return result
      }
    }
    return null
  }
  return findParent(departmentTree.value, currentDept.value.parentId) || '-'
})

const filterNode = (value, data) => {
  if (!value) return true
  return data.name.includes(value)
}

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

const handleNodeClick = (data) => {
  currentDept.value = data
  loadDeptEmployees(data.id)
}

const handleAddRoot = () => {
  dialogTitle.value = '新增根部门'
  resetForm()
  formData.parentId = null
  dialogVisible.value = true
}

const handleAddChild = (data) => {
  dialogTitle.value = '新增子部门'
  resetForm()
  formData.parentId = data.id
  dialogVisible.value = true
}

const handleEdit = (data) => {
  dialogTitle.value = '编辑部门'
  resetForm()
  Object.assign(formData, {
    id: data.id,
    parentId: data.parentId,
    name: data.name,
    code: data.code,
    leader: data.leader,
    phone: data.phone,
    email: data.email,
    sort: data.sort,
    status: data.status
  })
  dialogVisible.value = true
}

const handleDelete = (data) => {
  if (data.children && data.children.length > 0) {
    ElMessage.warning('该部门下有子部门，无法删除')
    return
  }
  ElMessageBox.confirm(`确定要删除部门 "${data.name}" 吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteDepartmentApi(data.id)
      ElMessage.success('删除成功')
      await loadTree()
    } catch (e) {
      // 错误提示由 request 拦截器统一处理
    }
  }).catch(() => {})
}

const resetForm = () => {
  Object.keys(formData).forEach(key => {
    formData[key] = key === 'sort' ? 0 : key === 'status' ? 1 : null
  })
  formData.id = null
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      // 映射回后端字段；parentId 为空表示根部门（后端约定 0）
      const payload = {
        id: formData.id,
        parentId: formData.parentId || 0,
        deptName: formData.name,
        deptCode: formData.code,
        leader: formData.leader,
        phone: formData.phone,
        email: formData.email,
        sort: formData.sort,
        status: formData.status
      }
      if (formData.id) {
        await updateDepartmentApi(payload)
        ElMessage.success('编辑成功')
      } else {
        delete payload.id
        await addDepartmentApi(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      await loadTree(formData.id || currentDept.value?.id)
    } finally {
      submitLoading.value = false
    }
  })
}

const refreshData = () => {
  loadTree(currentDept.value?.id)
}

onMounted(() => {
  loadTree()
})
</script>

<style scoped lang="scss">
.department-container {
  .tree-card {
    height: 100%;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .filter-input {
    margin-bottom: 16px;
  }

  .tree-node {
    display: flex;
    align-items: center;
    flex: 1;
    font-size: 14px;

    .node-label {
      flex: 1;
    }

    .node-count {
      color: var(--el-text-color-secondary);
      font-size: 12px;
      margin-right: 10px;
    }

    .node-actions {
      display: none;
      gap: 8px;

      .el-icon {
        cursor: pointer;
        color: var(--el-text-color-secondary);
        font-size: 14px;

        &:hover {
          color: var(--el-color-primary);
        }

        &.delete-icon:hover {
          color: var(--el-color-danger);
        }
      }
    }

    &:hover .node-actions {
      display: flex;
    }
  }

  .dept-detail {
    .mb-20 {
      margin-bottom: 20px;
    }

    .section-title {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      font-size: 16px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }
  }
}
</style>