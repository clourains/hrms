<template>
  <el-dialog v-model="visible" :title="`批量导入${label}`" width="620px" destroy-on-close @closed="reset">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      :title="tip"
      style="margin-bottom: 14px"
    />
    <div class="import-toolbar">
      <el-button link type="primary" :icon="Download" @click="downloadTemplate">下载导入模板</el-button>
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :show-file-list="true"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
      >
        <el-button :icon="Upload">选择文件</el-button>
      </el-upload>
    </div>

    <template v-if="result">
      <el-alert
        :type="result.failed === 0 ? 'success' : 'warning'"
        :closable="false"
        show-icon
        :title="`导入完成：成功 ${result.success} 条，跳过 ${result.skipped} 条，失败 ${result.failed} 条`"
        style="margin-bottom: 10px"
      />
      <el-table v-if="result.errors && result.errors.length > 0" :data="result.errors" size="small" border max-height="240">
        <el-table-column prop="row" label="行号" width="80" align="center" />
        <el-table-column prop="message" label="说明" min-width="300" show-overflow-tooltip />
      </el-table>
      <div v-if="result.failed > 0 || result.skipped > 0" class="result-tip">
        修改模板后可再次选择文件重新导入；已成功的数据不会重复写入。
      </div>
    </template>

    <template v-if="logs.length > 0">
      <div class="logs-title">最近导入记录</div>
      <el-table :data="logs" size="small" border max-height="180">
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="70" align="center" />
        <el-table-column prop="operator" label="操作人" width="110" show-overflow-tooltip />
        <el-table-column label="结果" min-width="180">
          <template #default="{ row }">
            成功 {{ row.success }} · 跳过 {{ row.skipped }} · 失败 {{ row.failed }}
          </template>
        </el-table-column>
      </el-table>
      <div class="result-tip">审计记录只保留来源、操作人与数量，不记录具体业务字段内容。</div>
    </template>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" :loading="loading" @click="submit">开始导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Download } from '@element-plus/icons-vue'
import { downloadImportTemplateApi, importExcelApi, getImportLogsApi } from '@/api/dataImport'

const props = defineProps({
  module: { type: String, required: true },
  label: { type: String, required: true },
  tip: {
    type: String,
    default: '按模板表头填写，带唯一编码的数据（编码/SKU/订单号/发票号）已存在时会自动跳过并在下方说明'
  }
})

const emit = defineEmits(['success'])

const visible = ref(false)
const loading = ref(false)
const uploadRef = ref(null)
const fileRaw = ref(null)
const result = ref(null)
const logs = ref([])

const open = () => {
  visible.value = true
  loadLogs()
}

const loadLogs = async () => {
  try {
    const res = await getImportLogsApi(props.module, 10)
    logs.value = res.data || []
  } catch (e) {
    logs.value = []
  }
}

const formatTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 19) : '-')

const reset = () => {
  fileRaw.value = null
  result.value = null
  loading.value = false
  uploadRef.value?.clearFiles()
}

const handleFileChange = (file) => {
  fileRaw.value = file.raw
  result.value = null
}



const downloadTemplate = async () => {
  try {
    const res = await downloadImportTemplateApi(props.module)
    const blob = new Blob([res.data], { type: res.headers['content-type'] })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${props.label}导入模板.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('模板下载失败')
  }
}

const submit = async () => {
  if (!fileRaw.value) {
    ElMessage.warning('请先选择 Excel 文件')
    return
  }
  loading.value = true
  try {
    const res = await importExcelApi(props.module, fileRaw.value)
    result.value = res.data
    if (res.data.failed === 0 && res.data.skipped === 0) {
      ElMessage.success(`导入成功 ${res.data.success} 条`)
    } else {
      ElMessage.warning(`成功 ${res.data.success} 条，跳过 ${res.data.skipped} 条，失败 ${res.data.failed} 条`)
    }
    if (res.data.success > 0) {
      emit('success')
    }
    loadLogs()
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.import-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.import-toolbar :deep(.el-upload-list) {
  margin-top: 8px;
}
.result-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.logs-title {
  margin: 16px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
</style>
