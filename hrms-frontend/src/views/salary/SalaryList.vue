<template>
  <div class="salary-container">
    <el-card shadow="hover">
      <!-- 统计概览 -->
      <el-row :gutter="20" class="stat-row">
        <el-col :xs="12" :sm="6">
          <div class="stat-card primary">
            <div class="stat-label">本月应发总额</div>
            <div class="stat-value">¥{{ formatMoney(totalStats.totalPayroll) }}</div>
            <div class="stat-sub">较上月 <span class="up">{{ totalStats.payrollChange }}</span></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card success">
            <div class="stat-label">人均薪资</div>
            <div class="stat-value">¥{{ formatMoney(totalStats.avgSalary) }}</div>
            <div class="stat-sub">较上月 <span class="up">{{ totalStats.avgChange }}</span></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card warning">
            <div class="stat-label">社保公积金</div>
            <div class="stat-value">¥{{ formatMoney(totalStats.socialInsurance) }}</div>
            <div class="stat-sub">单位缴纳部分</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card danger">
            <div class="stat-label">个人所得税</div>
            <div class="stat-value">¥{{ formatMoney(totalStats.incomeTax) }}</div>
            <div class="stat-sub">本月代扣</div>
          </div>
        </el-col>
      </el-row>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="薪资月份">
            <el-date-picker
              v-model="searchForm.month"
              type="month"
              placeholder="选择月份"
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="员工姓名">
            <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="部门">
            <el-select v-model="searchForm.departmentId" placeholder="请选择部门" clearable style="width: 160px">
              <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="薪资范围">
            <el-input-number v-model="searchForm.minSalary" :min="0" placeholder="最低" style="width: 120px" />
            <span style="margin: 0 8px">-</span>
            <el-input-number v-model="searchForm.maxSalary" :min="0" placeholder="最高" style="width: 120px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增薪资</el-button>
        <el-button type="success" :icon="MagicStick" :loading="calcLoading" @click="handleCalculate">一键计算薪资</el-button>
        <el-button type="warning" :icon="TrendCharts" @click="handleBatchAdjust">批量调薪</el-button>
        <el-button type="primary" :icon="Setting" @click="handleStructure">薪资结构</el-button>
        <el-button type="success" :icon="Download" @click="handleExport">导出报表</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="employeeNo" label="工号" width="100" align="center" />
        <el-table-column prop="name" label="姓名" width="100" align="center" />
        <el-table-column prop="department" label="部门" width="120" align="center" />
        <el-table-column prop="position" label="职位" width="120" align="center" />
        <el-table-column prop="baseSalary" label="基本工资" width="110" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.baseSalary) }}</template>
        </el-table-column>
        <el-table-column prop="performanceSalary" label="绩效工资" width="110" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.performanceSalary) }}</template>
        </el-table-column>
        <el-table-column prop="bonus" label="奖金补贴" width="100" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.bonus) }}</template>
        </el-table-column>
        <el-table-column prop="deduction" label="扣款" width="100" align="right">
          <template #default="{ row }" class="text-danger">-¥{{ formatMoney(row.deduction) }}</template>
        </el-table-column>
        <el-table-column prop="socialInsurance" label="社保公积金" width="120" align="right">
          <template #default="{ row }">-¥{{ formatMoney(row.socialInsurance) }}</template>
        </el-table-column>
        <el-table-column prop="incomeTax" label="个税" width="100" align="right">
          <template #default="{ row }">-¥{{ formatMoney(row.incomeTax) }}</template>
        </el-table-column>
        <el-table-column prop="netSalary" label="实发工资" width="120" align="right" fixed="right">
          <template #default="{ row }">
            <span class="net-salary">¥{{ formatMoney(row.netSalary) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">工资条</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">调整</el-button>
            <el-button type="success" link size="small" @click="handleDownloadPayslip(row)">PDF</el-button>
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

    <!-- 薪资调整弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form :model="formData" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="员工姓名">
              <el-select
                v-if="!formData.id"
                v-model="formData.empId"
                filterable
                placeholder="请选择员工"
                style="width: 100%"
                @change="handleEmployeeChange"
              >
                <el-option
                  v-for="emp in employeeOptions"
                  :key="emp.id"
                  :label="emp.name + '（' + emp.employeeNo + '）'"
                  :value="emp.id"
                />
              </el-select>
              <el-input v-else v-model="formData.name" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-input v-model="formData.department" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="薪资月份">
              <el-date-picker v-model="formData.month" type="month" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="基本工资">
              <el-input-number v-model="formData.baseSalary" :min="0" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="绩效工资">
              <el-input-number v-model="formData.performanceSalary" :min="0" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="奖金补贴">
              <el-input-number v-model="formData.bonus" :min="0" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="社保公积金">
              <el-input-number v-model="formData.socialInsurance" :min="0" :step="50" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="个人所得税">
              <el-input-number v-model="formData.incomeTax" :min="0" :step="50" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="其他扣款">
              <el-input-number v-model="formData.deduction" :min="0" :step="50" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实发工资">
              <el-input :value="'¥' + formatMoney(computedNetSalary)" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="formData.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 工资条详情弹窗 -->
    <el-dialog v-model="payslipVisible" title="工资条详情" width="500px">
      <div class="payslip-header">
        <div class="payslip-title">{{ formData.name }} - 工资条</div>
        <div class="payslip-month">{{ payslipMonth }}</div>
      </div>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="基本工资" align="right">¥{{ formatMoney(formData.baseSalary) }}</el-descriptions-item>
        <el-descriptions-item label="绩效工资" align="right">¥{{ formatMoney(formData.performanceSalary) }}</el-descriptions-item>
        <el-descriptions-item label="岗位津贴" align="right">¥{{ formatMoney(formData.bonus * 0.4) }}</el-descriptions-item>
        <el-descriptions-item label="全勤奖" align="right">¥{{ formatMoney(formData.bonus * 0.3) }}</el-descriptions-item>
        <el-descriptions-item label="其他补贴" align="right">¥{{ formatMoney(formData.bonus * 0.3) }}</el-descriptions-item>
        <el-descriptions-item label="应发工资" align="right">
          <span class="text-primary">¥{{ formatMoney(formData.baseSalary + formData.performanceSalary + formData.bonus) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="养老保险" align="right">-¥{{ formatMoney(formData.socialInsurance * 0.5) }}</el-descriptions-item>
        <el-descriptions-item label="医疗保险" align="right">-¥{{ formatMoney(formData.socialInsurance * 0.2) }}</el-descriptions-item>
        <el-descriptions-item label="失业保险" align="right">-¥{{ formatMoney(formData.socialInsurance * 0.1) }}</el-descriptions-item>
        <el-descriptions-item label="住房公积金" align="right">-¥{{ formatMoney(formData.socialInsurance * 0.2) }}</el-descriptions-item>
        <el-descriptions-item label="个人所得税" align="right">-¥{{ formatMoney(formData.incomeTax) }}</el-descriptions-item>
        <el-descriptions-item label="其他扣款" align="right">-¥{{ formatMoney(formData.deduction) }}</el-descriptions-item>
      </el-descriptions>
      <div class="payslip-total">
        <span>实发工资：</span>
        <span class="total-amount">¥{{ formatMoney(computedNetSalary) }}</span>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, MagicStick, TrendCharts, Setting, Download } from '@element-plus/icons-vue'
import { getSalaryListApi, getSalaryStatsApi, addSalaryApi, updateSalaryApi, calculateSalaryApi, downloadPayslipApi } from '@/api/salary'
import { getEmployeeListApi } from '@/api/employee'
import { getDepartmentListApi } from '@/api/department'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const payslipVisible = ref(false)
const dialogTitle = ref('')
const payslipMonth = ref('')

// P042：统计卡改为真实接口数据，环比由上月统计计算
const totalStats = reactive({
  totalPayroll: 0,
  avgSalary: 0,
  socialInsurance: 0,
  incomeTax: 0,
  payrollChange: '0.0%',
  avgChange: '0.0%'
})

const searchForm = reactive({
  month: '',
  name: '',
  departmentId: '',
  minSalary: null,
  maxSalary: null
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// P042：薪资列表改为真实接口加载（后端按月份查询全量，前端做条件过滤+分页）
const allRows = ref([])
const tableData = ref([])
const departmentOptions = ref([])
const employeeOptions = ref([])
let deptNameMap = {}
let empMap = {}

const formData = reactive({
  id: null,
  empId: null,
  name: '',
  department: '',
  month: '',
  baseSalary: 0,
  performanceSalary: 0,
  bonus: 0,
  socialInsurance: 0,
  incomeTax: 0,
  deduction: 0,
  remark: ''
})

const computedNetSalary = computed(() => {
  return formData.baseSalary + formData.performanceSalary + formData.bonus
    - formData.socialInsurance - formData.incomeTax - formData.deduction
})

const formatMoney = (value) => {
  if (!value && value !== 0) return '0.00'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const pad = (n) => String(n).padStart(2, '0')
const fmtMonth = (value) => {
  if (!value) return ''
  if (typeof value === 'string') return value.slice(0, 7)
  const d = new Date(value)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
}

const currentMonth = () => (searchForm.month ? fmtMonth(searchForm.month) : fmtMonth(new Date()))

/**
 * 后端字段 performanceBonus/overtimePay/allowance/socialSecurity/housingFund/tax/otherDeduction
 * 与页面展示字段 performanceSalary/bonus/socialInsurance/incomeTax/deduction 做归一化
 */
const normalizeSalary = (item) => {
  const emp = empMap[item.empId] || {}
  return {
    id: item.id,
    empId: item.empId,
    employeeNo: emp.empNo || '-',
    name: emp.name || '-',
    department: emp.departmentName || '-',
    position: emp.position || '-',
    salaryMonth: item.salaryMonth,
    baseSalary: Number(item.baseSalary || 0),
    performanceSalary: Number(item.performanceBonus || 0),
    bonus: Number((Number(item.overtimePay || 0) + Number(item.allowance || 0)).toFixed(2)),
    deduction: Number(item.otherDeduction || 0),
    socialInsurance: Number((Number(item.socialSecurity || 0) + Number(item.housingFund || 0)).toFixed(2)),
    incomeTax: Number(item.tax || 0),
    netSalary: Number(item.netSalary || 0),
    remark: item.remark || ''
  }
}

const loadBase = async () => {
  try {
    const [empRes, deptRes] = await Promise.all([getEmployeeListApi(), getDepartmentListApi()])
    const depts = deptRes.data || []
    deptNameMap = {}
    depts.forEach(d => { deptNameMap[d.id] = d.deptName })
    departmentOptions.value = depts.map(d => ({ id: d.id, name: d.deptName }))
    const emps = empRes.data || []
    empMap = {}
    emps.forEach(e => {
      empMap[e.id] = { ...e, departmentName: deptNameMap[e.deptId] || '-' }
    })
    employeeOptions.value = emps.map(e => ({ id: e.id, employeeNo: e.empNo, name: e.name }))
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSalaryListApi({ salaryMonth: currentMonth() })
    allRows.value = (res.data || []).map(normalizeSalary)
    refreshView()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}

const buildChange = (cur, prev) => {
  if (!prev) return '0.0%'
  const pct = (((cur - prev) / prev) * 100).toFixed(1)
  return pct > 0 ? `+${pct}%` : `${pct}%`
}

const loadStats = async () => {
  try {
    const month = currentMonth()
    const now = new Date()
    const prevMonth = fmtMonth(new Date(now.getFullYear(), now.getMonth() - 1, 1))
    const [curRes, prevRes] = await Promise.all([
      getSalaryStatsApi({ salaryMonth: month }),
      getSalaryStatsApi({ salaryMonth: prevMonth })
    ])
    const cur = curRes.data || {}
    const prev = prevRes.data || {}
    const curNet = Number(cur.totalNetSalary || 0)
    const prevNet = Number(prev.totalNetSalary || 0)
    const count = cur.employeeCount || 0
    const prevCount = prev.employeeCount || 0
    totalStats.totalPayroll = curNet
    totalStats.avgSalary = count ? Number((curNet / count).toFixed(2)) : 0
    totalStats.socialInsurance = Number((Number(cur.totalSocialSecurity || 0) + Number(cur.totalHousingFund || 0)).toFixed(2))
    totalStats.incomeTax = Number(cur.totalTax || 0)
    totalStats.payrollChange = buildChange(curNet, prevNet)
    totalStats.avgChange = buildChange(
      count ? curNet / count : 0,
      prevCount ? prevNet / prevCount : 0
    )
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

/** 前端条件过滤 + 分页切片（后端列表接口仅支持按月份查询） */
const refreshView = () => {
  let rows = allRows.value
  if (searchForm.name) rows = rows.filter(r => r.name.includes(searchForm.name))
  if (searchForm.departmentId) rows = rows.filter(r => r.department === (deptNameMap[searchForm.departmentId] || ''))
  if (searchForm.minSalary != null) rows = rows.filter(r => r.netSalary >= searchForm.minSalary)
  if (searchForm.maxSalary != null) rows = rows.filter(r => r.netSalary <= searchForm.maxSalary)
  pagination.total = rows.length
  const startIdx = (pagination.currentPage - 1) * pagination.pageSize
  tableData.value = rows.slice(startIdx, startIdx + pagination.pageSize)
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
  loadStats()
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  searchForm.minSalary = null
  searchForm.maxSalary = null
  pagination.currentPage = 1
  loadData()
  loadStats()
}

const handleAdd = () => {
  dialogTitle.value = '新增薪资记录'
  Object.keys(formData).forEach(key => {
    formData[key] = typeof formData[key] === 'number' ? 0 : ''
  })
  formData.id = null
  formData.empId = null
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '调整薪资'
  Object.assign(formData, row)
  formData.month = row.salaryMonth
  dialogVisible.value = true
}

const handleView = (row) => {
  Object.assign(formData, row)
  payslipMonth.value = row.salaryMonth
  payslipVisible.value = true
}

// P2-3 下载工资条 PDF（管理员任意员工 / 普通员工仅自己）
const handleDownloadPayslip = async (row) => {
  try {
    const response = await downloadPayslipApi({ empId: row.empId, salaryMonth: row.salaryMonth })
    const contentType = response.headers['content-type'] || ''
    if (contentType.includes('application/json')) {
      const text = await response.data.text()
      const json = JSON.parse(text)
      ElMessage.error(json.message || '下载失败')
      return
    }
    const blob = new Blob([response.data], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `工资条_${row.name}_${row.salaryMonth}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('工资条 PDF 已下载')
  } catch (e) {
    // 拦截器已提示
  }
}

// 新增模式下选择员工后带出姓名/部门展示
const handleEmployeeChange = (empId) => {
  const emp = empMap[empId]
  formData.name = emp?.name || ''
  formData.department = emp?.departmentName || ''
}

const handleSubmit = async () => {
  if (!formData.empId) {
    ElMessage.warning('请选择员工')
    return
  }
  submitLoading.value = true
  try {
    // 映射回后端字段；实发工资由后端重新计算
    const payload = {
      empId: formData.empId,
      salaryMonth: fmtMonth(formData.month) || currentMonth(),
      baseSalary: formData.baseSalary,
      performanceBonus: formData.performanceSalary,
      overtimePay: 0,
      allowance: formData.bonus,
      socialSecurity: formData.socialInsurance,
      housingFund: 0,
      tax: formData.incomeTax,
      otherDeduction: formData.deduction,
      remark: formData.remark
    }
    if (formData.id) {
      payload.id = formData.id
      await updateSalaryApi(payload)
      ElMessage.success('调整成功')
    } else {
      await addSalaryApi(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadData()
    loadStats()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    submitLoading.value = false
  }
}

// P2-2 一键计算月度薪资：底薪 + 考勤扣款 + 绩效奖金联动，已有记录跳过
const calcLoading = ref(false)
const handleCalculate = async () => {
  const month = currentMonth()
  try {
    await ElMessageBox.confirm(
      `将按「档案底薪 + 当月考勤扣款（缺勤/迟到早退）+ 绩效奖金联动 + 五险一金个税估算」为在职员工生成 ${month} 薪资记录；已有当月记录的员工将跳过。是否继续？`,
      '一键计算薪资',
      { confirmButtonText: '开始计算', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  calcLoading.value = true
  try {
    const res = await calculateSalaryApi(month)
    const d = res.data || {}
    ElMessage.success(`${month} 计算完成：新增 ${d.created ?? 0} 条，跳过 ${d.skipped ?? 0} 条（已有记录）`)
    await loadData()
    loadStats()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    calcLoading.value = false
  }
}

const handleBatchAdjust = () => {
  ElMessage.info('批量调薪功能演示')
}

const handleStructure = () => {
  ElMessage.info('薪资结构配置功能演示')
}

const handleExport = () => {
  ElMessage.info('导出功能演示')
}

const handleSizeChange = () => {
  pagination.currentPage = 1
  refreshView()
}

const handleCurrentChange = () => {
  refreshView()
}

onMounted(async () => {
  await loadBase()
  loadData()
  loadStats()
})
</script>

<style scoped lang="scss">
.salary-container {
  .stat-row {
    margin-bottom: 24px;
  }

  .stat-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    border-left: 4px solid;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);

    &.primary { border-left-color: #409EFF; }
    &.success { border-left-color: #67C23A; }
    &.warning { border-left-color: #E6A23C; }
    &.danger { border-left-color: #F56C6C; }

    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 8px;
    }

    .stat-sub {
      font-size: 12px;
      color: #909399;

      .up { color: #67C23A; }
      .down { color: #F56C6C; }
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

  .text-danger {
    color: #F56C6C;
  }

  .text-primary {
    color: #409EFF;
    font-weight: 600;
  }

  .net-salary {
    color: #F56C6C;
    font-weight: 600;
  }

  .payslip-header {
    text-align: center;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px dashed #dcdfe6;

    .payslip-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }

    .payslip-month {
      font-size: 14px;
      color: #909399;
      margin-top: 6px;
    }
  }

  .payslip-total {
    margin-top: 20px;
    padding-top: 16px;
    border-top: 2px solid #409EFF;
    text-align: right;
    font-size: 16px;

    .total-amount {
      font-size: 22px;
      font-weight: 600;
      color: #F56C6C;
    }
  }
}
</style>
