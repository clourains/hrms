<template>
  <div class="page-container page-stack">
    <!-- 页头 -->
    <PageHeader
      title="产品管理"
      description="维护产品资料与成本售价，支持按 SKU、分类检索"
      icon="Goods"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增产品</el-button>
      </template>
    </PageHeader>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="SKU / 名称 / 分类" clearable style="width: 220px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="在售" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-submit">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 产品列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe @sort-change="handleSort">
        <el-table-column prop="sku" label="SKU" width="120" sortable="custom" />
        <el-table-column prop="name" label="产品名称" min-width="150" sortable="custom" />
        <el-table-column prop="nameEn" label="英文名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" sortable="custom" />
        <el-table-column prop="unit" label="单位" width="70" align="center" />
        <el-table-column prop="costPrice" label="成本价(CNY)" width="130" align="right" sortable="custom">
          <template #default="{ row }">¥{{ formatNum(row.costPrice) }}</template>
        </el-table-column>
        <el-table-column prop="salePriceUsd" label="销售价(USD)" width="130" align="right" sortable="custom">
          <template #default="{ row }">${{ formatNum(row.salePriceUsd) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '在售' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination page-pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑产品' : '新增产品'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="SKU编码" prop="sku">
              <el-input v-model="form.sku" placeholder="如 LED-001" :disabled="isEdit" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-input v-model="form.category" placeholder="如 电子" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="产品名称" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="英文名称">
          <el-input v-model="form.nameEn" maxlength="200" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="单位">
              <el-input v-model="form.unit" placeholder="pcs" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="成本价(CNY)">
              <el-input-number v-model="form.costPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="销售价(USD)">
              <el-input-number v-model="form.salePriceUsd" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">在售</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="产品描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
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
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getProductPageApi, addProductApi, updateProductApi, deleteProductApi } from '@/api/trade'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const searchForm = reactive({ keyword: '', status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0, sortField: null, sortOrder: null })

const defaultForm = { id: null, sku: '', name: '', nameEn: '', category: '', unit: 'pcs', costPrice: 0, salePriceUsd: 0, description: '', status: 1 }
const form = reactive({ ...defaultForm })

const rules = {
  sku: [{ required: true, message: '请输入SKU编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入产品名称', trigger: 'blur' }]
}

const formatNum = (n) => (n == null ? '0.00' : Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }))

const loadData = async () => {
  loading.value = true
  try {
    const res = await getProductPageApi({
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
  searchForm.status = null
  pagination.sortField = null
  pagination.sortOrder = null
  handleSearch()
}

// 排序：SKU/名称按自然序，数字部分按大小比（P004）
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
      await updateProductApi(form)
      ElMessage.success('更新成功')
    } else {
      await addProductApi(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除产品「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteProductApi(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
