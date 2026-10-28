<template>
  <div class="notice-center page-stack">
    <PageHeader
      title="公告中心"
      description="全公司与本部门的通知都会汇总到这里；置顶和重要公告优先展示，打开即标记已读"
      icon="Notification"
    >
      <template #actions>
        <el-button :icon="Refresh" @click="loadNotices">刷新</el-button>
        <el-button v-if="isAdmin" type="primary" :icon="Plus" @click="openCreateDialog">发布公告</el-button>
      </template>
    </PageHeader>

    <div class="search-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="标题 / 内容" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item v-if="!isAdmin" label="阅读状态">
          <el-radio-group v-model="readFilter">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="unread">未读</el-radio-button>
            <el-radio-button value="read">已读</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <span class="summary-hint">
            共 {{ notices.length }} 条 · 未读 {{ unreadCount }} 条
          </span>
        </el-form-item>
      </el-form>
    </div>

    <div v-loading="loading" class="notice-list">
      <div
        v-for="n in filteredNotices"
        :key="n.id"
        class="notice-row"
        :class="{ 'is-pinned': n.pinned === 1, 'is-read': n.isRead === 1 }"
        @click="openNotice(n)"
      >
        <div class="row-main">
          <div class="row-title">
            <el-tag v-if="n.pinned === 1" type="danger" size="small" effect="dark">置顶</el-tag>
            <el-tag v-if="n.level === 'important'" type="danger" size="small" effect="plain">重要</el-tag>
            <el-tag v-if="isAdmin && n.status !== 1" type="info" size="small">已下线</el-tag>
            <el-tag v-if="isAdmin && n.scopeType === 1" type="info" size="small" effect="plain">指定部门</el-tag>
            <el-tag v-if="n.isRead === 0" type="warning" size="small" effect="plain">未读</el-tag>
            <span class="title-text">{{ n.title }}</span>
          </div>
          <div class="row-preview">{{ n.content }}</div>
        </div>
        <div class="row-meta">
          <span>{{ n.publisher || '系统' }}</span>
          <span>{{ formatTime(n.createTime) }}</span>
          <div v-if="isAdmin" class="row-actions">
            <el-button link type="primary" size="small" @click.stop="openEditDialog(n)">编辑</el-button>
            <el-button
              link
              :type="n.status === 1 ? 'warning' : 'success'"
              size="small"
              @click.stop="handleToggleStatus(n)"
            >{{ n.status === 1 ? '下线' : '上线' }}</el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(n)">删除</el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && filteredNotices.length === 0" description="暂无符合条件的公告" />
    </div>

    <el-dialog v-model="detailVisible" :title="current?.title || '公告详情'" width="640px" destroy-on-close>
      <template v-if="current">
        <div class="detail-meta">
          <span>{{ current.publisher || '系统' }} 发布</span>
          <span>{{ formatTime(current.createTime) }}</span>
          <span v-if="current.scopeType === 1">指定部门可见</span>
          <span v-else>全员可见</span>
        </div>
        <div class="detail-content">{{ current.content }}</div>
      </template>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">知道了</el-button>
      </template>
    </el-dialog>

    <!-- 管理员：发布 / 编辑公告 -->
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
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="公告正文" />
        </el-form-item>
        <el-form-item label="发布范围">
          <el-radio-group v-model="form.scopeType">
            <el-radio :value="0">全员</el-radio>
            <el-radio :value="1">指定部门</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.scopeType === 1" label="可见部门">
          <el-select v-model="deptIdsArray" multiple placeholder="请选择部门" style="width: 100%">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker
            v-model="form.effectiveRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始（留空=立即）"
            end-placeholder="结束（留空=长期）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.pinned" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存' : '发布' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { useUserStore } from '@/store/user'
import {
  getActiveNoticesApi, readNoticeApi, getNoticeListApi,
  createNoticeApi, updateNoticeApi, deleteNoticeApi
} from '@/api/notice'
import { getDepartmentListApi } from '@/api/department'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'admin')

const loading = ref(false)
const notices = ref([])
const keyword = ref('')
const readFilter = ref('all')
const detailVisible = ref(false)
const current = ref(null)

const unreadCount = computed(() => notices.value.filter(n => n.isRead === 0).length)

