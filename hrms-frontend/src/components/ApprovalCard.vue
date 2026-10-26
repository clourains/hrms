<template>
  <div class="appr-card" :class="{ highlight }" @click="$emit('view', row.id)">
    <div class="appr-head">
      <el-tag size="small" effect="plain">{{ row.bizType }}</el-tag>
      <span class="appr-title">{{ row.title }}</span>
      <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
    </div>
    <div class="appr-info">
      <span>申请人：{{ row.applicantName }}</span>
      <span>进度：第 {{ row.currentNode }}/{{ row.nodeCount }} 级</span>
      <span>{{ formatTime(row.createTime) }}</span>
    </div>
    <div class="appr-chain">
      <span v-for="(rec, i) in row.records" :key="rec.id" class="chain-node">
        <template v-if="i > 0">→ </template>{{ rec.approverName }}
        <el-tag size="small" :type="recType(rec.status)" class="chain-tag">{{ recText(rec.status) }}</el-tag>
      </span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  row: { type: Object, required: true },
  highlight: { type: Boolean, default: false }
})

defineEmits(['view'])

const statusText = (s) => ({ 0: '审批中', 1: '已通过', 2: '已驳回', 3: '已撤销' }[s] || '未知')
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }[s] || 'info')
const recText = (s) => ({ 0: '待审', 1: '通过', 2: '驳回' }[s] || '')
const recType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')
const formatTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 16) : '')
</script>

<style scoped lang="scss">
.appr-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
  }

  &.highlight {
    border-left: 3px solid #e6a23c;
    background-color: #fdf6ec;
  }
}

.appr-head {
  display: flex;
  align-items: center;
  gap: 8px;

  .appr-title {
    font-weight: 600;
    color: #303133;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.appr-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.appr-chain {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;

  .chain-tag {
    margin: 0 4px;
  }
}
</style>
