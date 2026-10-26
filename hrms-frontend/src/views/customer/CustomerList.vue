<template>
  <div class="page-container">
    <el-card shadow="hover">
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="关键字">
            <el-input v-model="searchForm.keyword" placeholder="公司名称 / 编码 / 联系人" clearable style="width: 240px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="国家">
            <el-input v-model="searchForm.country" placeholder="如 美国" clearable style="width: 140px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增客户</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe @sort-change="handleSort">
        <el-table-column prop="customerCode" label="客户编码" width="120" sortable="custom" />
        <el-table-column prop="companyName" label="公司名称" min-width="200" show-overflow-tooltip sortable="custom" />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="country" label="国家" width="100" sortable="custom" />
        <el-table-column prop="city" label="城市" width="110" sortable="custom" />
        <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
        <el-table-column prop="level" label="等级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === 'A' ? 'danger' : row.level === 'B' ? 'warning' : 'info'" effect="dark">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creditLimitUsd" label="信用额度(USD)" width="130" align="right">
          <template #default="{ row }">${{ formatNum(row.creditLimitUsd) }}</template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑客户' : '新增客户'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户编码" prop="customerCode">
              <el-input v-model="form.customerCode" placeholder="如 CUS-001" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户等级">
              <el-select v-model="form.level" style="width: 100%">
                <el-option label="A - 重点客户" value="A" />
                <el-option label="B - 普通客户" value="B" />
                <el-option label="C - 一般客户" value="C" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="form.companyName" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话" prop="phone">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="网站">
              <el-input v-model="form.website" />
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
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="信用额度(USD)">
              <el-input-number v-model="form.creditLimitUsd" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">合作中</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
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
import { getCustomerPageApi, addCustomerApi, updateCustomerApi, deleteCustomerApi } from '@/api/trade'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const searchForm = reactive({ keyword: '', country: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0, sortField: null, sortOrder: null })

const defaultForm = { id: null, customerCode: '', companyName: '', contactName: '', country: '美国', city: '', address: '', email: '', phone: '', website: '', level: 'C', creditLimitUsd: 0, status: 1, remark: '' }
const form = reactive({ ...defaultForm })

// 新增和编辑都必须填满：编码、名称、联系人、电话、国家、城市、地址
// 早期编辑时放开了后 5 项，结果「新增必填 → 编辑清空」就能绕过校验，这里统一成一套规则
const rules = {
  customerCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }],
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
  country: [{ required: true, message: '请输入国家', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

const formatNum = (n) => (n == null ? '0.00' : Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }))

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCustomerPageApi({
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
  searchForm.country = ''
  pagination.sortField = null
  pagination.sortOrder = null
  handleSearch()
}

// 排序：客户编码按自然序（P004）
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
      await updateCustomerApi(form)
      ElMessage.success('更新成功')
    } else {
      await addCustomerApi(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除客户「${row.companyName}」吗？`, '提示', { type: 'warning' })
  await deleteCustomerApi(row.id)
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
