<template>
  <el-dialog v-model="visible" title="开放接口（API 自动化写入）" width="720px" destroy-on-close>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="外部 ERP、脚本或爬虫可直接调用以下接口批量写入数据，无需登录；请求头带上 X-API-Key 即可。密钥泄露时可点击重置，旧密钥立即失效。"
      style="margin-bottom: 14px"
    />

    <div class="key-row">
      <span class="key-label">接口前缀</span>
      <code>{{ baseUrl }}</code>
    </div>
    <div class="key-row">
      <span class="key-label">请求头</span>
      <code>X-API-Key: {{ shownKey }}</code>
      <el-button link type="primary" @click="keyVisible = !keyVisible">{{ keyVisible ? '隐藏' : '显示' }}</el-button>
      <el-button link type="primary" :icon="CopyDocument" @click="copy(apiKey)">复制密钥</el-button>
      <el-button link type="danger" :icon="RefreshRight" @click="rotate">重置密钥</el-button>
    </div>

    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="密钥等同于管理员权限，请勿写入前端代码、提交到代码仓库或发送到聊天群；仅在本机粘贴到调用脚本或服务端配置中。怀疑泄露请立即重置。"
      style="margin: 4px 0 14px"
    />

    <el-table :data="endpoints" size="small" border style="margin-top: 14px">
      <el-table-column prop="name" label="用途" width="150" />
      <el-table-column prop="method" label="方法" width="70" align="center" />
      <el-table-column prop="path" label="路径" min-width="220">
        <template #default="{ row }"><code>{{ row.path }}</code></template>
      </el-table-column>
      <el-table-column prop="fields" label="字段" min-width="240" show-overflow-tooltip />
    </el-table>

    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="请求体格式：{&quot;items&quot;:[{...}]}；销售订单的 items 里再嵌套明细 items。与 Excel 导入同一套校验，重复数据自动跳过。"
      style="margin-top: 12px"
    />

    <div class="log-head">
      <span class="log-title">最近调用记录</span>
      <el-button link type="primary" :icon="Refresh" :loading="logLoading" @click="loadLogs">刷新</el-button>
    </div>
    <el-table v-loading="logLoading" :data="logs" size="small" border empty-text="暂无接口调用记录">
      <el-table-column label="时间" width="145">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="模块" width="100">
        <template #default="{ row }">{{ moduleText(row.module) }}</template>
      </el-table-column>
      <el-table-column label="提交 / 成功 / 跳过 / 失败" width="165">
        <template #default="{ row }">
          {{ row.total }} / <span class="ok">{{ row.success }}</span> /
          <span class="skip">{{ row.skipped }}</span> /
          <span :class="{ bad: row.failed > 0 }">{{ row.failed }}</span>
        </template>
      </el-table-column>
      <el-table-column label="失败明细" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.errors || '-' }}</template>
      </el-table-column>
      <el-table-column prop="ip" label="调用 IP" width="120" show-overflow-tooltip />
    </el-table>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Refresh, RefreshRight } from '@element-plus/icons-vue'
import { getImportLogsApi, getOpenApiCredentialApi, rotateOpenApiKeyApi } from '@/api/dataImport'

const visible = ref(false)
const apiKey = ref('')
const keyVisible = ref(false)
const endpoints = ref([])
const logs = ref([])
const logLoading = ref(false)
const baseUrl = window.location.origin + '/api'

const MODULE_TEXT = { shipment: '发货单', order: '销售订单', customer: '客户', product: '产品', employee: '员工' }
const moduleText = (module) => MODULE_TEXT[module] || module || '-'

// 后端返回形如 2026-10-29T10:00:00 或 2026-10-29 10:00:00，统一成可读格式
const fmtTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 19) : '-')

// 默认打码：截屏、旁人围观时不会把完整密钥带走；复制按钮仍复制完整值
const shownKey = computed(() => {
  if (!apiKey.value) return '（未生成）'
  if (keyVisible.value) return apiKey.value
  const tail = apiKey.value.slice(-4)
  return apiKey.value.slice(0, 5) + '••••••••••••' + tail
})

const open = async () => {
  visible.value = true
  keyVisible.value = false
  const res = await getOpenApiCredentialApi()
  apiKey.value = res.data.apiKey
  endpoints.value = res.data.endpoints || []
  loadLogs()
}

// 只看外部系统的调用（source=API），Excel 导入记录不混进来
const loadLogs = async () => {
  logLoading.value = true
  try {
    const res = await getImportLogsApi(null, 10, 'API')
    logs.value = res.data || []
  } finally {
    logLoading.value = false
  }
}

const copy = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    ElMessage.warning('浏览器不允许自动复制，请手动选中复制')
  }
}

const rotate = async () => {
  await ElMessageBox.confirm('重置后旧密钥立即失效，正在使用旧密钥的外部系统需要同步更新，确定继续吗？', '提示', { type: 'warning' })
  const res = await rotateOpenApiKeyApi()
  apiKey.value = res.data.apiKey
  ElMessage.success('密钥已重置')
}

defineExpose({ open })
</script>

<style scoped>
.key-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.key-label {
  width: 72px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.key-row code {
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  font-size: 13px;
  word-break: break-all;
}
.log-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 8px;
}
.log-title {
  font-size: 14px;
  font-weight: 600;
}
.ok {
  color: var(--el-color-success);
}
.skip {
  color: var(--el-text-color-secondary);
}
.bad {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
