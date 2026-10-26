<template>
  <div class="profile-container">
    <!-- 第一行：个人信息 + 今日打卡 -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <el-card shadow="hover" class="profile-card">
          <template #header><span class="card-title">个人信息</span></template>
          <div v-loading="loading">
            <template v-if="profile.employee">
              <div class="emp-head">
                <el-avatar :size="56" class="emp-avatar">{{ (profile.employee.name || '?').charAt(0) }}</el-avatar>
                <div>
                  <div class="emp-name">{{ profile.employee.name }}</div>
                  <div class="emp-sub">{{ profile.employee.position || '未设置职位' }} · {{ profile.department?.deptName || '未分配部门' }}</div>
                </div>
                <el-tag :type="profile.employee.status === 1 ? 'success' : 'info'" class="emp-status">
                  {{ profile.employee.status === 1 ? '在职' : '离职' }}
                </el-tag>
              </div>
              <el-descriptions :column="2" border class="emp-desc">
                <el-descriptions-item label="工号">{{ profile.employee.empNo || '-' }}</el-descriptions-item>
                <el-descriptions-item label="入职日期">{{ profile.employee.entryDate || '-' }}</el-descriptions-item>
                <el-descriptions-item label="手机号">{{ profile.employee.phone || '-' }}</el-descriptions-item>
                <el-descriptions-item label="邮箱">{{ profile.employee.email || '-' }}</el-descriptions-item>
                <el-descriptions-item label="学历">{{ profile.employee.education || '-' }}</el-descriptions-item>
                <el-descriptions-item label="工作地点">{{ profile.employee.workLocation || '-' }}</el-descriptions-item>
              </el-descriptions>
            </template>
            <el-empty v-else description="当前账号未绑定员工档案" :image-size="72" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card shadow="hover" class="profile-card">
          <template #header><span class="card-title">今日打卡</span></template>
          <div v-loading="todayLoading" class="punch-box">
            <div class="punch-status">
              <div class="punch-item">
                <div class="punch-label">上班</div>
                <el-tag :type="today.checkedIn ? 'success' : 'info'">{{ today.checkedIn ? '已打卡' : '未打卡' }}</el-tag>
                <div class="punch-time">{{ today.today?.checkInTime || '--:--' }}</div>
              </div>
              <div class="punch-divider"></div>
              <div class="punch-item">
                <div class="punch-label">下班</div>
                <el-tag :type="today.checkedOut ? 'success' : 'info'">{{ today.checkedOut ? '已打卡' : '未打卡' }}</el-tag>
                <div class="punch-time">{{ today.today?.checkOutTime || '--:--' }}</div>
              </div>
            </div>
            <div class="punch-actions">
              <el-button type="primary" :disabled="today.checkedIn" :loading="punchLoading" @click="handlePunch('in')">上班打卡</el-button>
              <el-button type="success" :disabled="!today.checkedIn || today.checkedOut" :loading="punchLoading" @click="handlePunch('out')">下班打卡</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行：本月考勤 + 我的薪资 -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="10">
        <el-card shadow="hover" class="profile-card">
          <template #header>
            <div class="card-head">
              <span class="card-title">本月考勤</span>
              <span class="card-extra">{{ currentMonth }}</span>
            </div>
          </template>
          <div v-loading="attendLoading">
            <div v-if="attendance.statistics" class="stat-grid">
              <div v-for="item in attendStats" :key="item.label" class="stat-cell">
                <div class="stat-value" :class="item.cls">{{ item.value }}</div>
                <div class="stat-label">{{ item.label }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无考勤数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="14">
        <el-card shadow="hover" class="profile-card">
          <template #header>
            <div class="card-head">
              <span class="card-title">我的薪资</span>
              <el-link type="primary" @click="$router.push('/salary')">查看全部</el-link>
            </div>
          </template>
          <div v-loading="salaryLoading">
            <el-table :data="salaryRows.slice(0, 4)" size="small" v-if="salaryRows.length > 0">
              <el-table-column prop="salaryMonth" label="月份" width="90" />
              <el-table-column label="基本工资" width="100">
                <template #default="{ row }">{{ fmtMoney(row.baseSalary) }}</template>
              </el-table-column>
              <el-table-column label="绩效奖金" width="100">
                <template #default="{ row }">{{ fmtMoney(row.performanceBonus) }}</template>
              </el-table-column>
              <el-table-column label="实发" width="100">
                <template #default="{ row }">
                  <span class="net-money">{{ fmtMoney(row.netSalary) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="downloadPayslip(row)">工资条PDF</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无薪资记录" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-row :gutter="16">
      <el-col :span="24">
        <el-card shadow="hover" class="profile-card">
          <template #header><span class="card-title">快捷入口</span></template>
          <div class="quick-links">
            <el-button type="warning" plain :icon="DocumentChecked" @click="$router.push('/approval')">
              审批中心<el-badge v-if="todoCount > 0" :value="todoCount" class="quick-badge" />
            </el-button>
            <el-button type="primary" plain :icon="ChatDotRound" @click="$router.push('/message')">
              消息中心<el-badge v-if="msgUnread > 0" :value="msgUnread" class="quick-badge" />
            </el-button>
            <el-button plain :icon="Calendar" @click="$router.push('/attendance')">考勤管理</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentChecked, ChatDotRound, Calendar } from '@element-plus/icons-vue'
import { getSelfProfileApi, getSelfSalaryApi, getSelfAttendanceApi, getSelfAttendanceTodayApi } from '@/api/self'
import { checkInApi, checkOutApi } from '@/api/attendance'
import { downloadPayslipApi } from '@/api/salary'
import { getApprovalTodoApi } from '@/api/approval'
import { getUnreadCountApi } from '@/api/message'

const loading = ref(false)
const profile = ref({ user: {}, employee: null, department: null })

const todayLoading = ref(false)
const today = ref({ checkedIn: false, checkedOut: false, today: null })
const punchLoading = ref(false)

const attendLoading = ref(false)
const attendance = ref({ statistics: null, records: [] })

const salaryLoading = ref(false)
const salaryRows = ref([])

const todoCount = ref(0)
const msgUnread = ref(0)

const currentMonth = new Date().toISOString().slice(0, 7)

const attendStats = computed(() => {
  const s = attendance.value.statistics || {}
  return [
    { label: '出勤天数', value: s.totalDays ?? s.attendDays ?? 0, cls: '' },
    { label: '迟到', value: s.lateCount ?? 0, cls: s.lateCount ? 'warn' : '' },
    { label: '早退', value: s.earlyLeaveCount ?? 0, cls: s.earlyLeaveCount ? 'warn' : '' },
    { label: '工时(h)', value: s.totalHours ?? 0, cls: '' }
  ]
})

const fmtMoney = (v) => {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const loadProfile = async () => {
  loading.value = true
  try {
    const res = await getSelfProfileApi()
    profile.value = res.data || { user: {}, employee: null, department: null }
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

const loadToday = async () => {
  todayLoading.value = true
  try {
    const res = await getSelfAttendanceTodayApi()
    today.value = res.data || { checkedIn: false, checkedOut: false, today: null }
  } catch (e) { /* 拦截器已提示 */ } finally {
    todayLoading.value = false
  }
}

const loadAttendance = async () => {
  attendLoading.value = true
  try {
    const res = await getSelfAttendanceApi(currentMonth)
    attendance.value = res.data || { statistics: null, records: [] }
  } catch (e) { /* 拦截器已提示 */ } finally {
    attendLoading.value = false
  }
}

const loadSalary = async () => {
  salaryLoading.value = true
  try {
    const res = await getSelfSalaryApi()
    salaryRows.value = res.data || []
  } catch (e) { /* 拦截器已提示 */ } finally {
    salaryLoading.value = false
  }
}

const loadBadges = async () => {
  try {
    const t = await getApprovalTodoApi()
    todoCount.value = (t.data || []).length
  } catch (e) { /* 忽略 */ }
  try {
    const m = await getUnreadCountApi()
    msgUnread.value = Number(m.data || 0)
  } catch (e) { /* 忽略 */ }
}

const handlePunch = async (type) => {
  punchLoading.value = true
  try {
    if (type === 'in') {
      await checkInApi()
      ElMessage.success('上班打卡成功')
    } else {
      await checkOutApi()
      ElMessage.success('下班打卡成功')
    }
    await loadToday()
    loadAttendance()
  } catch (e) { /* 拦截器已提示 */ } finally {
    punchLoading.value = false
  }
}

const downloadPayslip = async (row) => {
  try {
    const response = await downloadPayslipApi({ empId: row.empId, salaryMonth: row.salaryMonth })
    const contentType = response.headers['content-type'] || ''
    if (contentType.includes('application/json')) {
      const text = await response.data.text()
      ElMessage.error(JSON.parse(text).message || '下载失败')
      return
    }
    const blob = new Blob([response.data], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `工资条_${profile.value.employee?.name || ''}_${row.salaryMonth}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('工资条 PDF 已下载')
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(() => {
  loadProfile()
  loadToday()
  loadAttendance()
  loadSalary()
  loadBadges()
})
</script>

<style scoped lang="scss">
.profile-container {
  .profile-card {
    margin-bottom: 16px;
  }

  .card-title {
    font-weight: 600;
    color: #303133;
  }

  .card-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .card-extra {
    font-size: 12px;
    color: #909399;
  }

  .emp-head {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 16px;

    .emp-avatar {
      background: #409eff;
      color: #fff;
      font-size: 22px;
    }

    .emp-name {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }

    .emp-sub {
      font-size: 13px;
      color: #909399;
      margin-top: 4px;
    }

    .emp-status {
      margin-left: auto;
    }
  }

  .emp-desc {
    margin-top: 4px;
  }

  .punch-box {
    .punch-status {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 32px;
      padding: 18px 0;
    }

    .punch-item {
      text-align: center;

      .punch-label {
        font-size: 13px;
        color: #909399;
        margin-bottom: 8px;
      }

      .punch-time {
        font-size: 15px;
        font-weight: 600;
        color: #303133;
        margin-top: 8px;
        font-family: monospace;
      }
    }

    .punch-divider {
      width: 1px;
      height: 56px;
      background: #ebeef5;
    }

    .punch-actions {
      display: flex;
      justify-content: center;
      gap: 12px;
      padding-top: 6px;
    }
  }

  .stat-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;

    .stat-cell {
      text-align: center;
      padding: 14px 0;
      background: #f5f7fa;
      border-radius: 8px;

      .stat-value {
        font-size: 22px;
        font-weight: 700;
        color: #303133;

        &.warn {
          color: #e6a23c;
        }
      }

      .stat-label {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .net-money {
    color: #c0392b;
    font-weight: 600;
  }

  .quick-links {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }

  .quick-badge {
    margin-left: 6px;
  }
}
</style>
