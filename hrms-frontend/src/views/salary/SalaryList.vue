<template>
  <div class="salary-container">
    <el-card shadow="hover">
      <!-- 统计概览 -->
      <el-row :gutter="20" class="stat-row">
        <el-col :xs="12" :sm="6">
          <div class="stat-card primary">
            <div class="stat-label">本月应发总额</div>
            <div class="stat-value">¥{{ formatMoney(totalStats.totalPayroll) }}</div>
            <div class="stat-sub">较上月 <span class="up">+5.2%</span></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card success">
            <div class="stat-label">人均薪资</div>
            <div class="stat-value">¥{{ formatMoney(totalStats.avgSalary) }}</div>
            <div class="stat-sub">较上月 <span class="up">+3.8%</span></div>
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
              <el-option label="技术部" :value="1" />
              <el-option label="产品部" :value="2" />
              <el-option label="市场部" :value="3" />
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
        <el-button type="success" :icon="Money" @click="handleGenerate">生成工资条</el-button>
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
            <el-button type="success" link size="small" @click="handleSend(row)">发送</el-button>
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

    <!-- 薪资调整弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form :model="formData" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="员工姓名">
              <el-input v-model="formData.name" disabled />
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
        <el-button type="primary" @click="handleSubmit">确定</el-button>
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
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus, Money, TrendCharts, Setting, Download } from '@element-plus/icons-vue'

const loading = ref(false)
const dialogVisible = ref(false)
const payslipVisible = ref(false)
const dialogTitle = ref('')
const payslipMonth = ref('')

const totalStats = reactive({
  totalPayroll: 1856800,
  avgSalary: 12650,
  socialInsurance: 385600,
  incomeTax: 128500
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
  total: 56
})

const tableData = ref([
  { id: 1, employeeNo: 'E001', name: '张三', department: '技术部', position: '高级工程师', baseSalary: 18000, performanceSalary: 5000, bonus: 2000, deduction: 200, socialInsurance: 3200, incomeTax: 1800, netSalary: 19800 },
  { id: 2, employeeNo: 'E002', name: '李四', department: '技术部', position: '前端工程师', baseSalary: 12000, performanceSalary: 3000, bonus: 1000, deduction: 0, socialInsurance: 2100, incomeTax: 850, netSalary: 13050 },
  { id: 3, employeeNo: 'E003', name: '王五', department: '产品部', position: '产品经理', baseSalary: 15000, performanceSalary: 4000, bonus: 1500, deduction: 100, socialInsurance: 2700, incomeTax: 1200, netSalary: 16500 },
  { id: 4, employeeNo: 'E004', name: '赵六', department: '市场部', position: '市场专员', baseSalary: 8000, performanceSalary: 2000, bonus: 800, deduction: 0, socialInsurance: 1400, incomeTax: 350, netSalary: 9050 },
  { id: 5, employeeNo: 'E005', name: '钱七', department: '运营部', position: '运营主管', baseSalary: 13000, performanceSalary: 3500, bonus: 1200, deduction: 50, socialInsurance: 2300, incomeTax: 950, netSalary: 14400 },
  { id: 6, employeeNo: 'E006', name: '孙八', department: '人事部', position: 'HR专员', baseSalary: 9000, performanceSalary: 2000, bonus: 800, deduction: 0, socialInsurance: 1600, incomeTax: 420, netSalary: 9780 },
  { id: 7, employeeNo: 'E007', name: '周九', department: '财务部', position: '会计', baseSalary: 10000, performanceSalary: 2500, bonus: 1000, deduction: 0, socialInsurance: 1800, incomeTax: 580, netSalary: 11120 },
  { id: 8, employeeNo: 'E008', name: '吴十', department: '行政部', position: '行政专员', baseSalary: 7500, performanceSalary: 1500, bonus: 600, deduction: 0, socialInsurance: 1300, incomeTax: 280, netSalary: 8020 },
  { id: 9, employeeNo: 'E009', name: '郑十一', department: '技术部', position: '后端工程师', baseSalary: 16000, performanceSalary: 4500, bonus: 1800, deduction: 150, socialInsurance: 2900, incomeTax: 1500, netSalary: 17750 },
  { id: 10, employeeNo: 'E010', name: '冯十二', department: '销售部', position: '销售经理', baseSalary: 10000, performanceSalary: 8000, bonus: 3000, deduction: 200, socialInsurance: 1800, incomeTax: 1600, netSalary: 17400 }
])

const formData = reactive({
  id: null,
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

const handleSearch = () => {
  ElMessage.success('搜索功能演示')
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  searchForm.minSalary = null
  searchForm.maxSalary = null
}

const handleAdd = () => {
  dialogTitle.value = '新增薪资记录'
  Object.keys(formData).forEach(key => {
    formData[key] = typeof formData[key] === 'number' ? 0 : ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '调整薪资'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleView = (row) => {
  Object.assign(formData, row)
  payslipMonth.value = '2024年1月'
  payslipVisible.value = true
}

const handleSend = (row) => {
  ElMessage.success(`已向 ${row.name} 发送工资条`)
}

const handleSubmit = () => {
  ElMessage.success('保存成功')
  dialogVisible.value = false
}

const handleGenerate = () => {
  ElMessage.success('工资条生成成功')
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
