<template>
  <div class="page-container">
    <el-card shadow="hover">
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="关键字">
            <el-input
              v-model="searchForm.keyword"
              placeholder="候选人 / 面试官 / 地点"
              clearable
              style="width: 210px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="轮次">
            <el-select v-model="searchForm.round" placeholder="全部" clearable style="width: 110px">
              <el-option label="初试" :value="1" />
              <el-option label="复试" :value="2" />
              <el-option label="终试" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="结果">
            <el-select v-model="searchForm.result" placeholder="全部" clearable style="width: 120px">
              <el-option label="待面试" :value="0" />
              <el-option label="通过" :value="1" />
              <el-option label="未通过" :value="2" />
              <el-option label="爽约" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" :icon="Plus" @click="handleAdd">安排面试</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="interviewTime" label="面试时间" width="140">
          <template #default="{ row }">{{ formatTime(row.interviewTime) }}</template>
        </el-table-column>
        <el-table-column prop="candidateName" label="候选人" width="95" />
        <el-table-column prop="candidatePhone" label="联系电话" width="120" />
        <el-table-column prop="requisitionPosition" label="应聘岗位" min-width="120" show-overflow-tooltip />
        <el-table-column prop="round" label="轮次" width="75" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ roundText(row.round) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="interviewType" label="形式" width="72" align="center">
          <template #default="{ row }">{{ typeText(row.interviewType) }}</template>
        </el-table-column>
        <el-table-column prop="interviewer" label="面试官" min-width="110" show-overflow-tooltip />
        <el-table-column prop="location" label="地点" min-width="130" show-overflow-tooltip />
        <el-table-column prop="result" label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="resultType(row.result)" size="small">{{ resultText(row.result) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="评分" width="70" align="center">
          <template #default="{ row }">{{ row.score ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="195" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.result === 0"
              type="success"
              link
              @click="openResult(row)"
            >登记结果</el-button>
            <el-button v-else type="primary" link @click="openResult(row)">改判结果</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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

    <!-- 安排 / 编辑面试 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑面试安排' : '安排面试'"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="候选人" prop="candidateId">
          <el-select
            v-model="form.candidateId"
            placeholder="请选择候选人"
            filterable
            :disabled="isEdit"
            style="width: 100%"
          >
            <el-option
              v-for="c in candidateOptions"
              :key="c.id"
              :label="`${c.name} · ${c.requisitionPosition || '未关联岗位'}（${stageMap[c.stage]?.text || ''}）`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="轮次" prop="round">
              <el-select v-model="form.round" style="width: 100%">
                <el-option label="初试" :value="1" />
                <el-option label="复试" :value="2" />
                <el-option label="终试" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="面试形式" prop="interviewType">
              <el-select v-model="form.interviewType" style="width: 100%">
                <el-option label="现场" :value="1" />
                <el-option label="电话" :value="2" />
                <el-option label="视频" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="面试时间" prop="interviewTime">
              <el-date-picker
                v-model="form.interviewTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时长(分钟)" prop="duration">
              <el-input-number v-model="form.duration" :min="15" :max="240" :step="15" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="面试官" prop="interviewer">
          <el-input v-model="form.interviewer" placeholder="多人用逗号分隔" />
        </el-form-item>
        <el-form-item label="地点" prop="location">
          <el-input v-model="form.location" placeholder="会议室 / 线上会议链接" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 登记结果 -->
    <el-dialog v-model="resultVisible" title="登记面试结果" width="480px" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="登记后会自动推进候选人阶段：初试通过进入复试，复试通过进入待录用，未通过/爽约自动淘汰。"
        style="margin-bottom: 14px"
      />
      <el-form label-width="90px">
        <el-form-item label="候选人">
          <span>{{ currentRow?.candidateName }} · {{ roundText(currentRow?.round) }}</span>
        </el-form-item>
        <el-form-item label="结果">
          <el-radio-group v-model="resultForm.result">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">未通过</el-radio>
            <el-radio :label="3">候选人爽约</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评分">
          <el-input-number v-model="resultForm.score" :min="0" :max="100" style="width: 160px" />
          <span class="tip">（选填，0~100）</span>
        </el-form-item>
        <el-form-item label="面试评价">
          <el-input v-model="resultForm.comment" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resultVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitResult">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getInterviewPageApi,
  addInterviewApi,
  updateInterviewApi,
  deleteInterviewApi,
  submitInterviewResultApi,
  getCandidatePageApi
} from '@/api/recruit'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const candidateOptions = ref([])
const dialogVisible = ref(false)
const resultVisible = ref(false)
const isEdit = ref(false)
const currentRow = ref(null)
const formRef = ref(null)

const searchForm = reactive({ keyword: '', round: null, result: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const stageMap = {
  1: '待筛选', 2: '初试', 3: '复试', 4: '待录用', 5: '已录用', 6: '已淘汰'
}

const defaultForm = {
  id: null,
  candidateId: null,
  requisitionId: null,
  round: 1,
  interviewType: 1,
  interviewTime: '',
  duration: 60,
  interviewer: '',
  location: ''
}
const form = reactive({ ...defaultForm })

const rules = {
  candidateId: [{ required: true, message: '请选择候选人', trigger: 'change' }],
  round: [{ required: true, message: '请选择轮次', trigger: 'change' }],
  interviewType: [{ required: true, message: '请选择面试形式', trigger: 'change' }],
  interviewTime: [{ required: true, message: '请选择面试时间', trigger: 'change' }],
  duration: [{ required: true, message: '请填写面试时长', trigger: 'blur' }],
  interviewer: [{ required: true, message: '请填写面试官', trigger: 'blur' }],
  location: [{ required: true, message: '请填写面试地点', trigger: 'blur' }]
}

const resultForm = reactive({ result: 1, score: null, comment: '' })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getInterviewPageApi({
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const loadCandidates = async () => {
  // 只取未淘汰的候选人，避免下拉里全是已凉的人
  const res = await getCandidatePageApi({ pageNum: 1, pageSize: 100 })
  candidateOptions.value = (res.data.list || []).filter((c) => c.stage !== 6)
}

const handleSearch = () => { pagination.pageNum = 1; loadData() }
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.round = null
  searchForm.result = null
  handleSearch()
}
const handleSizeChange = () => { pagination.pageNum = 1; loadData() }

const handleAdd = () => {
  Object.assign(form, defaultForm)
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, defaultForm, {
    id: row.id,
    candidateId: row.candidateId,
    requisitionId: row.requisitionId,
    round: row.round,
    interviewType: row.interviewType,
    interviewTime: row.interviewTime,
    duration: row.duration,
    interviewer: row.interviewer,
    location: row.location
  })
  isEdit.value = true
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateInterviewApi(form)
      ElMessage.success('更新成功')
    } else {
      const candidate = candidateOptions.value.find((c) => c.id === form.candidateId)
      await addInterviewApi({ ...form, requisitionId: candidate?.requisitionId || null })
      ElMessage.success('面试已安排，候选人阶段已同步推进')
    }
    dialogVisible.value = false
    loadData()
    loadCandidates()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除「${row.candidateName}」的这场面试记录吗？`, '提示', { type: 'warning' })
  await deleteInterviewApi(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const openResult = (row) => {
  currentRow.value = row
  resultForm.result = row.result === 0 ? 1 : row.result
  resultForm.score = row.score
  resultForm.comment = row.comment || ''
  resultVisible.value = true
}

const submitResult = async () => {
  submitting.value = true
  try {
    await submitInterviewResultApi(currentRow.value.id, { ...resultForm })
    ElMessage.success('结果已登记，候选人阶段已同步')
    resultVisible.value = false
    loadData()
    loadCandidates()
  } finally {
    submitting.value = false
  }
}

const roundText = (round) => ({ 1: '初试', 2: '复试', 3: '终试' }[round] || '面试')
const typeText = (type) => ({ 1: '现场', 2: '电话', 3: '视频' }[type] || '现场')
const resultText = (result) => ({ 0: '待面试', 1: '通过', 2: '未通过', 3: '爽约' }[result] || '待面试')
const resultType = (result) => ({ 0: 'info', 1: 'success', 2: 'danger', 3: 'warning' }[result] || 'info')
const formatTime = (time) => (time ? String(time).replace('T', ' ').slice(0, 16) : '-')

onMounted(async () => {
  await loadCandidates()
  await loadData()
})
</script>

<style scoped>
.page-container { width: 100%; }
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.tip { margin-left: 8px; font-size: 12px; color: #b1b3b8; }
</style>
