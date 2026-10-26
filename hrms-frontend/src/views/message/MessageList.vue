<template>
  <div class="message-container">
    <el-card shadow="hover">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="类型">
            <el-select v-model="searchForm.msgType" placeholder="全部类型" clearable style="width: 140px">
              <el-option label="系统" value="系统" />
              <el-option label="审批" value="审批" />
              <el-option label="薪资" value="薪资" />
              <el-option label="提醒" value="提醒" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键字">
            <el-input
              v-model="searchForm.keyword"
              placeholder="标题 / 内容"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button type="success" :icon="Check" :disabled="unreadCount === 0" @click="handleReadAll">
          全部已读{{ unreadCount > 0 ? `（${unreadCount}）` : '' }}
        </el-button>
        <el-button v-if="isAdmin" type="primary" :icon="Promotion" @click="openCompose">发消息</el-button>
      </div>

      <!-- 消息列表 -->
      <div v-loading="loading" class="msg-list">
        <el-empty v-if="rows.length === 0" description="暂无消息" />
        <div
          v-for="msg in rows"
          :key="msg.id"
          class="msg-item"
          :class="{ unread: msg.isRead === 0 }"
          @click="handleView(msg)"
        >
          <div class="msg-icon">
            <el-badge is-dot :hidden="msg.isRead === 1">
              <el-icon :size="22" :color="typeColor(msg.msgType)">
                <component :is="typeIcon(msg.msgType)" />
              </el-icon>
            </el-badge>
          </div>
          <div class="msg-body">
            <div class="msg-title">
              <el-tag :type="tagType(msg.msgType)" size="small" effect="plain">{{ msg.msgType }}</el-tag>
              <span class="title-text">{{ msg.title }}</span>
            </div>
            <div class="msg-content">{{ msg.content }}</div>
            <div class="msg-meta">
              <span>{{ msg.senderName || '系统' }}</span>
              <span>{{ formatTime(msg.createTime) }}</span>
              <span v-if="msg.isRead === 1" class="read-mark">已读</span>
            </div>
          </div>
          <div class="msg-actions">
            <el-button
              v-if="jumpTarget(msg)"
              type="primary"
              link
              size="small"
              :icon="Promotion"
              @click.stop="handleJump(msg)"
            >{{ jumpLabel(msg) }}</el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click.stop="handleDelete(msg)">删除</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 消息详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="currentMsg?.title || '消息详情'" size="420px">
      <template v-if="currentMsg">
        <div class="detail-meta">
          <el-tag :type="tagType(currentMsg.msgType)" size="small" effect="plain">{{ currentMsg.msgType }}</el-tag>
          <span>{{ currentMsg.senderName || '系统' }}</span>
          <span>{{ formatTime(currentMsg.createTime) }}</span>
        </div>
        <div class="detail-content">{{ currentMsg.content }}</div>
        <div v-if="jumpTarget(currentMsg)" class="detail-jump">
          <el-button type="primary" :icon="Promotion" @click="handleJump(currentMsg)">{{ jumpLabel(currentMsg) }}</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 发消息弹窗（管理员） -->
    <el-dialog v-model="composeVisible" title="发送站内信" width="560px" destroy-on-close>
      <el-form :model="composeForm" label-width="80px">
        <el-form-item label="接收人" required>
          <el-select
            v-model="composeForm.receivers"
            multiple
            filterable
            placeholder="选择接收人"
            style="width: 100%"
          >
            <el-option label="全体用户" value="all" />
            <el-option
              v-for="u in userOptions"
              :key="u.username"
              :label="`${u.nickname || u.username}（${u.role === 'admin' ? '管理员' : '员工'}）`"
              :value="u.username"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="composeForm.msgType">
            <el-radio-button value="系统">系统</el-radio-button>
            <el-radio-button value="审批">审批</el-radio-button>
            <el-radio-button value="提醒">提醒</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="composeForm.title" placeholder="请输入标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="composeForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入消息内容"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="composeVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendLoading" @click="handleSend">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Check, Delete, Promotion, Bell, EditPen, Warning } from '@element-plus/icons-vue'
import { messageJumpTarget as jumpTarget, messageJumpLabel as jumpLabel } from '@/utils/messageJump'
import {
  getMessageInboxApi,
  getUnreadCountApi,
  markMessageReadApi,
  readAllMessagesApi,
  deleteMessageApi,
  getMessageUsersApi,
  sendMessageApi
} from '@/api/message'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'admin')

const loading = ref(false)
const rows = ref([])
const unreadCount = ref(0)
const userOptions = ref([])

