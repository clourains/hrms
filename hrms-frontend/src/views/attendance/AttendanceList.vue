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
              <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
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
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
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

    <!-- 导出月报弹窗（P1-3） -->
    <el-dialog v-model="exportDialogVisible" title="导出考勤月报" width="420px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="考勤月份">
          <el-date-picker v-model="exportForm.month" type="month" value-format="YYYY-MM" placeholder="请选择月份" style="width: 100%" />
        </el-form-item>
        <el-form-item label="导出范围">
          <el-select v-model="exportForm.deptId" placeholder="全部部门" clearable style="width: 100%">
            <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="exportLoading" @click="confirmExport">导出 Excel</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Clock, Finished, EditPen, Calendar, Download, TrendCharts } from '@element-plus/icons-vue'
import { getAttendanceListApi, getAttendanceStatsApi, checkInApi, checkOutApi } from '@/api/attendance'
import { request } from '@/utils/request'
import { getEmployeeListApi } from '@/api/employee'
import { getDepartmentListApi } from '@/api/department'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

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

// 打卡状态：P042 由当日真实考勤记录驱动
const hasCheckedIn = ref(false)
const hasCheckedOut = ref(false)
const checkInTime = ref('')
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

// P042：月度统计改为真实接口数据（后端统计无请假维度，leaveDays 恒为 0）
const monthStats = reactive({
  normalDays: 0,
  lateDays: 0,
  earlyDays: 0,
  leaveDays: 0
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
  total: 0
})

// P042：考勤列表改为真实接口加载（后端仅支持按员工+月份查询，即查看本人的月度考勤）
const allRows = ref([])
const tableData = ref([])
const departmentOptions = ref([])
let deptNameMap = {}
// 当前登录人对应的员工信息，用于展示工号/姓名/部门列
const myEmp = ref({ empNo: '-', name: '-', departmentName: '-' })

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

const pad = (n) => String(n).padStart(2, '0')
const fmtMonth = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
const fmtDate = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const timePart = (value) => (value ? String(value).slice(11, 19) : '')

// 后端考勤状态：0正常 1迟到 2早退 3迟到+早退；其余（如休息日）无打卡时间按缺勤展示
const mapStatus = (att) => {
  const s = att.status
  if (s === 1) return 'late'
  if (s === 2) return 'early'
  if (s === 3) return 'late'
  if (s === 0 || s == null) return att.checkInTime ? 'normal' : 'absent'
  return 'absent'
}

const normalizeRow = (att) => ({
  id: att.id,
  date: String(att.attendDate || '').slice(0, 10),
  employeeNo: myEmp.value.empNo,
  name: myEmp.value.name,
  department: myEmp.value.departmentName,
  checkIn: timePart(att.checkInTime),
  checkInStatus: att.status === 1 || att.status === 3 ? 'late' : 'normal',
  checkOut: timePart(att.checkOutTime),
  checkOutStatus: att.status === 2 || att.status === 3 ? 'early' : 'normal',
  workHours: att.workHours != null ? `${att.workHours}h` : '--',
  status: mapStatus(att),
  remark: att.remark || ''
})

