<template>
  <div class="approval-container page-stack">
    <!-- 页头 -->
    <PageHeader
      title="审批中心"
      description="请假、加班、调薪等申请的多级审批处理与进度跟踪"
      icon="DocumentChecked"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openApply">发起申请</el-button>
        <el-button :icon="Refresh" @click="loadActiveTab">刷新</el-button>
      </template>
    </PageHeader>

    <el-card shadow="hover">
      <el-tabs v-model="activeTab" @tab-change="loadActiveTab">
        <!-- 待我审批 -->
        <el-tab-pane name="todo">
          <template #label>
            <el-badge :value="todoCount" :hidden="todoCount === 0">待我审批</el-badge>
          </template>
          <el-empty v-if="todoList.length === 0" description="暂无待审批事项" />
          <ApprovalCard
            v-for="row in todoList"
            :key="row.id"
            :row="row"
            highlight
            @view="openDetail(row.id)"
          />
        </el-tab-pane>

        <!-- 我的申请 -->
        <el-tab-pane name="mine" lazy>
          <el-empty v-if="myList.length === 0" description="暂无申请记录" />
          <ApprovalCard
            v-for="row in myList"
            :key="row.id"
            :row="row"
            @view="openDetail(row.id)"
          />
        </el-tab-pane>

        <!-- 我已处理 -->
        <el-tab-pane name="done" lazy>
          <el-empty v-if="doneList.length === 0" description="暂无已处理事项" />
          <ApprovalCard
            v-for="row in doneList"
            :key="row.id"
            :row="row"
            @view="openDetail(row.id)"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="审批详情" size="480px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <h3 class="detail-title">{{ detail.title }}</h3>
          <div class="detail-meta">
            <el-tag size="small" effect="plain">{{ detail.bizType }}</el-tag>
            <el-tag size="small" :type="instanceTagType(detail.status)">{{ instanceText(detail.status) }}</el-tag>
            <span>第 {{ detail.currentNode }}/{{ detail.nodeCount }} 级</span>
            <span>{{ formatTime(detail.createTime) }}</span>
          </div>

          <!-- 申请内容 -->
          <div class="detail-section">
            <div class="section-label">申请人</div>
            <div>{{ detail.applicantName }}（{{ detail.applicant }}）</div>
          </div>
          <div v-if="leaveInfo" class="detail-section">
            <div class="section-label">请假信息</div>
            <div class="leave-grid">
              <span>开始：{{ leaveInfo.startDate }}</span>
              <span>结束：{{ leaveInfo.endDate }}</span>
              <span>天数：{{ leaveInfo.days }}</span>
              <span>事由：{{ leaveInfo.reason || '-' }}</span>
            </div>
          </div>
          <div v-else-if="overtimeInfo" class="detail-section">
            <div class="section-label">加班信息（通过后计入薪资加班费）</div>
            <div class="leave-grid">
              <span>加班日期：{{ overtimeInfo.overtimeDate }}</span>
              <span>时长：{{ overtimeInfo.hours }} 小时</span>
              <span>事由：{{ overtimeInfo.reason || '-' }}</span>
            </div>
          </div>
          <div v-else-if="detail.detail" class="detail-section">
            <div class="section-label">申请内容</div>
            <div class="detail-text">{{ detail.detail }}</div>
          </div>

          <!-- 节点进度 -->
          <div class="detail-section">
            <div class="section-label">审批进度</div>
            <div class="node-list">
              <div v-for="rec in detail.records" :key="rec.id" class="node-item">
                <div class="node-left">
                  <span class="node-order">第{{ rec.nodeOrder }}级</span>
                  <span class="node-name">{{ rec.approverName }}</span>
                </div>
                <div class="node-right">
                  <el-tag size="small" :type="recordTagType(rec.status)">{{ recordText(rec.status) }}</el-tag>
                  <span v-if="rec.approveTime" class="node-time">{{ formatTime(rec.approveTime) }}</span>
                </div>
                <div v-if="rec.comment" class="node-comment">意见：{{ rec.comment }}</div>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="detail-actions">
            <template v-if="detail.canApprove">
              <el-input
                v-model="opComment"
                type="textarea"
                :rows="2"
                placeholder="审批意见（选填，驳回建议填写）"
                maxlength="200"
                style="margin-bottom: 12px"
              />
              <el-button type="success" @click="handleApprove">通过</el-button>
              <el-button type="danger" @click="handleReject">驳回</el-button>
            </template>
            <el-button v-if="detail.canCancel" type="warning" @click="handleCancel">撤销申请</el-button>
          </div>
        </template>
      </div>
    </el-drawer>

    <!-- 发起申请弹窗 -->
    <el-dialog v-model="applyVisible" title="发起申请" width="560px" destroy-on-close>
      <el-form :model="applyForm" label-width="80px">
        <el-form-item label="类型" required>
          <el-radio-group v-model="applyForm.bizType">
            <el-radio-button value="请假">请假</el-radio-button>
            <el-radio-button value="调薪">调薪</el-radio-button>
            <el-radio-button value="通用">通用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="applyForm.title" placeholder="如：十月年假申请" maxlength="100" show-word-limit />
        </el-form-item>

        <!-- 请假明细 -->
        <template v-if="applyForm.bizType === '请假'">
          <el-form-item label="开始日期" required>
            <el-date-picker v-model="applyForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
          </el-form-item>
          <el-form-item label="结束日期" required>
            <el-date-picker v-model="applyForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
          </el-form-item>
          <el-form-item label="请假天数">
            <el-input-number v-model="applyForm.days" :min="0.5" :step="0.5" :max="365" />
          </el-form-item>
        </template>

        <el-form-item :label="applyForm.bizType === '请假' ? '事由' : '内容'" required>
          <el-input
            v-model="applyForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请填写申请事由"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="审批人" required>
          <el-select
            v-model="applyForm.approvers"
            multiple
            filterable
            :multiple-limit="3"
            placeholder="逐级审批人（1-3级，按选择顺序）"
            style="width: 100%"
          >
            <el-option
              v-for="u in approverOptions"
              :key="u.username"
              :label="`${u.nickname || u.username}（${u.role === 'admin' ? '管理员' : '员工'}）`"
              :value="u.username"
            />
          </el-select>
          <div class="approver-hint">将按选择顺序逐级审批：{{ approverChain || '未选择' }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applyLoading" @click="handleApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import ApprovalCard from '@/components/ApprovalCard.vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  applyApprovalApi,
  approveApprovalApi,
  rejectApprovalApi,
  cancelApprovalApi,
  getMyApplyApi,
  getApprovalTodoApi,
  getApprovalDoneApi,
  getApprovalDetailApi,
  getApproverOptionsApi
} from '@/api/approval'

