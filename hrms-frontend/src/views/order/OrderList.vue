<template>
  <div class="page-container">
    <el-card shadow="hover">
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="关键字">
            <el-input v-model="searchForm.keyword" placeholder="订单号" clearable style="width: 200px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="订单状态">
            <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
              <el-option v-for="(label, s) in statusMap" :key="s" :label="label" :value="Number(s)" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新建订单</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="订单号" width="170">
          <template #default="{ row }">
            <el-link type="primary" @click="handleViewItems(row)">{{ row.order.orderNo }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" min-width="160" show-overflow-tooltip />
        <el-table-column prop="customerCountry" label="国家" width="90" />
        <el-table-column label="订单日期" width="110">
          <template #default="{ row }">{{ row.order.orderDate }}</template>
        </el-table-column>
        <el-table-column label="总数量" width="90" align="right">
          <template #default="{ row }">{{ row.order.totalQuantity }}</template>
        </el-table-column>
        <el-table-column label="总金额(USD)" width="130" align="right">
          <template #default="{ row }">
            <span style="color: #F56C6C; font-weight: 600">${{ formatNum(row.order.totalAmountUsd) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.order.status)" size="small">{{ statusMap[row.order.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="付款状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="payTag(row.order.paymentStatus)" size="small">{{ payMap[row.order.paymentStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.order.status === 0" type="success" link @click="handleStatus(row, 1)">确认</el-button>
            <el-button v-if="row.order.status === 1 || row.order.status === 2" type="primary" link @click="handleStatus(row, 3)">发货</el-button>
            <el-button v-if="row.order.status === 3" type="warning" link @click="handleStatus(row, 4)">签收</el-button>
            <el-button v-if="row.order.status === 4" type="success" link @click="handleStatus(row, 5)">完成</el-button>
            <el-button v-if="row.order.status !== 5 && row.order.status !== 9" type="info" link @click="handleStatus(row, 9)">取消</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新建订单对话框 -->
    <el-dialog v-model="dialogVisible" title="新建销售订单" width="900px" destroy-on-close top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户" prop="customerId">
              <el-select v-model="form.customerId" placeholder="选择客户" filterable style="width: 100%">
                <el-option v-for="c in customerList" :key="c.id" :label="`${c.companyName} (${c.country || ''})`" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单日期" prop="orderDate">
              <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="汇率">
              <el-input-number v-model="form.exchangeRate" :min="0" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="订单备注" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">订单明细</el-divider>

        <el-table :data="form.items" border size="small">
          <el-table-column label="产品" min-width="220">
            <template #default="{ row }">
              <el-select v-model="row.productId" placeholder="选择产品" filterable style="width: 100%" @change="(v) => handleProductChange(row, v)">
                <el-option v-for="p in productList" :key="p.id" :label="`${p.name} (${p.sku})`" :value="p.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="发货仓库" min-width="180">
            <template #default="{ row }">
              <el-select v-model="row.warehouseId" placeholder="选择仓库" filterable style="width: 100%">
                <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="1" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="单价(USD)" width="130">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPriceUsd" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="小计(USD)" width="120" align="right">
            <template #default="{ row }">
              ${{ formatNum((row.quantity || 0) * (row.unitPriceUsd || 0)) }}
            </template>
          </el-table-column>
          <el-table-column label="" width="60" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link :icon="Delete" @click="form.items.splice($index, 1)" />
            </template>
          </el-table-column>
        </el-table>

        <div class="items-footer">
          <el-button type="primary" plain :icon="Plus" @click="addItem">添加明细</el-button>
          <div class="total-info">
            共 <span class="total-num">{{ totalQuantity }}</span> 件，
            合计 <span class="total-amount">${{ formatNum(totalAmount) }}</span>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">创建订单</el-button>
      </template>
    </el-dialog>

    <!-- 订单明细抽屉 -->
    <el-drawer v-model="drawerVisible" :title="`订单明细 - ${currentOrderNo}`" size="640px">
      <el-descriptions v-if="currentOrder" :column="2" border size="small" style="margin-bottom: 16px">
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单日期">{{ currentOrder.orderDate }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="statusTag(currentOrder.status)" size="small">{{ statusMap[currentOrder.status] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="付款状态">
          <el-tag :type="payTag(currentOrder.paymentStatus)" size="small">{{ payMap[currentOrder.paymentStatus] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-table v-loading="itemsLoading" :data="itemsData" border size="small">
        <el-table-column label="产品" min-width="160">
          <template #default="{ row }">{{ productNameOf(row.productId) }}</template>
        </el-table-column>
        <el-table-column label="发货仓库" min-width="130">
          <template #default="{ row }">{{ warehouseNameOf(row.warehouseId) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column label="单价(USD)" width="110" align="right">
          <template #default="{ row }">${{ formatNum(row.unitPriceUsd) }}</template>
        </el-table-column>
        <el-table-column label="小计(USD)" width="110" align="right">
          <template #default="{ row }">${{ formatNum(row.totalPriceUsd) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import {
  getOrderPageApi, createOrderApi, updateOrderStatusApi, deleteOrderApi,
  getOrderItemsApi, getCustomerListApi, getProductListApi, getWarehouseListApi
} from '@/api/trade'

const statusMap = { 0: '待确认', 1: '已确认', 2: '备货中', 3: '已发货', 4: '已签收', 5: '已完成', 9: '已取消' }
const payMap = { 0: '未付款', 1: '部分付款', 2: '已付清' }

const statusTag = (s) => ({ 0: 'info', 1: 'primary', 2: 'warning', 3: 'warning', 4: 'success', 5: 'success', 9: 'danger' }[s] || 'info')
const payTag = (s) => ({ 0: 'danger', 1: 'warning', 2: 'success' }[s] || 'info')

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

const searchForm = reactive({ keyword: '', status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const defaultItem = () => ({ productId: null, warehouseId: null, quantity: 1, unitPriceUsd: 0 })
const form = reactive({ customerId: null, orderDate: new Date().toISOString().slice(0, 10), exchangeRate: 7.2, remark: '', items: [defaultItem()] })

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择订单日期', trigger: 'change' }]
}

const customerList = ref([])
const productList = ref([])
const warehouseList = ref([])

const formatNum = (n) => (n == null ? '0.00' : Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }))

const validItems = computed(() => form.items.filter(i => i.productId && i.quantity > 0))
const totalQuantity = computed(() => validItems.value.reduce((sum, i) => sum + (i.quantity || 0), 0))
const totalAmount = computed(() => validItems.value.reduce((sum, i) => sum + (i.quantity || 0) * (i.unitPriceUsd || 0), 0))

const loadData = async () => {
  loading.value = true
  try {
    const res = await getOrderPageApi({ ...searchForm, pageNum: pagination.pageNum, pageSize: pagination.pageSize })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNum = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.status = null; handleSearch() }

const handleAdd = () => {
  Object.assign(form, {
    customerId: null,
    orderDate: new Date().toISOString().slice(0, 10),
    exchangeRate: 7.2,
    remark: '',
    items: [defaultItem()]
  })
  dialogVisible.value = true
}

const addItem = () => form.items.push(defaultItem())

const handleProductChange = (row, productId) => {
  const product = productList.value.find(p => p.id === productId)
  if (product) {
    row.unitPriceUsd = product.salePriceUsd || 0
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (validItems.value.length === 0) {
    ElMessage.warning('请至少添加一条有效明细（选择产品并填写数量）')
    return
  }
  submitting.value = true
  try {
    await createOrderApi({
      order: { customerId: form.customerId, orderDate: form.orderDate, exchangeRate: form.exchangeRate, remark: form.remark },
      items: validItems.value.map(i => ({ productId: i.productId, warehouseId: i.warehouseId, quantity: i.quantity, unitPriceUsd: i.unitPriceUsd }))
    })
    ElMessage.success('订单创建成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleStatus = async (row, status) => {
  const actionText = statusMap[status]
  await ElMessageBox.confirm(`确定将订单「${row.order.orderNo}」变更为「${actionText}」吗？`, '提示', { type: 'warning' })
  await updateOrderStatusApi(row.order.id, status)
  ElMessage.success('操作成功')
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除订单「${row.order.orderNo}」吗？`, '提示', { type: 'warning' })
  await deleteOrderApi(row.order.id)
  ElMessage.success('删除成功')
  loadData()
}

// ========== 明细抽屉 ==========
const drawerVisible = ref(false)
const itemsLoading = ref(false)
const itemsData = ref([])
const currentOrder = ref(null)
const currentOrderNo = ref('')

const productNameOf = (id) => {
  const p = productList.value.find(p => p.id === id)
  return p ? `${p.name} (${p.sku})` : `#${id}`
}

const warehouseNameOf = (id) => {
  const w = warehouseList.value.find(w => w.id === id)
  return w ? w.warehouseName : (id ? `#${id}` : '-')
}

const handleViewItems = async (row) => {
  currentOrder.value = row.order
  currentOrderNo.value = row.order.orderNo
  drawerVisible.value = true
  itemsLoading.value = true
  try {
    const res = await getOrderItemsApi(row.order.id)
    itemsData.value = res.data || []
  } finally {
    itemsLoading.value = false
  }
}

const loadOptions = async () => {
  const [customers, products, warehouses] = await Promise.all([
    getCustomerListApi(), getProductListApi(), getWarehouseListApi()
  ])
  customerList.value = customers.data || []
  productList.value = products.data || []
  warehouseList.value = warehouses.data || []
}

onMounted(() => {
  loadData()
  loadOptions()
})
</script>

<style scoped>
.page-container { width: 100%; }
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.items-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; }
.total-info { font-size: 14px; color: #606266; }
.total-num { font-weight: 600; color: #409EFF; }
.total-amount { font-weight: 600; color: #F56C6C; font-size: 16px; }
</style>
