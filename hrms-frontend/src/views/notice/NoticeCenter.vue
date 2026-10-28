<template>
  <div class="notice-center page-stack">
    <PageHeader
      title="公告中心"
      description="全公司与本部门的通知都会汇总到这里；置顶和重要公告优先展示，打开即标记已读"
      icon="Notification"
    >
      <template #actions>
        <el-button :icon="Refresh" @click="loadNotices">刷新</el-button>
      </template>
    </PageHeader>

    <div class="search-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="标题 / 内容" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="阅读状态">
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
            <el-tag v-if="n.isRead === 0" type="warning" size="small" effect="plain">未读</el-tag>
            <span class="title-text">{{ n.title }}</span>
          </div>
          <div class="row-preview">{{ n.content }}</div>
        </div>
        <div class="row-meta">
          <span>{{ n.publisher || '系统' }}</span>
          <span>{{ formatTime(n.createTime) }}</span>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getActiveNoticesApi, readNoticeApi } from '@/api/notice'

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
    const res = await getActiveNoticesApi()
    notices.value = res.data || []
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

onMounted(loadNotices)
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
