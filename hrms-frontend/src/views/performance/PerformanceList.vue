<template>
  <div class="performance-container page-stack">
    <!-- 页头 -->
    <PageHeader
      title="绩效管理"
      description="考核周期、自评与上级评分全流程管理，支持绩效等级统计与分析"
      icon="Trophy"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增绩效</el-button>
        <el-button type="success" :icon="Upload" @click="handleImport">导入</el-button>
        <el-button type="warning" :icon="Download" @click="handleExport">导出</el-button>
        <el-button type="primary" :icon="Setting" @click="handleIndicators">指标配置</el-button>
        <el-button type="info" :icon="TrendCharts" @click="handleAnalytics">绩效分析</el-button>
      </template>
    </PageHeader>

    <el-card shadow="hover">
      <!-- 考核周期选择 -->
      <div class="cycle-selector">
        <div class="cycle-info">
          <span class="label">当前考核周期：</span>
          <el-tag type="primary" size="large">{{ currentCycle.name }}</el-tag>
          <span class="cycle-date">{{ currentCycle.startDate }} ~ {{ currentCycle.endDate }}</span>
          <el-tag :type="currentCycle.status === 'ongoing' ? 'success' : 'info'" size="small">
            {{ currentCycle.status === 'ongoing' ? '进行中' : '已结束' }}
          </el-tag>
        </div>
        <div>
          <el-button :icon="Plus" type="primary" @click="handleCreateCycle">创建考核周期</el-button>
        </div>
      </div>

      <!-- 统计概览 -->
      <el-row :gutter="20" class="stat-row">
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon s-grade"><el-icon :size="28"><Trophy /></el-icon></div>
            <div class="stat-info">
              <div class="stat-num">{{ stats.gradeS }}人</div>
              <div class="stat-label">S级（优秀）</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon a-grade"><el-icon :size="28"><Medal /></el-icon></div>
            <div class="stat-info">
              <div class="stat-num">{{ stats.gradeA }}人</div>
              <div class="stat-label">A级（良好）</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon b-grade"><el-icon :size="28"><Star /></el-icon></div>
            <div class="stat-info">
              <div class="stat-num">{{ stats.gradeB }}人</div>
              <div class="stat-label">B级（合格）</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon c-grade"><el-icon :size="28"><Warning /></el-icon></div>
            <div class="stat-info">
              <div class="stat-num">{{ stats.gradeC }}人</div>
              <div class="stat-label">C/D级（待改进）</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 搜索区 -->
    <div class="search-card">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="考核周期">
            <el-select v-model="searchForm.cycleId" placeholder="请选择周期" style="width: 180px">
              <el-option v-for="p in periodOptions" :key="p.value" :label="p.label" :value="p.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="员工姓名">
            <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="部门">
            <el-select v-model="searchForm.departmentId" placeholder="请选择部门" clearable style="width: 160px">
              <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="绩效等级">
            <el-select v-model="searchForm.grade" placeholder="全部等级" clearable style="width: 140px">
              <el-option label="S-优秀" value="S" />
              <el-option label="A-良好" value="A" />
              <el-option label="B-合格" value="B" />
              <el-option label="C-待改进" value="C" />
              <el-option label="D-不合格" value="D" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 140px">
              <el-option label="待自评" value="pending_self" />
              <el-option label="待上级评" value="pending_manager" />
              <el-option label="待面谈" value="pending_interview" />
              <el-option label="已完成" value="completed" />
            </el-select>
          </el-form-item>
          <el-form-item class="search-submit">
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="employeeNo" label="工号" width="100" align="center" />
        <el-table-column prop="name" label="姓名" width="100" align="center" />
        <el-table-column prop="department" label="部门" width="120" align="center" />
        <el-table-column prop="position" label="职位" width="120" align="center" />
        <el-table-column prop="selfScore" label="自评得分" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.selfScore !== null">{{ row.selfScore }}</span>
            <el-tag v-else type="info" size="small">未提交</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="managerScore" label="上级评分" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.managerScore !== null">{{ row.managerScore }}</span>
            <el-tag v-else type="info" size="small">未评分</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="finalScore" label="最终得分" width="100" align="center">
          <template #default="{ row }">
            <span class="final-score" v-if="row.finalScore !== null">{{ row.finalScore }}</span>
            <el-tag v-else type="info" size="small">-</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="grade" label="绩效等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.grade" :type="getGradeTagType(row.grade)" effect="dark" size="small">
              {{ row.grade }}
            </el-tag>
            <el-tag v-else type="info" size="small">待定</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">详情</el-button>
            <el-button v-if="row.status === 'pending_self'" type="warning" link size="small" @click="handleSelfReview(row)">
              自评
            </el-button>
            <el-button v-if="row.status === 'pending_manager'" type="success" link size="small" @click="handleManagerReview(row)">
              评分
            </el-button>
            <el-button v-if="row.status === 'pending_interview'" type="primary" link size="small" @click="handleInterview(row)">
              面谈
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination page-pagination">
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
    </div>

    <!-- 自评弹窗 -->
    <el-dialog v-model="reviewDialogVisible" title="绩效自评" width="700px" destroy-on-close>
      <div class="review-header">
        <span>员工：{{ currentReview.name }}</span>
        <span>部门：{{ currentReview.department }}</span>
        <span>周期：{{ currentCycle.name }}</span>
      </div>
      <el-form :model="reviewForm" label-width="120px">
        <el-table :data="indicatorList" border size="small">
          <el-table-column prop="name" label="考核指标" width="200" />
          <el-table-column prop="weight" label="权重" width="100" align="center">
            <template #default="{ row }">{{ row.weight }}%</template>
          </el-table-column>
          <el-table-column prop="description" label="指标说明" min-width="200" />
          <el-table-column label="自评分" width="150" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.selfScore"
                :min="0"
                :max="100"
                size="small"
                style="width: 100%"
              />
            </template>
          </el-table-column>
        </el-table>
        <el-form-item label="自我总结" style="margin-top: 20px">
          <el-input v-model="reviewForm.selfSummary" type="textarea" :rows="4" placeholder="请输入本周期的工作内容和自我总结" />
        </el-form-item>
        <el-form-item label="改进计划">
          <el-input v-model="reviewForm.improvementPlan" type="textarea" :rows="3" placeholder="请输入下一周期的改进计划" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitSelfReview">提交自评</el-button>
      </template>
    </el-dialog>

    <!-- 新增绩效弹窗 -->
    <el-dialog v-model="addDialogVisible" title="新增绩效" width="600px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="120px">
        <el-form-item label="员工" prop="empId">
          <el-select v-model="addForm.empId" filterable placeholder="请选择员工" style="width: 100%">
            <el-option
              v-for="emp in employeeOptions"
              :key="emp.id"
              :label="emp.name + '（' + emp.employeeNo + '）'"
              :value="emp.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="考核周期" prop="period">
          <el-select v-model="addForm.period" placeholder="请选择考核周期" style="width: 100%">
            <el-option v-for="p in periodOptions" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作质量" prop="workQuality">
          <el-input-number v-model="addForm.workQuality" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工作效率" prop="workEfficiency">
          <el-input-number v-model="addForm.workEfficiency" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工作态度" prop="workAttitude">
          <el-input-number v-model="addForm.workAttitude" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="团队协作" prop="teamwork">
          <el-input-number v-model="addForm.teamwork" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="创新改进" prop="innovation">
          <el-input-number v-model="addForm.innovation" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="addForm.comment" type="textarea" :rows="2" placeholder="请输入评语" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 绩效分析 -->
    <el-dialog v-model="analyticsVisible" title="绩效分析" width="640px">
      <div v-loading="analyticsLoading">
        <el-row :gutter="16" class="analytics-summary">
          <el-col :span="8">
            <div class="summary-card">
              <div class="summary-label">考核周期</div>
              <div class="summary-value">{{ analyticsPeriod }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="summary-card">
              <div class="summary-label">参评人数</div>
              <div class="summary-value">{{ analytics.totalCount }} 人</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="summary-card">
              <div class="summary-label">平均分</div>
              <div class="summary-value">{{ analytics.avgScore }}</div>
            </div>
          </el-col>
        </el-row>
        <div class="analytics-title">等级分布</div>
        <div v-for="g in gradeBars" :key="g.label" class="grade-row">
          <span class="grade-name">{{ g.label }}</span>
          <el-progress
            :percentage="g.percent"
            :color="g.color"
            :stroke-width="14"
            :show-text="false"
            class="grade-progress"
          />
          <span class="grade-num">{{ g.count }} 人 / {{ g.percent }}%</span>
        </div>
        <el-empty v-if="!analyticsLoading && !analytics.totalCount" description="当前周期暂无绩效数据" />
      </div>
      <template #footer>
        <el-button @click="analyticsVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 指标配置 -->
    <el-dialog v-model="indicatorVisible" title="绩效指标配置" width="680px">
      <el-table :data="indicatorList" size="small">
        <el-table-column prop="name" label="指标名称" width="140" />
        <el-table-column label="权重(%)" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.weight" :min="0" :max="100" :step="5" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200" />
      </el-table>
      <div class="weight-total">
        权重合计：
        <span :class="{ 'weight-error': indicatorWeightTotal !== 100 }">{{ indicatorWeightTotal }}%</span>
        <span class="weight-hint">（建议等于 100%）</span>
      </div>
      <template #footer>
        <el-button @click="indicatorVisible = false">取消</el-button>
        <el-button type="primary" :loading="indicatorSaving" @click="saveIndicators">保存</el-button>
      </template>
    </el-dialog>

    <!-- 创建考核周期 -->
    <el-dialog v-model="cycleVisible" title="创建考核周期" width="520px">
      <el-form :model="cycleForm" label-width="90px">
        <el-form-item label="周期名称">
          <el-input v-model="cycleForm.cycleName" placeholder="如：2026年11月考核" />
        </el-form-item>
        <el-form-item label="考核月份">
          <el-date-picker v-model="cycleForm.period" type="month" value-format="YYYY-MM" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="cycleForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="cycleForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cycleVisible = false">取消</el-button>
        <el-button type="primary" :loading="cycleSaving" @click="saveCycle">创建</el-button>
      </template>
    </el-dialog>

    <!-- 绩效详情 -->
    <el-dialog v-model="detailVisible" title="绩效详情" width="680px">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detailData" :column="2" border>
          <el-descriptions-item label="姓名">{{ detailData.name }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ detailData.department }}</el-descriptions-item>
          <el-descriptions-item label="考核周期">{{ detailData.period }}</el-descriptions-item>
          <el-descriptions-item label="流程状态">{{ getStatusText(detailData.flowStatus || 'completed') }}</el-descriptions-item>
          <el-descriptions-item label="工作质量">{{ detailData.workQuality ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="工作效率">{{ detailData.workEfficiency ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="工作态度">{{ detailData.workAttitude ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="团队协作">{{ detailData.teamwork ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="创新改进">{{ detailData.innovation ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="自评总分">{{ detailData.selfScore ?? '未提交' }}</el-descriptions-item>
          <el-descriptions-item label="上级评分">{{ detailData.managerScore ?? '未评分' }}</el-descriptions-item>
          <el-descriptions-item label="最终得分">{{ detailData.totalScore ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="绩效等级">{{ detailData.grade || '待定' }}</el-descriptions-item>
          <el-descriptions-item label="考核人">{{ detailData.evaluator || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detailData" class="detail-section">
          <div class="detail-label">评语</div>
          <div class="detail-text">{{ detailData.comment || '--' }}</div>
        </div>
        <div v-if="detailData" class="detail-section">
          <div class="detail-label">自我总结</div>
          <div class="detail-text">{{ detailData.selfSummary || '--' }}</div>
        </div>
        <div v-if="detailData" class="detail-section">
          <div class="detail-label">改进计划</div>
          <div class="detail-text">{{ detailData.improvementPlan || '--' }}</div>
        </div>
        <div v-if="detailData && detailData.interviewNote" class="detail-section">
          <div class="detail-label">面谈记录（{{ detailData.interviewTime || '' }}）</div>
          <div class="detail-text">{{ detailData.interviewNote }}</div>
        </div>
      </div>
    </el-dialog>

    <!-- 上级评分 -->
    <el-dialog v-model="managerVisible" title="上级评分" width="560px">
      <el-form :model="managerForm" label-width="100px">
        <el-form-item label="工作质量">
          <el-input-number v-model="managerForm.workQuality" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工作效率">
          <el-input-number v-model="managerForm.workEfficiency" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工作态度">
          <el-input-number v-model="managerForm.workAttitude" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="团队协作">
          <el-input-number v-model="managerForm.teamwork" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="创新改进">
          <el-input-number v-model="managerForm.innovation" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="managerForm.comment" type="textarea" :rows="3" placeholder="请输入评语" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="managerVisible = false">取消</el-button>
        <el-button type="primary" :loading="managerSaving" @click="submitManagerReview">提交评分</el-button>
      </template>
    </el-dialog>

    <!-- 绩效面谈 -->
    <el-dialog v-model="interviewVisible" title="绩效面谈" width="560px">
      <el-form :model="interviewForm" label-width="100px">
        <el-form-item label="面谈时间">
          <el-date-picker
            v-model="interviewForm.interviewTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择面谈时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="面谈记录" required>
          <el-input v-model="interviewForm.interviewNote" type="textarea" :rows="5" placeholder="请记录面谈内容、改进共识与后续计划" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="interviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="interviewSaving" @click="submitInterview">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入绩效 -->
    <el-dialog v-model="importVisible" title="导入绩效" width="600px">
      <el-alert
        title="请先下载模板并按表头填写：工号/姓名至少填一项，考核周期格式为 yyyy-MM，五个维度分数 0-100"
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
      />
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="onImportFileChange"
        :on-remove="() => (importFile = null)"
      >
        <el-icon class="el-icon--upload"><Upload /></el-icon>
        <div class="el-upload__text">将 Excel 拖到此处，或<em>点击选择</em></div>
      </el-upload>
      <div v-if="importResult" class="import-result">
        <div class="import-summary">
          成功 {{ importResult.success }} 条，失败 {{ importResult.failed }} 条
        </div>
        <el-table
          v-if="importResult.errors && importResult.errors.length"
          :data="importResult.errors"
          size="small"
          border
          max-height="220"
        >
          <el-table-column prop="row" label="行号" width="70" />
          <el-table-column prop="empNo" label="工号" width="100" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="reason" label="失败原因" min-width="180" show-overflow-tooltip />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="downloadTemplate">下载模板</el-button>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Refresh, Plus, Upload, Download, Setting, TrendCharts,
  Trophy, Medal, Star, Warning
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  getPerformanceListApi, getPerformanceStatsApi, addPerformanceApi, exportPerformanceApi,
  getPerformanceIndicatorsApi, updatePerformanceIndicatorsApi, createReviewCycleApi,
  getPerformanceDetailApi, submitSelfReviewApi, submitManagerReviewApi, performanceInterviewApi,
  downloadPerformanceTemplateApi, importPerformanceApi
} from '@/api/performance'
import { getEmployeeListApi } from '@/api/employee'
import { getDepartmentListApi } from '@/api/department'

const loading = ref(false)
const reviewDialogVisible = ref(false)
const addDialogVisible = ref(false)
const addFormRef = ref(null)
const submitLoading = ref(false)

const pad = (n) => String(n).padStart(2, '0')

// P042：考核周期改为按自然月生成（与后端 period 字段 yyyy-MM 格式一致），不再写死
const buildPeriodOptions = () => {
  const options = []
  const now = new Date()
  for (let i = 0; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
    options.push({ label: `${d.getFullYear()}年${d.getMonth() + 1}月`, value })
  }
  return options
}
const periodOptions = ref(buildPeriodOptions())

const searchForm = reactive({
  cycleId: periodOptions.value[0].value,
  name: '',
  departmentId: '',
  grade: '',
  status: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 当前选中周期信息（展示用）
const currentCycle = computed(() => {
  const period = searchForm.cycleId || periodOptions.value[0].value
  const [year, month] = period.split('-').map(Number)
  const now = new Date()
  const isOngoing = year === now.getFullYear() && month === now.getMonth() + 1
  return {
    name: `${year}年${month}月`,
    startDate: `${period}-01`,
    endDate: `${period}-${new Date(year, month, 0).getDate()}`,
    status: isOngoing ? 'ongoing' : 'ended'
  }
})

// P042：等级统计改为真实接口数据（后端只评定 A/B/C/D，S 由总数减去 A/B/C/D 推得）
const stats = reactive({
  gradeS: 0,
  gradeA: 0,
  gradeB: 0,
  gradeC: 0
})

// P042：绩效列表改为真实接口加载（后端按周期查询全量，前端做条件过滤+分页）
const allRows = ref([])
const tableData = ref([])
const departmentOptions = ref([])
const employeeOptions = ref([])
let deptNameMap = {}
let empMap = {}

const currentReview = reactive({
  name: '',
  department: ''
})

const reviewForm = reactive({
  selfSummary: '',
  improvementPlan: ''
})

const addForm = reactive({
  empId: null,
  period: periodOptions.value[0].value,
  workQuality: 0,
  workEfficiency: 0,
  workAttitude: 0,
  teamwork: 0,
  innovation: 0,
  comment: ''
})

const addRules = {
  empId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  period: [{ required: true, message: '请选择考核周期', trigger: 'change' }]
}

const getGradeTagType = (grade) => {
  const map = { S: 'success', A: 'primary', B: 'warning', C: 'danger', D: 'info' }
  return map[grade] || 'info'
}

const getStatusText = (status) => {
  const map = {
    pending_self: '待自评',
    pending_manager: '待上级评',
    pending_interview: '待面谈',
    completed: '已完成'
  }
  return map[status] || '未知'
}

const getStatusTagType = (status) => {
  const map = {
    pending_self: 'warning',
    pending_manager: 'primary',
    pending_interview: 'success',
    completed: 'info'
  }
  return map[status] || 'info'
}

const normalizeRow = (item) => {
  const emp = empMap[item.empId] || {}
  return {
    id: item.id,
    employeeNo: emp.empNo || '-',
    name: emp.name || '-',
    department: emp.departmentName || '-',
    position: emp.position || '-',
    // 自评/上级评分/流程状态来自绩效流程字段
    selfScore: item.selfScore != null ? Number(item.selfScore) : null,
    managerScore: item.managerScore != null ? Number(item.managerScore) : null,
    finalScore: item.totalScore != null ? Number(item.totalScore) : null,
    grade: item.grade || null,
    status: item.flowStatus || 'completed',
    flowStatus: item.flowStatus || 'completed'
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
    const [listRes, statsRes] = await Promise.all([
      getPerformanceListApi({ period: searchForm.cycleId }),
      getPerformanceStatsApi({ period: searchForm.cycleId })
    ])
    allRows.value = (listRes.data || []).map(normalizeRow)
    const s = statsRes.data || {}
    const abcd = (s.gradeACount || 0) + (s.gradeBCount || 0) + (s.gradeCCount || 0) + (s.gradeDCount || 0)
    stats.gradeS = Math.max((s.totalCount || 0) - abcd, 0)
    stats.gradeA = s.gradeACount || 0
    stats.gradeB = s.gradeBCount || 0
    stats.gradeC = (s.gradeCCount || 0) + (s.gradeDCount || 0)
    refreshView()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}

/** 前端条件过滤 + 分页切片（后端列表接口仅支持按周期查询） */
const refreshView = () => {
  let rows = allRows.value
  if (searchForm.name) rows = rows.filter(r => r.name.includes(searchForm.name))
  if (searchForm.departmentId) rows = rows.filter(r => r.department === (deptNameMap[searchForm.departmentId] || ''))
  if (searchForm.grade) rows = rows.filter(r => r.grade === searchForm.grade)
  if (searchForm.status) rows = rows.filter(r => r.status === searchForm.status)
  pagination.total = rows.length
  const startIdx = (pagination.currentPage - 1) * pagination.pageSize
  tableData.value = rows.slice(startIdx, startIdx + pagination.pageSize)
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  searchForm.cycleId = periodOptions.value[0].value
  pagination.currentPage = 1
  loadData()
}

const handleAdd = () => {
  Object.assign(addForm, {
    empId: null,
    period: searchForm.cycleId || periodOptions.value[0].value,
    workQuality: 0,
    workEfficiency: 0,
    workAttitude: 0,
    teamwork: 0,
    innovation: 0,
    comment: ''
  })
  addDialogVisible.value = true
}

const submitAdd = async () => {
  if (!addFormRef.value) return
  await addFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      // 总分与绩效等级由后端计算
      await addPerformanceApi({
        empId: addForm.empId,
        period: addForm.period,
        workQuality: addForm.workQuality,
        workEfficiency: addForm.workEfficiency,
        workAttitude: addForm.workAttitude,
        teamwork: addForm.teamwork,
        innovation: addForm.innovation,
        comment: addForm.comment
      })
      ElMessage.success('新增成功')
      addDialogVisible.value = false
      await loadData()
    } finally {
      submitLoading.value = false
    }
  })
}

// ===== 导入绩效：模板下载 + Excel 上传 =====
const importVisible = ref(false)
const importLoading = ref(false)
const importFile = ref(null)
const importResult = ref(null)

const handleImport = () => {
  importFile.value = null
  importResult.value = null
  importVisible.value = true
}

const onImportFileChange = (file) => {
  importFile.value = file.raw
}

const downloadTemplate = async () => {
  try {
    const response = await downloadPerformanceTemplateApi()
    const contentType = response.headers['content-type'] || ''
    if (contentType.includes('application/json')) {
      const text = await response.data.text()
      ElMessage.error(JSON.parse(text).message || '模板下载失败')
      return
    }
    const blob = new Blob([response.data], { type: contentType })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '绩效导入模板.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

const submitImport = async () => {
  if (!importFile.value) {
    ElMessage.warning('请选择要导入的 Excel 文件')
    return
  }
  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    const res = await importPerformanceApi(formData)
    importResult.value = res.data || { success: 0, failed: 0, errors: [] }
    ElMessage.success(`导入完成：成功 ${importResult.value.success} 条，失败 ${importResult.value.failed} 条`)
    if (importResult.value.success > 0) {
      loadData()
    }
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    importLoading.value = false
  }
}

const handleExport = async () => {
  const period = searchForm.cycleId || periodOptions.value[0].value
  try {
    const response = await exportPerformanceApi({ period })
    // 后端出错时返回 JSON，需要先识别再下载
    const contentType = response.headers['content-type'] || ''
    if (contentType.includes('application/json')) {
      const text = await response.data.text()
      ElMessage.error(JSON.parse(text).message || '导出失败')
      return
    }
    const blob = new Blob([response.data], { type: contentType })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `绩效报表_${period}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

// 指标配置：从后端加载指标，可编辑权重后整体保存
const indicatorVisible = ref(false)
const indicatorSaving = ref(false)
const indicatorList = ref([
  { id: null, name: '工作业绩', weight: 40, description: '工作目标完成情况' },
  { id: null, name: '工作能力', weight: 25, description: '专业技能与学习能力' },
  { id: null, name: '工作态度', weight: 20, description: '责任心与积极性' },
  { id: null, name: '创新改进', weight: 15, description: '创新意识与流程优化' }
])
const indicatorWeightTotal = computed(() =>
  indicatorList.value.reduce((acc, cur) => acc + Number(cur.weight || 0), 0)
)

const handleIndicators = async () => {
  indicatorVisible.value = true
  try {
    const res = await getPerformanceIndicatorsApi()
    const list = (res && res.data) || []
    if (list.length) {
      indicatorList.value = list.map(it => ({
        id: it.id,
        name: it.indicatorName,
        weight: it.weight,
        description: it.description,
        sort: it.sort
      }))
    }
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

const saveIndicators = async () => {
  if (indicatorWeightTotal.value !== 100) {
    ElMessage.warning('权重合计建议为 100%，当前为 ' + indicatorWeightTotal.value + '%')
  }
  indicatorSaving.value = true
  try {
    await updatePerformanceIndicatorsApi(indicatorList.value.map(it => ({
      id: it.id,
      indicatorName: it.name,
      weight: it.weight,
      description: it.description,
      sort: it.sort
    })))
    ElMessage.success('保存成功')
    indicatorVisible.value = false
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    indicatorSaving.value = false
  }
}

// 创建考核周期
const cycleVisible = ref(false)
const cycleSaving = ref(false)
const cycleForm = reactive({ cycleName: '', period: '', startDate: '', endDate: '' })

const handleCreateCycle = () => {
  const period = searchForm.cycleId || periodOptions.value[0].value
  const [year, month] = period.split('-').map(Number)
  const lastDay = new Date(year, month, 0).getDate()
  Object.assign(cycleForm, {
    cycleName: `${year}年${month}月考核`,
    period,
    startDate: `${period}-01`,
    endDate: `${period}-${String(lastDay).padStart(2, '0')}`
  })
  cycleVisible.value = true
}

const saveCycle = async () => {
  if (!cycleForm.cycleName || !cycleForm.period) {
    ElMessage.warning('请填写周期名称和考核月份')
    return
  }
  cycleSaving.value = true
  try {
    await createReviewCycleApi({ ...cycleForm, status: 1 })
    ElMessage.success('创建成功')
    cycleVisible.value = false
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    cycleSaving.value = false
  }
}

// 绩效分析：基于 /performance/statistics 展示等级分布
const analyticsVisible = ref(false)
const analyticsLoading = ref(false)
const analyticsPeriod = ref('')
const analytics = reactive({
  totalCount: 0,
  avgScore: 0,
  gradeACount: 0,
  gradeBCount: 0,
  gradeCCount: 0,
  gradeDCount: 0
})

const gradeBars = computed(() => {
  const items = [
    { label: 'A 级（优秀）', count: analytics.gradeACount || 0, color: '#67c23a' },
    { label: 'B 级（良好）', count: analytics.gradeBCount || 0, color: '#409eff' },
    { label: 'C 级（合格）', count: analytics.gradeCCount || 0, color: '#e6a23c' },
    { label: 'D 级（待改进）', count: analytics.gradeDCount || 0, color: '#f56c6c' }
  ]
  const sum = items.reduce((acc, cur) => acc + cur.count, 0)
  return items.map(it => ({ ...it, percent: sum ? Math.round(it.count / sum * 100) : 0 }))
})

const handleAnalytics = async () => {
  const period = searchForm.cycleId || periodOptions.value[0].value
  analyticsPeriod.value = period
  analyticsVisible.value = true
  analyticsLoading.value = true
  try {
    const res = await getPerformanceStatsApi({ period })
    const d = (res && res.data) || {}
    analytics.totalCount = d.totalCount || 0
    analytics.avgScore = d.avgScore != null ? d.avgScore : 0
    analytics.gradeACount = d.gradeACount || 0
    analytics.gradeBCount = d.gradeBCount || 0
    analytics.gradeCCount = d.gradeCCount || 0
    analytics.gradeDCount = d.gradeDCount || 0
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    analyticsLoading.value = false
  }
}

// ===== 绩效详情 =====
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)

const handleView = async (row) => {
  detailData.value = { ...row }
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getPerformanceDetailApi(row.id)
    if (res.data) {
      detailData.value = { ...row, ...res.data }
    }
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    detailLoading.value = false
  }
}

// ===== 员工自评 =====
const reviewTargetId = ref(null)
const reviewSubmitting = ref(false)

const handleSelfReview = async (row) => {
  reviewTargetId.value = row.id
  currentReview.name = row.name
  currentReview.department = row.department
  reviewForm.selfSummary = ''
  reviewForm.improvementPlan = ''
  try {
    const res = await getPerformanceIndicatorsApi()
    const list = (res && res.data) || []
    if (list.length) {
      indicatorList.value = list.map(it => ({
        id: it.id,
        name: it.indicatorName,
        weight: it.weight,
        description: it.description,
        sort: it.sort,
        selfScore: 0
      }))
    } else {
      indicatorList.value.forEach(item => { item.selfScore = 0 })
    }
  } catch (e) {
    indicatorList.value.forEach(item => { item.selfScore = 0 })
  }
  reviewDialogVisible.value = true
}

const submitSelfReview = async () => {
  if (!reviewTargetId.value) return
  const totalWeight = indicatorList.value.reduce((acc, cur) => acc + Number(cur.weight || 0), 0) || 100
  const weighted = indicatorList.value.reduce(
    (acc, cur) => acc + Number(cur.selfScore || 0) * Number(cur.weight || 0), 0
  )
  const selfScore = Math.round((weighted / totalWeight) * 100) / 100
  reviewSubmitting.value = true
  try {
    await submitSelfReviewApi(reviewTargetId.value, {
      selfScore,
      selfSummary: reviewForm.selfSummary,
      improvementPlan: reviewForm.improvementPlan
    })
    ElMessage.success('自评已提交，进入上级评分环节')
    reviewDialogVisible.value = false
    await loadData()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    reviewSubmitting.value = false
  }
}

// ===== 上级评分 =====
const managerVisible = ref(false)
const managerSaving = ref(false)
const managerTargetId = ref(null)
const managerForm = reactive({
  workQuality: 0,
  workEfficiency: 0,
  workAttitude: 0,
  teamwork: 0,
  innovation: 0,
  comment: ''
})

const handleManagerReview = async (row) => {
  managerTargetId.value = row.id
  try {
    const res = await getPerformanceDetailApi(row.id)
    const d = (res && res.data) || {}
    Object.assign(managerForm, {
      workQuality: Number(d.workQuality || 0),
      workEfficiency: Number(d.workEfficiency || 0),
      workAttitude: Number(d.workAttitude || 0),
      teamwork: Number(d.teamwork || 0),
      innovation: Number(d.innovation || 0),
      comment: d.comment || ''
    })
  } catch (e) {
    Object.assign(managerForm, {
      workQuality: 0,
      workEfficiency: 0,
      workAttitude: 0,
      teamwork: 0,
      innovation: 0,
      comment: ''
    })
  }
  managerVisible.value = true
}

const submitManagerReview = async () => {
  if (!managerTargetId.value) return
  managerSaving.value = true
  try {
    await submitManagerReviewApi(managerTargetId.value, { ...managerForm })
    ElMessage.success('评分已提交，进入绩效面谈环节')
    managerVisible.value = false
    await loadData()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    managerSaving.value = false
  }
}

// ===== 绩效面谈 =====
const interviewVisible = ref(false)
const interviewSaving = ref(false)
const interviewTargetId = ref(null)
const interviewForm = reactive({
  interviewTime: '',
  interviewNote: ''
})

const nowStamp = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const handleInterview = async (row) => {
  interviewTargetId.value = row.id
  interviewForm.interviewTime = nowStamp()
  interviewForm.interviewNote = ''
  try {
    const res = await getPerformanceDetailApi(row.id)
    const d = (res && res.data) || {}
    if (d.interviewNote) {
      interviewForm.interviewNote = d.interviewNote
    }
    if (d.interviewTime) {
      interviewForm.interviewTime = String(d.interviewTime).replace('T', ' ').slice(0, 19)
    }
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
  interviewVisible.value = true
}

const submitInterview = async () => {
  if (!interviewTargetId.value) return
  if (!interviewForm.interviewNote.trim()) {
    ElMessage.warning('请填写面谈记录')
    return
  }
  interviewSaving.value = true
  try {
    await performanceInterviewApi(interviewTargetId.value, { ...interviewForm })
    ElMessage.success('面谈记录已保存')
    interviewVisible.value = false
    await loadData()
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    interviewSaving.value = false
  }
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
})
</script>

<style scoped lang="scss">
.performance-container {
  .cycle-selector {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    background: var(--el-color-primary-light-9);
    border-radius: 8px;
    margin-bottom: 20px;

    .cycle-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .label {
        font-size: 14px;
        color: var(--el-text-color-regular);
      }

      .cycle-date {
        font-size: 13px;
        color: var(--el-text-color-secondary);
      }
    }
  }

  .stat-row {
    margin-bottom: 24px;
  }

  .stat-card {
    display: flex;
    align-items: center;
    gap: 16px;
    background: var(--el-bg-color);
    border-radius: 8px;
    padding: 16px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;

      &.s-grade { background: linear-gradient(135deg, #67C23A, #85ce61); }
      &.a-grade { background: linear-gradient(135deg, var(--el-color-primary), #66b1ff); }
      &.b-grade { background: linear-gradient(135deg, #E6A23C, #ebb563); }
      &.c-grade { background: linear-gradient(135deg, #F56C6C, #f78989); }
    }

    .stat-info {
      .stat-num {
        font-size: 22px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }

      .stat-label {
        font-size: 13px;
        color: var(--el-text-color-secondary);
        margin-top: 4px;
      }
    }
  }

  .final-score {
    font-weight: 600;
    color: var(--el-color-primary);
  }

  .review-header {
    display: flex;
    justify-content: space-around;
    padding: 12px;
    background: var(--el-fill-color-light);
    border-radius: 6px;
    margin-bottom: 20px;
    font-size: 14px;
    color: var(--el-text-color-regular);
  }
}
.analytics-summary {
  margin-bottom: 18px;
}

.summary-card {
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 12px 14px;
}

.summary-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}

.summary-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.analytics-title {
  font-size: 14px;
  font-weight: 600;
  margin: 6px 0 14px;
  color: var(--el-text-color-primary);
}

.grade-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.grade-name {
  width: 96px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.grade-progress {
  flex: 1;
}

.grade-num {
  width: 110px;
  flex-shrink: 0;
  text-align: right;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.weight-total {
  margin-top: 14px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.weight-error {
  color: var(--el-color-danger);
  font-weight: 600;
}

.weight-hint {
  color: var(--el-text-color-secondary);
}

.detail-section {
  margin-top: 14px;
  padding: 12px 14px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
}

.detail-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 6px;
}

.detail-text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
}

.import-result {
  margin-top: 16px;
}

.import-summary {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  margin-bottom: 10px;
}
</style>
