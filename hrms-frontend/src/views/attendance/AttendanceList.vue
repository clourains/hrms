<template>
  <div class="attendance-container">
    <el-card shadow="hover">
      <!-- 顶部打卡区 -->
      <div class="checkin-section">
        <div class="checkin-card">
          <div class="checkin-time">{{ currentTime }}</div>
          <div class="checkin-date">{{ currentDate }}</div>
          <div class="checkin-status">
            <el-tag :type="todayStatus.type" size="large">{{ todayStatus.text }}</el-tag>
          </div>
          <div class="checkin-buttons">
            <el-button
              type="primary"
              size="large"
              :icon="Clock"
              :disabled="hasCheckedIn"
              @click="handleCheckIn"
            >
              {{ hasCheckedIn ? '已打卡' : '上班打卡' }}
            </el-button>
            <el-button
              type="success"
              size="large"
              :icon="Finished"
              :disabled="!hasCheckedIn || hasCheckedOut"
              @click="handleCheckOut"
            >
              {{ hasCheckedOut ? '已签退' : '下班签退' }}
            </el-button>
          </div>
          <div class="checkin-records">
            <div class="record-item">
              <span class="record-label">上班打卡</span>
              <span class="record-value">{{ checkInTime || '--:--' }}</span>
            </div>
            <div class="record-item">
              <span class="record-label">下班签退</span>
              <span class="record-value">{{ checkOutTime || '--:--' }}</span>
            </div>
            <div class="record-item">
              <span class="record-label">工作时长</span>
              <span class="record-value">{{ workDuration || '--小时' }}</span>
            </div>
          </div>
        </div>

        <div class="stats-cards">
          <el-row :gutter="12">
            <el-col :span="12">
              <div class="stat-item">
                <div class="stat-num success">{{ monthStats.normalDays }}</div>
                <div class="stat-label">正常出勤</div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="stat-item">
                <div class="stat-num warning">{{ monthStats.lateDays }}</div>
                <div class="stat-label">迟到次数</div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="stat-item">
                <div class="stat-num danger">{{ monthStats.earlyDays }}</div>
                <div class="stat-label">早退次数</div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="stat-item">
                <div class="stat-num info">{{ monthStats.leaveDays }}</div>
                <div class="stat-label">请假天数</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="员工姓名">
            <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="部门">
            <el-select v-model="searchForm.departmentId" placeholder="请选择部门" clearable style="width: 160px">
              <el-option label="技术部" :value="1" />
              <el-option label="产品部" :value="2" />
              <el-option label="市场部" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="考勤日期">
            <el-date-picker
              v-model="searchForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 260px"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 140px">
              <el-option label="正常" value="normal" />
              <el-option label="迟到" value="late" />
              <el-option label="早退" value="early" />
              <el-option label="缺勤" value="absent" />
              <el-option label="请假" value="leave" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button type="warning" :icon="EditPen" @click="handleMakeup">补卡申请</el-button>
        <el-button type="success" :icon="Calendar" @click="handleLeave">请假申请</el-button>
        <el-button type="primary" :icon="Download" @click="handleExport">导出报表</el-button>
        <el-button :icon="TrendCharts" @click="handleStats">统计分析</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="date" label="日期" width="120" align="center" />
        <el-table-column prop="employeeNo" label="工号" width="100" align="center" />
        <el-table-column prop="name" label="姓名" width="100" align="center" />
        <el-table-column prop="department" label="部门" width="120" align="center" />
        <el-table-column prop="checkIn" label="上班打卡" width="120" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-warning': row.checkInStatus === 'late' }">{{ row.checkIn || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="checkOut" label="下班签退" width="120" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.checkOutStatus === 'early' }">{{ row.checkOut || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="workHours" label="工作时长" width="100" align="center" />
        <el-table-column prop="status" label="考勤状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button type="warning" link size="small" @click="handleApprove(row)">审批</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>

    <!-- 补卡申请弹窗 -->
    <el-dialog v-model="makeupDialogVisible" title="补卡申请" width="450px">
      <el-form :model="makeupForm" label-width="100px">
        <el-form-item label="补卡日期">
          <el-date-picker v-model="makeupForm.date" type="date" placeholder="请选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="补卡类型">
          <el-radio-group v-model="makeupForm.type">
            <el-radio label="checkIn">上班卡</el-radio>
            <el-radio label="checkOut">下班卡</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="补卡时间">
          <el-time-picker v-model="makeupForm.time" placeholder="请选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input v-model="makeupForm.reason" type="textarea" :rows="3" placeholder="请输入申请原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="makeupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMakeup">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 请假申请弹窗 -->
    <el-dialog v-model="leaveDialogVisible" title="请假申请" width="450px">
      <el-form :model="leaveForm" label-width="100px">
        <el-form-item label="请假类型">
          <el-select v-model="leaveForm.type" placeholder="请选择请假类型" style="width: 100%">
            <el-option label="年假" value="annual" />
            <el-option label="事假" value="personal" />
            <el-option label="病假" value="sick" />
            <el-option label="婚假" value="marriage" />
            <el-option label="产假" value="maternity" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="leaveForm.startDate" type="datetime" placeholder="请选择开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="leaveForm.endDate" type="datetime" placeholder="请选择结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="请假时长">
          <el-input v-model="leaveForm.duration" placeholder="自动计算" disabled />
        </el-form-item>
        <el-form-item label="请假事由">
          <el-input v-model="leaveForm.reason" type="textarea" :rows="3" placeholder="请输入请假事由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="leaveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLeave">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Clock, Finished, EditPen, Calendar, Download, TrendCharts } from '@element-plus/icons-vue'