const loadBase = async () => {
  try {
    const [empRes, deptRes] = await Promise.all([getEmployeeListApi(), getDepartmentListApi()])
    const depts = deptRes.data || []
    deptNameMap = {}
    depts.forEach(d => { deptNameMap[d.id] = d.deptName })
    departmentOptions.value = depts.map(d => ({ id: d.id, name: d.deptName }))
    const myEmpId = Number(userStore.userInfo.empId ?? userStore.userInfo.id)
    const me = (empRes.data || []).find(e => e.id === myEmpId)
    myEmp.value = me
      ? { empNo: me.empNo || '-', name: me.name || '-', departmentName: deptNameMap[me.deptId] || '-' }
      : { empNo: '-', name: userStore.userInfo.nickname || '-', departmentName: '-' }
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

// 查询月份：取日期范围的起始月，未选则当前月
const queryMonth = () => {
  const range = searchForm.dateRange
  if (range && range[0]) return fmtMonth(new Date(range[0]))
  return fmtMonth(new Date())
}

const loadData = async () => {
  loading.value = true
  try {
    const empId = userStore.userInfo.empId ?? userStore.userInfo.id
    const month = queryMonth()
    const [listRes, statsRes] = await Promise.all([
      getAttendanceListApi({ empId, month }),
      getAttendanceStatsApi({ empId, month })
    ])
    allRows.value = (listRes.data || []).map(normalizeRow)
    const stats = statsRes.data || {}
    monthStats.normalDays = stats.normalCount || 0
    monthStats.lateDays = stats.lateCount || 0
    monthStats.earlyDays = stats.earlyLeaveCount || 0
    syncTodayCard()
    refreshView()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}

/** 用今日打卡记录同步顶部打卡卡片状态 */
const syncTodayCard = () => {
  const today = fmtDate(new Date())
  const row = allRows.value.find(r => r.date === today)
  if (row) {
    hasCheckedIn.value = !!row.checkIn
    checkInTime.value = row.checkIn
    hasCheckedOut.value = !!row.checkOut
    checkOutTime.value = row.checkOut
    workDuration.value = row.workHours !== '--' ? row.workHours.replace('h', '小时') : ''
  }
}

/** 前端条件过滤 + 分页切片（后端列表接口仅支持员工+月份条件） */
const refreshView = () => {
  let rows = allRows.value
  const range = searchForm.dateRange || []
  const start = range[0] ? fmtDate(new Date(range[0])) : ''
  const end = range[1] ? fmtDate(new Date(range[1])) : ''
  if (start) rows = rows.filter(r => r.date >= start)
  if (end) rows = rows.filter(r => r.date <= end)
  if (searchForm.name) rows = rows.filter(r => r.name.includes(searchForm.name))
  if (searchForm.departmentId) rows = rows.filter(r => r.department === (deptNameMap[searchForm.departmentId] || ''))
  if (searchForm.status) rows = rows.filter(r => r.status === searchForm.status)
  pagination.total = rows.length
  const startIdx = (pagination.currentPage - 1) * pagination.pageSize
  tableData.value = rows.slice(startIdx, startIdx + pagination.pageSize)
}

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

const handleCheckIn = async () => {
  try {
    const res = await checkInApi()
    ElMessage.success(res.message || '上班打卡成功')
    await loadData()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

const handleCheckOut = async () => {
  try {
    const res = await checkOutApi()
    ElMessage.success(res.message || '下班签退成功')
    await loadData()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = Array.isArray(searchForm[key]) ? [] : ''
  })
  pagination.currentPage = 1
  loadData()
}

// 补卡/请假：后端暂无对应接口，保留演示逻辑
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

// 导出月报（P1-3）：弹窗选月份/部门，从后端下载 Excel
const exportDialogVisible = ref(false)
const exportForm = reactive({
  month: new Date().toISOString().slice(0, 7),
  deptId: null
})
const exportLoading = ref(false)

const handleExport = () => {
  exportDialogVisible.value = true
}

const confirmExport = async () => {
  if (!exportForm.month) {
    ElMessage.warning('请选择导出月份')
    return
  }
  exportLoading.value = true
  try {
    const params = { month: exportForm.month }
    if (exportForm.deptId) params.deptId = exportForm.deptId
    const response = await request.download('/attendance/export', params)
    // 后端返回 JSON 错误时 blob 类型是 application/json
    const contentType = response.headers['content-type'] || ''
    if (contentType.includes('application/json')) {
      const text = await response.data.text()
      const json = JSON.parse(text)
      ElMessage.error(json.message || '导出失败')
      return
    }
    const blob = new Blob([response.data], { type: contentType })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `考勤月报_${exportForm.month}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
    exportDialogVisible.value = false
  } catch (e) {
    // 拦截器已提示
  } finally {
    exportLoading.value = false
  }
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

const handleSizeChange = () => {
  pagination.currentPage = 1
  refreshView()
}

const handleCurrentChange = () => {
  refreshView()
}

onMounted(async () => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  await loadBase()
  loadData()
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