const filteredNotices = computed(() => {
  const kw = keyword.value.trim()
  return notices.value.filter(n => {
    if (readFilter.value === 'unread' && n.isRead !== 0) return false
    if (readFilter.value === 'read' && n.isRead !== 1) return false
    if (!kw) return true
    return `${n.title || ''}${n.content || ''}`.includes(kw)
  })
})

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

const loadNotices = async () => {
  loading.value = true
  try {
    if (isAdmin.value) {
      // 管理员看全部公告（含已下线/未生效），便于直接管理
      const res = await getNoticeListApi({ current: 1, size: 100 })
      notices.value = res.data?.records || []
    } else {
      const res = await getActiveNoticesApi()
      notices.value = res.data || []
    }
  } catch (e) {
    // 拦截器统一提示
  } finally {
    loading.value = false
  }
}

const openNotice = async (n) => {
  current.value = n
  detailVisible.value = true
  if (n.isRead === 1) return
  try {
    await readNoticeApi(n.id)
    n.isRead = 1
  } catch (e) {
    // 已读记录失败不阻断阅读
  }
}

/* ---------- 管理员：发布 / 编辑 / 上下线 / 删除 ---------- */
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const deptIdsArray = ref([])
const deptOptions = ref([])
const form = reactive({ id: null, title: '', content: '', level: 'normal', scopeType: 0, pinned: 0, effectiveRange: [] })
const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const openCreateDialog = () => {
  isEdit.value = false
  Object.assign(form, { id: null, title: '', content: '', level: 'normal', scopeType: 0, pinned: 0, effectiveRange: [] })
  deptIdsArray.value = []
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    title: row.title,
    content: row.content || '',
    level: row.level || 'normal',
    scopeType: row.scopeType === 1 ? 1 : 0,
    pinned: row.pinned === 1 ? 1 : 0,
    effectiveRange: (row.effectiveStart || row.effectiveEnd)
      ? [row.effectiveStart || '', row.effectiveEnd || ''].map(v => (v ? String(v).replace('T', ' ').slice(0, 19) : null))
      : []
  })
  deptIdsArray.value = row.deptIds ? String(row.deptIds).split(',').filter(Boolean).map(Number) : []
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (form.scopeType === 1 && deptIdsArray.value.length === 0) {
    ElMessage.warning('指定部门发布时请至少选择一个部门')
    return
  }
  const range = form.effectiveRange || []
  if (range[0] && range[1] && range[1] <= range[0]) {
    ElMessage.warning('生效结束时间不能早于开始时间')
    return
  }
  const payload = {
    id: form.id,
    title: form.title,
    content: form.content,
    level: form.level,
    scopeType: form.scopeType,
    deptIds: form.scopeType === 1 ? deptIdsArray.value.join(',') : null,
    effectiveStart: range[0] || null,
    effectiveEnd: range[1] || null,
    pinned: form.pinned
  }
  submitting.value = true
  try {
    const res = isEdit.value ? await updateNoticeApi(payload) : await createNoticeApi(payload)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '已保存' : '发布成功')
      dialogVisible.value = false
      loadNotices()
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
    loadNotices()
  } else {
    ElMessage.error(res.message || '操作失败')
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
      loadNotices()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadNotices()
  if (isAdmin.value) {
    getDepartmentListApi().then(res => { deptOptions.value = res.data || [] }).catch(() => {})
  }
})
</script>

<style scoped lang="scss">
.notice-center {
  .summary-hint {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .notice-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .notice-row {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    padding: 16px 20px;
    border-radius: 10px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    cursor: pointer;
    transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;

    &:hover {
      border-color: var(--el-color-primary-light-5);
      box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
      transform: translateY(-1px);
    }

    &.is-pinned {
      border-left: 3px solid var(--el-color-danger);
    }

    &.is-read {
      opacity: 0.82;
    }

    .row-main {
      flex: 1;
      min-width: 0;
    }

    .row-title {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .title-text {
        font-size: 15px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }
    }

    .row-preview {
      margin-top: 6px;
      font-size: 13px;
      color: var(--el-text-color-regular);
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .row-meta {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 6px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      white-space: nowrap;
    }

    .row-actions {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .detail-meta {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-bottom: 12px;
  }

  .detail-content {
    font-size: 14px;
    line-height: 1.8;
    color: var(--el-text-color-primary);
    white-space: pre-wrap;
    word-break: break-word;
    max-height: 52vh;
    overflow-y: auto;
  }
}
</style>