const loading = ref(false)
const makeupDialogVisible = ref(false)
const leaveDialogVisible = ref(false)

// 当前时间
const currentTime = ref('')
const currentDate = ref('')
let timer = null

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  currentDate.value = now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
}

// 打卡状态
const hasCheckedIn = ref(true)
const hasCheckedOut = ref(false)
const checkInTime = ref('09:02:15')
const checkOutTime = ref('')
const workDuration = ref('')

const todayStatus = computed(() => {
  if (hasCheckedOut.value) return { type: 'success', text: '今日已完成打卡' }
  if (hasCheckedIn.value) {
    const isLate = checkInTime.value > '09:00:00'
    return { type: isLate ? 'warning' : 'success', text: isLate ? '已上班（迟到）' : '已上班' }
  }
  return { type: 'info', text: '未打卡' }
})

const monthStats = reactive({
  normalDays: 18,
  lateDays: 2,
  earlyDays: 1,
  leaveDays: 2
})

const searchForm = reactive({
  name: '',
  departmentId: '',
  dateRange: [],
  status: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 56
})

const tableData = ref([
  { id: 1, date: '2024-01-15', employeeNo: 'E001', name: '张三', department: '技术部', checkIn: '08:55:30', checkInStatus: 'normal', checkOut: '18:30:00', checkOutStatus: 'normal', workHours: '9.5h', status: 'normal', remark: '' },
  { id: 2, date: '2024-01-15', employeeNo: 'E002', name: '李四', department: '技术部', checkIn: '09:15:20', checkInStatus: 'late', checkOut: '18:20:00', checkOutStatus: 'normal', workHours: '9.1h', status: 'late', remark: '迟到15分钟' },
  { id: 3, date: '2024-01-15', employeeNo: 'E003', name: '王五', department: '产品部', checkIn: '08:50:00', checkInStatus: 'normal', checkOut: '17:30:00', checkOutStatus: 'early', workHours: '8.7h', status: 'early', remark: '早退1小时' },
  { id: 4, date: '2024-01-15', employeeNo: 'E004', name: '赵六', department: '市场部', checkIn: '--', checkInStatus: '', checkOut: '--', checkOutStatus: '', workHours: '--', status: 'absent', remark: '缺勤' },
  { id: 5, date: '2024-01-15', employeeNo: 'E005', name: '钱七', department: '运营部', checkIn: '09:00:00', checkInStatus: 'normal', checkOut: '18:00:00', checkOutStatus: 'normal', workHours: '9.0h', status: 'leave', remark: '下午请假半天' },
  { id: 6, date: '2024-01-14', employeeNo: 'E001', name: '张三', department: '技术部', checkIn: '08:58:00', checkInStatus: 'normal', checkOut: '19:00:00', checkOutStatus: 'normal', workHours: '10.0h', status: 'normal', remark: '加班1小时' },
  { id: 7, date: '2024-01-14', employeeNo: 'E002', name: '李四', department: '技术部', checkIn: '09:05:10', checkInStatus: 'late', checkOut: '18:10:00', checkOutStatus: 'normal', workHours: '9.1h', status: 'late', remark: '迟到5分钟' },
  { id: 8, date: '2024-01-14', employeeNo: 'E006', name: '孙八', department: '人事部', checkIn: '08:45:00', checkInStatus: 'normal', checkOut: '18:00:00', checkOutStatus: 'normal', workHours: '9.3h', status: 'normal', remark: '' },
  { id: 9, date: '2024-01-14', employeeNo: 'E007', name: '周九', department: '财务部', checkIn: '09:00:00', checkInStatus: 'normal', checkOut: '18:00:00', checkOutStatus: 'normal', workHours: '9.0h', status: 'normal', remark: '' },
  { id: 10, date: '2024-01-14', employeeNo: 'E008', name: '吴十', department: '行政部', checkIn: '08:55:00', checkInStatus: 'normal', checkOut: '18:05:00', checkOutStatus: 'normal', workHours: '9.2h', status: 'normal', remark: '' }
])

