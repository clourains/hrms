<template>
  <div class="page-stack">
    <!-- 页头 -->
    <PageHeader
      title="公告管理"
      description="发布系统公告，发布后所有用户首页置顶可见；支持已读人数统计"
      icon="Notification"
    >
      <template #actions>
        <el-button type="primary" :icon="Download" @click="handleExport">导出 Excel</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">发布公告</el-button>
      </template>
    </PageHeader>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="标题 / 内容" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="已发布" :value="1" />
            <el-option label="已下线" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-submit">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 公告表格 -->
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="level" label="级别" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === 'important' ? 'danger' : 'info'" size="small">
              {{ row.level === 'important' ? '重要' : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
              {{ row.status === 1 ? '已发布' : '已下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publisher" label="发布人" width="110" align="center" />
        <el-table-column prop="createTime" label="发布时间" width="165" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="openReadStats(row)">已读统计</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >{{ row.status === 1 ? '下线' : '上线' }}</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 发布/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '发布公告'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="级别">
          <el-radio-group v-model="form.level">
            <el-radio value="normal">普通</el-radio>
            <el-radio value="important">重要（首页置顶）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="公告正文" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存' : '发布' }}</el-button>
      </template>
    </el-dialog>

    <!-- 已读统计抽屉 -->
    <el-drawer v-model="statsVisible" title="已读统计" size="380px">
      <div v-loading="statsLoading">
        <el-statistic title="已读 / 总用户" :value="`${stats.readCount} / ${stats.totalUsers}`" style="margin-bottom: 16px" />
        <el-empty v-if="stats.readers.length === 0" description="暂无人阅读" :image-size="60" />
        <el-table v-else :data="stats.readers" size="small" border>
          <el-table-column prop="username" label="用户" width="120" />
          <el-table-column prop="readTime" label="阅读时间">
            <template #default="{ row }">{{ formatTime(row.readTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Download } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  getNoticeListApi, createNoticeApi, updateNoticeApi,
  deleteNoticeApi, getNoticeReadStatsApi
} from '@/api/notice'

const loading = ref(false)
const tableData = ref([])
const searchForm = reactive({ keyword: '', status: null })
const pagination = reactive({ current: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, title: '', content: '', level: 'normal' })

const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const statsVisible = ref(false)
const statsLoading = ref(false)
const stats = reactive({ totalUsers: 0, readCount: 0, readers: [] })

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: pagination.current, size: pagination.size }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
    const res = await getNoticeListApi(params)
    if (res.code === 200) {
      tableData.value = res.data.records || []
      pagination.total = Number(res.data.total) || 0
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = null
  handleSearch()
}

// 导出 Excel（P8-A）
const handleExport = async () => {
  const base = import.meta.env.VITE_API_BASE || '/api'
  const token = localStorage.getItem('token') || ''
  const qs = new URLSearchParams()
  if (searchForm.keyword) qs.append('keyword', searchForm.keyword)
  if (searchForm.status !== null && searchForm.status !== '') qs.append('status', searchForm.status)
  try {
    const r = await fetch(`${base}/notice/export?${qs.toString()}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (!r.ok) throw new Error(r.statusText)
    const blob = await r.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = '公告列表.xlsx'
    a.click()
    URL.revokeObjectURL(a.href)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  Object.assign(form, { id: null, title: '', content: '', level: 'normal' })
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(form, { id: row.id, title: row.title, content: row.content || '', level: row.level || 'normal' })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const res = isEdit.value ? await updateNoticeApi(form) : await createNoticeApi(form)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '已保存' : '发布成功')
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
  const res = await updateNoticeApi({ id: row.id, status: target })
  if (res.code === 200) {
    ElMessage.success(target === 1 ? '已上线' : '已下线')
    loadData()
  } else {
    ElMessage.error(res.message || '操作失败')
  }
}

const openReadStats = async (row) => {
  statsVisible.value = true
  statsLoading.value = true
  try {
    const res = await getNoticeReadStatsApi(row.id)
    if (res.code === 200) {
      Object.assign(stats, {
        totalUsers: res.data.totalUsers || 0,
        readCount: res.data.readCount || 0,
        readers: res.data.readers || []
      })
    }
  } finally {
    statsLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    const res = await deleteNoticeApi(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    // 用户取消
  }
}

onMounted(loadData)
</script>