// 支持消息快速通道深链：/approval?tab=todo|mine|done
const route = useRoute()
const validTabs = ['todo', 'mine', 'done']
const activeTab = ref(validTabs.includes(route.query.tab) ? route.query.tab : 'todo')
watch(() => route.query.tab, (tab) => {
  if (validTabs.includes(tab) && tab !== activeTab.value) {
    activeTab.value = tab
    loadActiveTab()
  }
})
const todoList = ref([])
const myList = ref([])
const doneList = ref([])
const todoCount = ref(0)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const opComment = ref('')

const applyVisible = ref(false)
const applyLoading = ref(false)
const approverOptions = ref([])
const applyForm = reactive({
  bizType: '请假',
  title: '',
  startDate: '',
  endDate: '',
  days: 1,
  reason: '',
  approvers: []
})

const leaveInfo = computed(() => {
  if (!detail.value || detail.value.bizType !== '请假' || !detail.value.detail) return null
  try {
    const obj = JSON.parse(detail.value.detail)
    return obj && (obj.startDate || obj.reason) ? obj : null
  } catch (e) {
    return null
  }
})

const approverChain = computed(() => {
  return applyForm.approvers
    .map(u => {
      const opt = approverOptions.value.find(o => o.username === u)
      return opt ? (opt.nickname || opt.username) : u
    })
    .join(' → ')
})

const overtimeInfo = computed(() => {
  if (!detail.value || detail.value.bizType !== '加班' || !detail.value.detail) return null
  try {
    return JSON.parse(detail.value.detail)
  } catch (e) {
    return null
  }
})

const formatTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 16) : '')
const instanceText = (s) => ({ 0: '审批中', 1: '已通过', 2: '已驳回', 3: '已撤销' }[s] || '未知')
const instanceTagType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }[s] || 'info')
const recordText = (s) => ({ 0: '待审', 1: '通过', 2: '驳回' }[s] || '')
const recordTagType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')

const loadTodo = async () => {
  try {
    const res = await getApprovalTodoApi()
    todoList.value = res.data || []
    todoCount.value = todoList.value.length
  } catch (e) { /* 拦截器已提示 */ }
}

const loadMine = async () => {
  try {
    const res = await getMyApplyApi()
    myList.value = res.data || []
  } catch (e) { /* 拦截器已提示 */ }
}

const loadDone = async () => {
  try {
    const res = await getApprovalDoneApi()
    doneList.value = res.data || []
  } catch (e) { /* 拦截器已提示 */ }
}