const makeupForm = reactive({
  date: '',
  type: 'checkIn',
  time: '',
  reason: ''
})

const leaveForm = reactive({
  type: '',
  startDate: '',
  endDate: '',
  duration: '',
  reason: ''
})

const getStatusText = (status) => {
  const map = {
    normal: '正常',
    late: '迟到',
    early: '早退',
    absent: '缺勤',
    leave: '请假'
  }
  return map[status] || '未知'
}

const getStatusTagType = (status) => {
  const map = {
    normal: 'success',
    late: 'warning',
    early: 'danger',
    absent: 'info',
    leave: 'primary'
  }
  return map[status] || 'info'
}

const handleCheckIn = () => {
  const now = new Date()
  checkInTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  hasCheckedIn.value = true
  ElMessage.success('上班打卡成功')
}

const handleCheckOut = () => {
  const now = new Date()
  checkOutTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  hasCheckedOut.value = true
  // 计算工作时长
  const inTime = checkInTime.value.split(':').map(Number)
  const outTime = checkOutTime.value.split(':').map(Number)
  const hours = outTime[0] - inTime[0] + (outTime[1] - inTime[1]) / 60
  workDuration.value = hours.toFixed(1) + '小时'
  ElMessage.success('下班签退成功')
}

const handleSearch = () => {
  ElMessage.success('搜索功能演示')
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = Array.isArray(searchForm[key]) ? [] : ''
  })
}

const handleMakeup = () => {
  makeupDialogVisible.value = true
}

const submitMakeup = () => {
  ElMessage.success('补卡申请已提交，等待审批')
  makeupDialogVisible.value = false
}

const handleLeave = () => {
  leaveDialogVisible.value = true
}

const submitLeave = () => {
  ElMessage.success('请假申请已提交，等待审批')
  leaveDialogVisible.value = false
}

const handleExport = () => {
  ElMessage.info('导出功能演示')
}

const handleStats = () => {
  ElMessage.info('统计分析功能演示')
}

const handleViewDetail = (row) => {
  ElMessage.info(`查看 ${row.name} 的考勤详情`)
}

const handleApprove = (row) => {
  ElMessage.info(`审批 ${row.name} 的考勤申请`)
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
.attendance-container {
  .checkin-section {
    display: flex;
    gap: 20px;
    margin-bottom: 24px;
    padding-bottom: 24px;
    border-bottom: 1px solid #ebeef5;
  }

  .checkin-card {
    flex: 1;
    background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
    border-radius: 12px;
    padding: 30px;
    color: #fff;
    text-align: center;

    .checkin-time {
      font-size: 48px;
      font-weight: 600;
      letter-spacing: 2px;
    }

    .checkin-date {
      font-size: 16px;
      opacity: 0.9;
      margin-top: 8px;
    }

    .checkin-status {
      margin: 20px 0;
    }

    .checkin-buttons {
      display: flex;
      justify-content: center;
      gap: 20px;
      margin-bottom: 24px;
    }

    .checkin-records {
      display: flex;
      justify-content: space-around;
      padding-top: 20px;
      border-top: 1px solid rgba(255, 255, 255, 0.2);

      .record-item {
        text-align: center;

        .record-label {
          font-size: 13px;
          opacity: 0.85;
          margin-bottom: 6px;
        }

        .record-value {
          font-size: 18px;
          font-weight: 500;
        }
      }
    }
  }

  .stats-cards {
    width: 280px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 12px;
    padding: 20px;

    .stat-item {
      text-align: center;
      padding: 15px 0;

      .stat-num {
        font-size: 28px;
        font-weight: 600;

        &.success { color: #67C23A; }
        &.warning { color: #E6A23C; }
        &.danger { color: #F56C6C; }
        &.info { color: #909399; }
      }

      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .search-bar {
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid #ebeef5;
  }

  .action-bar {
    margin-bottom: 16px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .text-warning {
    color: #E6A23C;
  }

  .text-danger {
    color: #F56C6C;
  }
}
</style>