const searchForm = reactive({
  msgType: '',
  keyword: ''
})

const detailVisible = ref(false)
const currentMsg = ref(null)

const composeVisible = ref(false)
const sendLoading = ref(false)
const composeForm = reactive({
  receivers: [],
  msgType: '系统',
  title: '',
  content: ''
})

const typeIcon = (type) => {
  if (type === '审批') return EditPen
  if (type === '提醒') return Warning
  return Bell
}

const typeColor = (type) => {
  if (type === '审批') return '#E6A23C'
  if (type === '提醒') return '#F56C6C'
  return '#409EFF'
}

const tagType = (type) => {
  if (type === '审批') return 'warning'
  if (type === '提醒') return 'danger'
  return 'primary'
}

const formatTime = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {}
    if (searchForm.msgType) params.msgType = searchForm.msgType
    if (searchForm.keyword) params.keyword = searchForm.keyword
    const res = await getMessageInboxApi(params)
    rows.value = res.data || []
    await refreshUnread()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}

const refreshUnread = async () => {
  try {
    const res = await getUnreadCountApi()
    unreadCount.value = Number(res.data || 0)
  } catch (e) {
    // 忽略
  }
}

const handleSearch = () => loadData()

const handleReset = () => {
  searchForm.msgType = ''
  searchForm.keyword = ''
  loadData()
}

const handleView = async (msg) => {
  currentMsg.value = msg
  detailVisible.value = true
  if (msg.isRead === 0) {
    try {
      await markMessageReadApi(msg.id)
      msg.isRead = 1
      await refreshUnread()
    } catch (e) {
      // 忽略
    }
  }
}

// 快速通道：标记已读后直达处理页面
const router = useRouter()
const handleJump = async (msg) => {
  const target = jumpTarget(msg)
  if (!target) return
  detailVisible.value = false
  if (msg.isRead === 0) {
    try {
      await markMessageReadApi(msg.id)
      msg.isRead = 1
      await refreshUnread()
    } catch (e) {
      // 忽略
    }
  }
  router.push(target)
}

const handleReadAll = async () => {
  try {
    await readAllMessagesApi()
    ElMessage.success('已全部标记为已读')
    loadData()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

const handleDelete = async (msg) => {
  try {
    await ElMessageBox.confirm('确定删除这条消息吗？', '提示', { type: 'warning' })
    await deleteMessageApi(msg.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // 用户取消或接口报错
  }
}

const openCompose = async () => {
  composeForm.receivers = []
  composeForm.msgType = '系统'
  composeForm.title = ''
  composeForm.content = ''
  composeVisible.value = true
  try {
    const res = await getMessageUsersApi()
    userOptions.value = (res.data || []).filter(u => u.username !== userStore.username)
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

const handleSend = async () => {
  if (composeForm.receivers.length === 0) {
    ElMessage.warning('请选择接收人')
    return
  }
  if (!composeForm.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!composeForm.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  sendLoading.value = true
  try {
    const res = await sendMessageApi({ ...composeForm })
    ElMessage.success(`发送成功，已送达 ${res.data?.count ?? composeForm.receivers.length} 人`)
    composeVisible.value = false
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    sendLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.message-container {
  .search-bar {
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid #ebeef5;
  }

  .action-bar {
    margin-bottom: 16px;
  }

  .msg-list {
    min-height: 200px;
  }

  .msg-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: background-color 0.2s;

    &:hover {
      background-color: #f5f7fa;
    }

    &.unread {
      background-color: #f0f7ff;

      &:hover {
        background-color: #e8f3ff;
      }

      .title-text {
        font-weight: 600;
      }
    }

    & + .msg-item {
      margin-top: 4px;
    }
  }

  .msg-icon {
    padding-top: 2px;
  }

  .msg-body {
    flex: 1;
    min-width: 0;
  }

  .msg-title {
    display: flex;
    align-items: center;
    gap: 8px;

    .title-text {
      font-size: 14px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .msg-content {
    font-size: 13px;
    color: #606266;
    margin-top: 6px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .msg-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: #909399;
    margin-top: 6px;

    .read-mark {
      color: #c0c4cc;
    }
  }

  .msg-actions {
    flex: none;
  }

  .detail-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 13px;
    color: #909399;
    padding-bottom: 12px;
    border-bottom: 1px solid #ebeef5;
  }

  .detail-content {
    font-size: 14px;
    color: #303133;
    line-height: 1.8;
    white-space: pre-wrap;
    padding-top: 16px;
  }

  .detail-jump {
    padding-top: 20px;
  }
}
</style>
