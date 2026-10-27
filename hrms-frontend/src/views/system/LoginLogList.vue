<template>
  <div class="page-stack">
    <!-- 页头 -->
    <PageHeader
      title="登录日志"
      description="记录账号登录成功与失败，可追溯来源 IP 与浏览器，用于安全排查"
      icon="Key"
    >
      <template #actions>
        <el-button type="primary" plain :icon="Download" @click="handleExport">导出 Excel</el-button>
      </template>
    </PageHeader>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="searchForm.success" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.range"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item class="search-submit">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 日志表格 -->
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="createTime" label="登录时间" width="170" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="success" label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="msg" label="说明" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.msg || '-' }}</template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="140" align="center" />
        <el-table-column prop="userAgent" label="浏览器" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.userAgent || '-' }}</template>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getLoginLogListApi } from '@/api/loginLog'

const loading = ref(false)
const tableData = ref([])
const searchForm = reactive({ username: '', success: null, range: null })
const pagination = reactive({ current: 1, size: 10, total: 0 })

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: pagination.current, size: pagination.size }
    if (searchForm.username) params.username = searchForm.username
    if (searchForm.success !== null && searchForm.success !== '') params.success = searchForm.success
    if (searchForm.range && searchForm.range.length === 2) {
      params.startTime = searchForm.range[0]
      params.endTime = searchForm.range[1]
    }
    const res = await getLoginLogListApi(params)
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
  searchForm.username = ''
  searchForm.success = null
  searchForm.range = null
  handleSearch()
}

// 导出 Excel（P8-A）
const handleExport = async () => {
  const base = import.meta.env.VITE_API_BASE || '/api'
  const token = localStorage.getItem('token') || ''
  const qs = new URLSearchParams()
  if (searchForm.username) qs.append('username', searchForm.username)
  if (searchForm.success !== null && searchForm.success !== '') qs.append('success', searchForm.success)
  if (searchForm.range && searchForm.range.length === 2) {
    qs.append('startTime', searchForm.range[0])
    qs.append('endTime', searchForm.range[1])
  }
  try {
    const r = await fetch(`${base}/loginLog/export?${qs.toString()}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (!r.ok) throw new Error(r.statusText)
    const blob = await r.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = '登录日志.xlsx'
    a.click()
    URL.revokeObjectURL(a.href)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

onMounted(loadData)
</script>
