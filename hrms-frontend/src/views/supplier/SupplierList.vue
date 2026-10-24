<template>
  <div class="page-container">
    <el-card shadow="hover">
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="关键字">
            <el-input v-model="searchForm.keyword" placeholder="公司名称 / 编码 / 类别" clearable style="width: 240px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增供应商</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe @sort-change="handleSort">
        <el-table-column prop="supplierCode" label="编码" width="120" sortable="custom" />
        <el-table-column prop="companyName" label="公司名称" min-width="200" show-overflow-tooltip sortable="custom" />
        <el-table-column prop="contactName" label="联系人" width="110" />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="category" label="供应类别" min-width="140" show-overflow-tooltip sortable="custom" />
        <el-table-column prop="status" label="状态" width="90" align="center" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '合作中' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑供应商' : '新增供应商'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="编码" prop="supplierCode">
              <el-input v-model="form.supplierCode" placeholder="如 SUP-001" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="form.companyName" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="电话" prop="phone">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="国家" prop="country">
              <el-input v-model="form.country" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市" prop="city">
              <el-input v-model="form.city" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="供应类别" prop="category">
          <el-input v-model="form.category" placeholder="如 LED照明 / 电子产品" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">合作中</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getSupplierPageApi, addSupplierApi, updateSupplierApi, deleteSupplierApi } from '@/api/trade'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const searchForm = reactive({ keyword: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0, sortField: null, sortOrder: null })

const defaultForm = { id: null, supplierCode: '', companyName: '', contactName: '', country: '中国', city: '', phone: '', email: '', category: '', status: 1, remark: '' }
const form = reactive({ ...defaultForm })

// 新增时必填：编码、公司名称、联系人、电话、国家、城市、供应类别
// 编辑时只保留编码和名称，避免历史数据缺联系人的老供应商改不了
const rules = computed(() => {
  const base = {
    supplierCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
    companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }]
  }
  if (isEdit.value) return base
  return {
    ...base,
    contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
    phone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
    country: [{ required: true, message: '请输入国家', trigger: 'blur' }],
    city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
    category: [{ required: true, message: '请输入供应类别', trigger: 'blur' }]
  }
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSupplierPageApi({
      ...searchForm,
      sortField: pagination.sortField,
      sortOrder: pagination.sortOrder,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNum = 1; loadData() }
const handleReset = () => {
  searchForm.keyword = ''
  pagination.sortField = null
  pagination.sortOrder = null
  handleSearch()
}

// 排序：供应商编码按自然序（P004）
const handleSort = ({ prop, order }) => {
  pagination.sortField = order ? prop : null
  pagination.sortOrder = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : null
  handleSearch()
}

// 切换每页条数回到第 1 页（P031）
const handleSizeChange = () => { pagination.pageNum = 1; loadData() }

const handleAdd = () => { Object.assign(form, defaultForm); isEdit.value = false; dialogVisible.value = true }
const handleEdit = (row) => { Object.assign(form, defaultForm, row); isEdit.value = true; dialogVisible.value = true }

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateSupplierApi(form)
      ElMessage.success('更新成功')
    } else {
      await addSupplierApi(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除供应商「${row.companyName}」吗？`, '提示', { type: 'warning' })
  await deleteSupplierApi(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page-container { width: 100%; }
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
