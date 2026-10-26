<template>
  <div class="page-container page-stack">
    <!-- 页头 -->
    <PageHeader
      title="收付款管理"
      description="登记客户回款与供应商付款，自动折算人民币金额"
      icon="Money"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="handleAdd">登记收付款</el-button>
      </template>
    </PageHeader>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="收付款单号 / 订单号" clearable style="width: 220px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable style="width: 120px">
            <el-option label="收款" :value="1" />
            <el-option label="付款" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-submit">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 收付款列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="单号" width="170">
          <template #default="{ row }">{{ row.payment.paymentNo }}</template>
        </el-table-column>
        <el-table-column prop="orderNo" label="关联订单" width="170" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.payment.type === 1 ? 'success' : 'danger'" size="small">
              {{ row.payment.type === 1 ? '收款' : '付款' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额(USD)" width="130" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.payment.type === 1 ? 'var(--el-color-success)' : 'var(--el-color-danger)', fontWeight: 600 }">
              {{ row.payment.type === 1 ? '+' : '-' }}${{ formatNum(row.payment.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="币种" width="80" align="center">
          <template #default="{ row }">{{ row.payment.currency || 'USD' }}</template>
        </el-table-column>
        <el-table-column label="汇率" width="90" align="right">
          <template #default="{ row }">{{ row.payment.exchangeRate }}</template>
        </el-table-column>
        <el-table-column label="折合人民币" width="130" align="right">
          <template #default="{ row }">¥{{ formatNum(row.payment.amountCny) }}</template>
        </el-table-column>
        <el-table-column label="日期" width="105">
          <template #default="{ row }">{{ row.payment.paymentDate }}</template>
        </el-table-column>
        <el-table-column label="方式" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.payment.method || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.payment.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination page-pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="登记收付款" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :label="1">收款（客户回款）</el-radio>
            <el-radio :label="2">付款（供应商等）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联订单">
          <el-select v-model="form.orderId" placeholder="选择订单（可选）" filterable clearable style="width: 100%">
            <el-option v-for="o in orderList" :key="o.id" :label="o.orderNo" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="金额(USD)" prop="amount">
              <el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="汇率">
              <el-input-number v-model="form.exchangeRate" :min="0" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="收付日期" prop="paymentDate">
              <el-date-picker v-model="form.paymentDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="方式">
              <el-select v-model="form.method" style="width: 100%">
                <el-option label="电汇 T/T" value="电汇" />
                <el-option label="信用证 L/C" value="信用证" />
                <el-option label="PayPal" value="PayPal" />
                <el-option label="西联汇款" value="西联" />
                <el-option label="现金" value="现金" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="如 定金 30% / 尾款" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getPaymentPageApi, addPaymentApi, deletePaymentApi, getOrderPageApi } from '@/api/trade'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

const searchForm = reactive({ keyword: '', type: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const defaultForm = {
  type: 1, orderId: null, amount: null, currency: 'USD', exchangeRate: 7.2,
  paymentDate: new Date().toISOString().slice(0, 10), method: '电汇', remark: ''
}
const form = reactive({ ...defaultForm })

const rules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  paymentDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

const orderList = ref([])

const formatNum = (n) => (n == null ? '0.00' : Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }))

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPaymentPageApi({ ...searchForm, pageNum: pagination.pageNum, pageSize: pagination.pageSize })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNum = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.type = null; handleSearch() }

const handleAdd = () => { Object.assign(form, defaultForm); dialogVisible.value = true }

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    await addPaymentApi(form)
    ElMessage.success('登记成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除收付款单「${row.payment.paymentNo}」吗？`, '提示', { type: 'warning' })
  await deletePaymentApi(row.payment.id)
  ElMessage.success('删除成功')
  loadData()
}

const loadOrderOptions = async () => {
  const res = await getOrderPageApi({ pageNum: 1, pageSize: 100 })
  orderList.value = (res.data.list || []).map(item => item.order)
}

onMounted(() => {
  loadData()
  loadOrderOptions()
})
</script>
