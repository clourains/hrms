<template>
  <div class="page-stack">
    <!-- 页头 -->
    <PageHeader
      title="参数配置"
      description="系统运行参数在线调整：薪资计算、考勤扣款、SMTP 邮件等规则即时生效，无需改代码重启"
      icon="Tools"
    >
      <template #actions>
        <el-button type="primary" plain :icon="Promotion" @click="handleTestMail">测试发信</el-button>
        <el-button :icon="Connection" @click="openApiRef.open()">开放接口</el-button>
      </template>
    </PageHeader>

    <!-- 参数表格 -->
    <div class="table-card">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="修改参数立即生效；薪资、考勤相关参数只影响之后的新计算，不回溯历史记录"
        style="margin-bottom: 16px"
      />
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="configKey" label="参数键" min-width="200">
          <template #default="{ row }">
            <code>{{ row.configKey }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="configValue" label="参数值" width="130" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="primary" effect="plain">{{ row.configValue }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column prop="updateBy" label="修改人" width="110" align="center">
          <template #default="{ row }">{{ row.updateBy || '-' }}</template>
        </el-table-column>
        <el-table-column prop="updateTime" label="修改时间" width="165" align="center">
          <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" title="编辑参数" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="参数键">
          <el-input :model-value="currentRow?.configKey" disabled />
        </el-form-item>
        <el-form-item label="说明">
          <span>{{ currentRow?.remark || '-' }}</span>
        </el-form-item>
        <el-form-item label="参数值" required>
          <el-input v-model="editValue" placeholder="请输入参数值" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <OpenApiDialog ref="openApiRef" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion, Connection } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import OpenApiDialog from '@/components/OpenApiDialog.vue'
import { getConfigListApi, updateConfigApi, testMailApi } from '@/api/config'

const loading = ref(false)
const openApiRef = ref(null)
const tableData = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const currentRow = ref(null)
const editValue = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getConfigListApi()
    if (res.code === 200) {
      tableData.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

const openEditDialog = (row) => {
  currentRow.value = row
  editValue.value = row.configValue
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!editValue.value.trim()) {
    ElMessage.warning('参数值不能为空')
    return
  }
  submitting.value = true
  try {
    const res = await updateConfigApi(currentRow.value.configKey, editValue.value.trim())
    if (res.code === 200) {
      ElMessage.success('参数已更新')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } finally {
    submitting.value = false
  }
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

// P6-3 邮件连通性测试：先配好 mail.* 参数再点
const handleTestMail = async () => {
  try {
    const { value } = await ElMessageBox.prompt(
      '向指定邮箱发送一封测试邮件（需先配置 mail.enabled=true 及 host/username/password）',
      '测试发信',
      { confirmButtonText: '发送', cancelButtonText: '取消', inputPlaceholder: '收件邮箱地址' }
    )
    if (!value || !value.trim()) return
    const res = await testMailApi(value.trim())
    if (res.code === 200) {
      ElMessage.success(res.message || '测试邮件已发送')
    } else {
      ElMessage.error(res.message || '发送失败')
    }
  } catch {
    // 用户取消
  }
}

onMounted(loadData)
</script>