const loadActiveTab = () => {
  if (activeTab.value === 'todo') loadTodo()
  else if (activeTab.value === 'mine') loadMine()
  else loadDone()
}

const openDetail = async (id) => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  opComment.value = ''
  try {
    const res = await getApprovalDetailApi(id)
    const data = res.data || {}
    detail.value = { ...(data.instance || {}), canApprove: data.canApprove, canCancel: data.canCancel, records: data.records || [] }
  } catch (e) {
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

const handleApprove = async () => {
  try {
    const res = await approveApprovalApi(detail.value.id, opComment.value)
    ElMessage.success(res.message || '已通过')
    detailVisible.value = false
    loadActiveTab()
  } catch (e) { /* 拦截器已提示 */ }
}

const handleReject = async () => {
  try {
    await ElMessageBox.confirm('确定驳回该申请吗？驳回后整单结束。', '驳回确认', { type: 'warning' })
  } catch (e) { return }
  try {
    const res = await rejectApprovalApi(detail.value.id, opComment.value)
    ElMessage.success(res.message || '已驳回')
    detailVisible.value = false
    loadActiveTab()
  } catch (e) { /* 拦截器已提示 */ }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定撤销该申请吗？', '撤销确认', { type: 'warning' })
  } catch (e) { return }
  try {
    const res = await cancelApprovalApi(detail.value.id)
    ElMessage.success(res.message || '已撤销')
    detailVisible.value = false
    loadActiveTab()
  } catch (e) { /* 拦截器已提示 */ }
}

const openApply = async () => {
  Object.assign(applyForm, { bizType: '请假', title: '', startDate: '', endDate: '', days: 1, reason: '', approvers: [] })
  applyVisible.value = true
  try {
    const res = await getApproverOptionsApi()
    approverOptions.value = res.data || []
  } catch (e) { /* 拦截器已提示 */ }
}

const handleApply = async () => {
  const f = applyForm
  if (!f.title.trim()) { ElMessage.warning('请输入标题'); return }
  if (f.bizType === '请假' && (!f.startDate || !f.endDate)) {
    ElMessage.warning('请选择请假起止日期'); return
  }
  if (!f.reason.trim()) { ElMessage.warning('请填写事由/内容'); return }
  if (f.approvers.length === 0) { ElMessage.warning('请选择审批人'); return }
  applyLoading.value = true
  try {
    const detailPayload = f.bizType === '请假'
      ? { startDate: f.startDate, endDate: f.endDate, days: f.days, reason: f.reason }
      : f.reason
    await applyApprovalApi({ bizType: f.bizType, title: f.title, detail: detailPayload, approvers: f.approvers })
    ElMessage.success('申请已提交，已通知第一级审批人')
    applyVisible.value = false
    activeTab.value = 'mine'
    loadMine()
  } catch (e) { /* 拦截器已提示 */ } finally {
    applyLoading.value = false
  }
}

onMounted(loadTodo)
</script>

<style scoped lang="scss">
.approval-container {
  .detail-title {
    margin: 0 0 8px;
    color: var(--el-text-color-primary);
  }

  .detail-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #909399;
    padding-bottom: 12px;
    border-bottom: 1px solid #ebeef5;
    margin-bottom: 12px;
  }

  .detail-section {
    margin-bottom: 16px;

    .section-label {
      font-size: 12px;
      color: #909399;
      margin-bottom: 6px;
    }
  }

  .leave-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 6px;
    font-size: 13px;
    color: #303133;
  }

  .detail-text {
    font-size: 13px;
    color: #303133;
    line-height: 1.6;
    white-space: pre-wrap;
    background: #f5f7fa;
    border-radius: 6px;
    padding: 10px;
  }

  .node-item {
    padding: 10px;
    border: 1px solid #ebeef5;
    border-radius: 6px;
    margin-bottom: 8px;

    .node-left {
      display: flex;
      gap: 8px;
      align-items: center;
    }

    .node-order {
      font-size: 12px;
      color: #909399;
    }

    .node-name {
      font-size: 13px;
      font-weight: 500;
      color: #303133;
    }

    .node-right {
      display: flex;
      gap: 8px;
      align-items: center;
      margin-top: 4px;
    }

    .node-time {
      font-size: 12px;
      color: #c0c4cc;
    }

    .node-comment {
      font-size: 12px;
      color: #606266;
      margin-top: 6px;
      background: #f5f7fa;
      border-radius: 4px;
      padding: 6px;
    }
  }

  .detail-actions {
    margin-top: 20px;
  }

  .approver-hint {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}
</style>
