<template>
  <div class="audit-container page-stack">
    <!-- 页头 -->
    <PageHeader
      title="审计日志"
      description="记录敏感模块的关键操作，支持按操作人、模块、时间追溯"
      icon="Document"
    >
      <template #actions>
        <el-button type="primary" :icon="Download" @click="handleExport">导出 Excel</el-button>
      </template>
    </PageHeader>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="操作人">
          <el-input v-model="searchForm.username" placeholder="请输入操作人" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="searchForm.module" placeholder="请选择模块" clearable style="width: 160px">
            <el-option v-for="m in moduleOptions" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="searchForm.success" placeholder="全部结果" clearable style="width: 130px">
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
        <el-table-column prop="createTime" label="操作时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="110" align="center" />
        <el-table-column prop="module" label="模块" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="100" align="center" />
        <el-table-column prop="detail" label="操作详情" min-width="240" show-overflow-tooltip />
        <el-table-column prop="success" label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="失败原因" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.errorMsg || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时" width="90" align="center">
          <template #default="{ row }">
            {{ row.costMs != null ? row.costMs + 'ms' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" align="center" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
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

    <!-- 审计日志详情弹窗（P8-A） -->
    <el-dialog v-model="detailVisible" title="审计日志详情" width="540px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="操作时间">{{ formatTime(currentRow.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentRow.username }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ currentRow.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentRow.action }}</el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="currentRow.success === 1 ? 'success' : 'danger'" size="small">
            {{ currentRow.success === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentRow.costMs != null ? currentRow.costMs + ' ms' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ currentRow.ip }}</el-descriptions-item>
        <el-descriptions-item label="操作详情">
          <div style="white-space: pre-wrap; font-family: Consolas, monospace; font-size: 12px; background: var(--el-fill-color-lighter); padding: 8px; border-radius: 4px;">
            {{ currentRow.detail || '-' }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentRow.success === 0" label="失败原因">
          <div style="white-space: pre-wrap; color: var(--el-color-danger);">{{ currentRow.errorMsg || '-' }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getAuditLogListApi } from '@/api/auditLog'

// 审计覆盖的模块（与后端 @AuditLog 注解保持一致）
const moduleOptions = ['员工管理', '部门管理', '薪资管理', '绩效管理', '合同管理', '公告管理', '参数配置', '用户管理', '消息管理']

const loading = ref(false)
const tableData = ref([])
const searchForm = reactive({
  username: '',
  module: '',
  success: null,
  range: null
})
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 详情弹窗
const detailVisible = ref(false)
const currentRow = ref({})

const openDetail = (row) => {
  currentRow.value = row
  detailVisible.value = true
}

const buildQuery = () => {
  const params = { current: pagination.current, size: pagination.size }
  if (searchForm.username) params.username = searchForm.username
  if (searchForm.module) params.module = searchForm.module
  if (searchForm.success !== null && searchForm.success !== '') params.success = searchForm.success
  if (searchForm.range && searchForm.range.length === 2) {
    params.startTime = searchForm.range[0]
    params.endTime = searchForm.range[1]
  }
  return params
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAuditLogListApi(buildQuery())
    if (res.code === 200) {
      tableData.value = res.data.records || []
      pagination.total = Number(res.data.total) || 0
    }
  } finally {
    loading.value = false
  }
}

// 导出 Excel（P8-A）
const handleExport = async () => {
  const base = import.meta.env.VITE_API_BASE || '/api'
  const token = localStorage.getItem('token') || ''
  const qs = new URLSearchParams()
  qs.append('username', searchForm.username)
  qs.append('module', searchForm.module)
  if (searchForm.success !== null && searchForm.success !== '') qs.append('success', searchForm.success)
  if (searchForm.range && searchForm.range.length === 2) {
    qs.append('startTime', searchForm.range[0])
    qs.append('endTime', searchForm.range[1])
  }
  try {
    const r = await fetch(`${base}/auditLog/export?${qs.toString()}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (!r.ok) throw new Error(r.statusText)
    const blob = await r.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = '审计日志.xlsx'
    a.click()
    URL.revokeObjectURL(a.href)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.module = ''
  searchForm.success = null
  searchForm.range = null
  handleSearch()
}

const formatTime = (t) => {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

onMounted(loadData)
</script>
