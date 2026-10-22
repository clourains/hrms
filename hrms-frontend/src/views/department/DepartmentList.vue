<template>
  <div class="department-container">
    <el-row :gutter="20">
      <!-- 左侧部门树 -->
      <el-col :span="8">
        <el-card shadow="hover" class="tree-card">
          <template #header>
            <div class="card-header">
              <span>部门结构</span>
              <el-button type="primary" link :icon="Plus" @click="handleAddRoot">新增根部门</el-button>
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
              <div>
                <el-button :icon="Refresh" @click="refreshData">刷新</el-button>
              </div>
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
              <el-descriptions-item label="部门描述" :span="2">{{ currentDept.description || '-' }}</el-descriptions-item>
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
        <el-form-item label="部门描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入部门描述" />
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

const defaultExpandedKeys = ref([1])

const departmentTree = ref([
  {
    id: 1,
    name: '总公司',
    code: 'HQ001',
    leader: '张总',
    phone: '010-88888888',
    email: 'hq@company.com',
    employeeCount: 256,
    sort: 1,
    status: 1,
    description: '公司总部',
    createTime: '2020-01-01',
    children: [
      {
        id: 2,
        name: '技术部',
        code: 'TECH',
        leader: '李技术',
        phone: '010-88888801',
        email: 'tech@company.com',
        employeeCount: 85,
        sort: 1,
        status: 1,
        description: '负责产品研发',
        createTime: '2020-01-15',
        parentId: 1,
        children: [
          { id: 21, name: '前端组', code: 'TECH-FE', leader: '王前端', employeeCount: 25, sort: 1, status: 1, parentId: 2 },
          { id: 22, name: '后端组', code: 'TECH-BE', leader: '赵后端', employeeCount: 35, sort: 2, status: 1, parentId: 2 },
          { id: 23, name: '测试组', code: 'TECH-QA', leader: '钱测试', employeeCount: 15, sort: 3, status: 1, parentId: 2 },
          { id: 24, name: '运维组', code: 'TECH-OPS', leader: '孙运维', employeeCount: 10, sort: 4, status: 1, parentId: 2 }
        ]
      },
      {
        id: 3,
        name: '产品部',
        code: 'PROD',
        leader: '周产品',
        phone: '010-88888802',
        email: 'product@company.com',
        employeeCount: 35,
        sort: 2,
        status: 1,
        description: '负责产品规划与设计',
        createTime: '2020-01-20',
        parentId: 1,
        children: [
          { id: 31, name: '产品组', code: 'PROD-PM', leader: '吴产品', employeeCount: 20, sort: 1, status: 1, parentId: 3 },
          { id: 32, name: '设计组', code: 'PROD-UI', leader: '郑设计', employeeCount: 15, sort: 2, status: 1, parentId: 3 }
        ]
      },
      {
        id: 4,
        name: '市场部',
        code: 'MKT',
        leader: '冯市场',
        phone: '010-88888803',
        email: 'marketing@company.com',
        employeeCount: 28,
        sort: 3,
        status: 1,
        description: '负责市场营销推广',
        createTime: '2020-02-01',
        parentId: 1,
        children: []
      },
      {
        id: 5,
        name: '运营部',
        code: 'OPS',
        leader: '陈运营',
        phone: '010-88888804',
        email: 'operation@company.com',
        employeeCount: 32,
        sort: 4,
        status: 1,
        description: '负责产品运营',
        createTime: '2020-02-10',
        parentId: 1,
        children: []
      },
      {
        id: 6,
        name: '人事部',
        code: 'HR',
        leader: '褚人事',
        phone: '010-88888805',
        email: 'hr@company.com',
        employeeCount: 18,
        sort: 5,
        status: 1,
        description: '负责人力资源管理',
        createTime: '2020-01-05',
        parentId: 1,
        children: []
      },
      {
        id: 7,
        name: '财务部',
        code: 'FIN',
        leader: '卫财务',
        phone: '010-88888806',
        email: 'finance@company.com',
        employeeCount: 15,
        sort: 6,
        status: 1,
        description: '负责财务管理',
        createTime: '2020-01-05',
        parentId: 1,
        children: []
      },
      {
        id: 8,
        name: '行政部',
        code: 'ADM',
        leader: '蒋行政',
        phone: '010-88888807',
        email: 'admin@company.com',
        employeeCount: 20,
        sort: 7,
        status: 1,
        description: '负责行政管理',
        createTime: '2020-01-05',
        parentId: 1,
        children: []
      },
      {
        id: 9,
        name: '销售部',
        code: 'SALES',
        leader: '沈销售',
        phone: '010-88888808',
        email: 'sales@company.com',
        employeeCount: 23,
        sort: 8,
        status: 1,
        description: '负责销售业务',
        createTime: '2020-03-01',
        parentId: 1,
        children: []
      }
    ]
  }
])

const deptEmployees = ref([
  { employeeNo: 'E001', name: '张三', position: '高级工程师', phone: '13800138001', entryDate: '2022-01-15', status: 1 },
  { employeeNo: 'E002', name: '李四', position: '前端工程师', phone: '13800138002', entryDate: '2022-03-20', status: 1 },
  { employeeNo: 'E009', name: '郑十一', position: '后端工程师', phone: '13800138009', entryDate: '2022-05-18', status: 1 }
])

const formData = reactive({
  id: null,
  parentId: null,
  name: '',
  code: '',
  leader: '',
  phone: '',
  email: '',
  sort: 0,
  status: 1,
  description: ''
})

const formRules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入部门编码', trigger: 'blur' }]
}

const parentDeptName = computed(() => {
  if (!currentDept.value?.parentId) return '无'
  // 简单查找父部门名称
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
  Object.assign(formData, data)
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
  }).then(() => {
    // 简单删除逻辑
    const removeNode = (nodes, id) => {
      for (let i = 0; i < nodes.length; i++) {
        if (nodes[i].id === id) {
          nodes.splice(i, 1)
          return true
        }
        if (nodes[i].children && removeNode(nodes[i].children, id)) {
          return true
        }
      }
      return false
    }
    removeNode(departmentTree.value, data.id)
    if (currentDept.value?.id === data.id) {
      currentDept.value = null
    }
    ElMessage.success('删除成功')
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
  await formRef.value.validate((valid) => {
    if (valid) {
      submitLoading.value = true
      setTimeout(() => {
        if (formData.id) {
          // 编辑 - 更新节点
          const updateNode = (nodes) => {
            for (const node of nodes) {
              if (node.id === formData.id) {
                Object.assign(node, formData)
                return true
              }
              if (node.children && updateNode(node.children)) {
                return true
              }
            }
            return false
          }
          updateNode(departmentTree.value)
          if (currentDept.value?.id === formData.id) {
            Object.assign(currentDept.value, formData)
          }
          ElMessage.success('编辑成功')
        } else {
          // 新增
          const newDept = {
            ...formData,
            id: Date.now(),
            employeeCount: 0,
            createTime: new Date().toISOString().slice(0, 10),
            children: []
          }
          if (formData.parentId) {
            // 添加为子部门
            const addToParent = (nodes) => {
              for (const node of nodes) {
                if (node.id === formData.parentId) {
                  if (!node.children) node.children = []
                  node.children.push(newDept)
                  return true
                }
                if (node.children && addToParent(node.children)) {
                  return true
                }
              }
              return false
            }
            addToParent(departmentTree.value)
          } else {
            // 添加为根部门
            departmentTree.value.push(newDept)
          }
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        submitLoading.value = false
      }, 500)
    }
  })
}

const refreshData = () => {
  ElMessage.success('刷新成功')
}

onMounted(() => {
  // 默认选中第一个节点
  if (departmentTree.value.length > 0) {
    currentDept.value = departmentTree.value[0]
  }
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
      color: #909399;
      font-size: 12px;
      margin-right: 10px;
    }

    .node-actions {
      display: none;
      gap: 8px;

      .el-icon {
        cursor: pointer;
        color: #909399;
        font-size: 14px;

        &:hover {
          color: #409EFF;
        }

        &.delete-icon:hover {
          color: #F56C6C;
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
      color: #303133;
    }
  }
}
</style>
