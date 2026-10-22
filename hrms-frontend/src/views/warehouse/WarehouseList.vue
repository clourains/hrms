<template>
  <div class="page-container">
    <el-card shadow="hover">
      <el-tabs v-model="activeTab">
        <!-- ========== 仓库管理 ========== -->
        <el-tab-pane label="仓库管理" name="warehouse">
          <div class="search-bar">
            <el-form :inline="true" :model="whSearch">
              <el-form-item label="关键字">
                <el-input v-model="whSearch.keyword" placeholder="编码 / 名称 / 国家" clearable style="width: 200px" @keyup.enter="loadWarehouses" />
              </el-form-item>
              <el-form-item label="类型">
                <el-select v-model="whSearch.type" placeholder="全部" clearable style="width: 130px">
                  <el-option label="国内仓" :value="1" />
                  <el-option label="海外仓" :value="2" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="loadWarehouses">搜索</el-button>
                <el-button :icon="Refresh" @click="whSearch.keyword = ''; whSearch.type = null; loadWarehouses()">重置</el-button>
              </el-form-item>
            </el-form>
            <el-button type="primary" :icon="Plus" @click="handleAddWh">新增仓库</el-button>
          </div>

          <el-table v-loading="whLoading" :data="whList" border stripe>
            <el-table-column prop="warehouseCode" label="仓库编码" width="110" />
            <el-table-column prop="warehouseName" label="仓库名称" min-width="150" />
            <el-table-column prop="type" label="类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.type === 2 ? 'warning' : 'primary'">{{ row.type === 2 ? '海外仓' : '国内仓' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="country" label="国家" width="100" />
            <el-table-column prop="city" label="城市" width="110" />
            <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
            <el-table-column prop="manager" label="负责人" width="100" />
            <el-table-column prop="capacity" label="容量(件)" width="100" align="right">
              <template #default="{ row }">{{ row.capacity ? row.capacity.toLocaleString() : '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link :icon="Edit" @click="handleEditWh(row)">编辑</el-button>
                <el-button type="danger" link :icon="Delete" @click="handleDeleteWh(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ========== 库存总览 ========== -->
        <el-tab-pane label="库存总览" name="inventory">
          <div class="search-bar">
            <el-form :inline="true" :model="invSearch">
              <el-form-item label="产品">
                <el-select v-model="invSearch.productId" placeholder="全部产品" clearable filterable style="width: 200px">
                  <el-option v-for="p in productList" :key="p.id" :label="`${p.name} (${p.sku})`" :value="p.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="仓库">
                <el-select v-model="invSearch.warehouseId" placeholder="全部仓库" clearable filterable style="width: 180px">
                  <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-checkbox v-model="invSearch.lowStock" @change="loadInventory">只看低库存</el-checkbox>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="invPagination.pageNum = 1; loadInventory()">搜索</el-button>
                <el-button :icon="Refresh" @click="invSearch.productId = null; invSearch.warehouseId = null; invSearch.lowStock = false; invPagination.pageNum = 1; loadInventory()">重置</el-button>
              </el-form-item>
            </el-form>
            <el-button type="warning" :icon="Box" @click="handleOpenStockChange">出入库登记</el-button>
          </div>

          <el-table v-loading="invLoading" :data="invList" border stripe>
            <el-table-column prop="productName" label="产品名称" min-width="150" />
            <el-table-column prop="sku" label="SKU" width="110" />
            <el-table-column prop="warehouseName" label="仓库" min-width="140" />
            <el-table-column prop="warehouseType" label="仓库类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.warehouseType === 2 ? 'warning' : 'primary'" size="small">{{ row.warehouseType === 2 ? '海外仓' : '国内仓' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="库存数量" width="100" align="right" />
            <el-table-column prop="lockedQuantity" label="锁定数量" width="100" align="right" />
            <el-table-column prop="availableQuantity" label="可用数量" width="100" align="right">
              <template #default="{ row }">
                <span :style="{ color: isLowStock(row) ? '#F56C6C' : '#303133', fontWeight: isLowStock(row) ? 600 : 400 }">
                  {{ row.availableQuantity }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="warningQuantity" label="预警值" width="90" align="right" />
            <el-table-column label="库存状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag v-if="isLowStock(row)" type="danger" size="small">库存不足</el-tag>
                <el-tag v-else type="success" size="small">正常</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleRowStockChange(row, 1)">入库</el-button>
                <el-button type="warning" link @click="handleRowStockChange(row, 2)">出库</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="invPagination.pageNum"
            v-model:page-size="invPagination.pageSize"
            :total="invPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            class="pagination"
            @size-change="loadInventory"
            @current-change="loadInventory"
          />
        </el-tab-pane>

        <!-- ========== 变动记录 ========== -->
        <el-tab-pane label="变动记录" name="logs">
          <div class="search-bar">
            <el-form :inline="true" :model="logSearch">
              <el-form-item label="变动类型">
                <el-select v-model="logSearch.changeType" placeholder="全部" clearable style="width: 130px">
                  <el-option label="入库" :value="1" />
                  <el-option label="出库" :value="2" />
                  <el-option label="锁定" :value="3" />
                  <el-option label="解锁" :value="4" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="logPagination.pageNum = 1; loadLogs()">搜索</el-button>
                <el-button :icon="Refresh" @click="logSearch.changeType = null; logPagination.pageNum = 1; loadLogs()">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table v-loading="logLoading" :data="logList" border stripe>
            <el-table-column prop="createTime" label="时间" width="170" />
            <el-table-column prop="productName" label="产品" min-width="140" />
            <el-table-column prop="sku" label="SKU" width="110" />
            <el-table-column prop="warehouseName" label="仓库" min-width="130" />
            <el-table-column prop="changeType" label="类型" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="changeTypeTag(row.changeType)" size="small">{{ changeTypeText(row.changeType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="changeQuantity" label="变动数量" width="100" align="right">
              <template #default="{ row }">
                <span :style="{ color: row.changeQuantity >= 0 ? '#67C23A' : '#F56C6C', fontWeight: 600 }">
                  {{ row.changeQuantity >= 0 ? '+' : '' }}{{ row.changeQuantity }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="afterQuantity" label="变动后库存" width="110" align="right" />
            <el-table-column prop="orderNo" label="关联订单" width="160" />
            <el-table-column prop="operatorName" label="操作人" width="100" />
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
          </el-table>

          <el-pagination
            v-model:current-page="logPagination.pageNum"
            v-model:page-size="logPagination.pageSize"
            :total="logPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            class="pagination"
            @size-change="loadLogs"
            @current-change="loadLogs"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 仓库编辑对话框 -->
    <el-dialog v-model="whDialogVisible" :title="isEditWh ? '编辑仓库' : '新增仓库'" width="600px" destroy-on-close>
      <el-form ref="whFormRef" :model="whForm" :rules="whRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="仓库编码" prop="warehouseCode">
              <el-input v-model="whForm.warehouseCode" placeholder="如 WH-CN-01" :disabled="isEditWh" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库类型" prop="type">
              <el-radio-group v-model="whForm.type">
                <el-radio :label="1">国内仓</el-radio>
                <el-radio :label="2">海外仓</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="仓库名称" prop="warehouseName">
          <el-input v-model="whForm.warehouseName" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="国家" prop="country">
              <el-input v-model="whForm.country" placeholder="如 中国 / 美国" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市">
              <el-input v-model="whForm.city" placeholder="如 洛杉矶" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址">
          <el-input v-model="whForm.address" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-input v-model="whForm.manager" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="容量(件)">
              <el-input-number v-model="whForm.capacity" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-radio-group v-model="whForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="whDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitWh">确定</el-button>
      </template>
    </el-dialog>

    <!-- 出入库对话框 -->
    <el-dialog v-model="stockDialogVisible" title="出入库登记" width="560px" destroy-on-close>
      <el-form ref="stockFormRef" :model="stockForm" :rules="stockRules" label-width="110px">
        <el-form-item label="产品" prop="productId">
          <el-select v-model="stockForm.productId" placeholder="选择产品" filterable style="width: 100%">
            <el-option v-for="p in productList" :key="p.id" :label="`${p.name} (${p.sku})`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="stockForm.warehouseId" placeholder="选择仓库" filterable style="width: 100%">
            <el-option v-for="w in warehouseList" :key="w.id" :label="`${w.warehouseName} (${w.type === 2 ? '海外仓' : '国内仓'})`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="变动类型" prop="changeType">
          <el-radio-group v-model="stockForm.changeType">
            <el-radio :label="1">入库</el-radio>
            <el-radio :label="2">出库</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="stockForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stockForm.remark" type="textarea" :rows="2" placeholder="如 采购入库 / 订单出库" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitStock">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Box } from '@element-plus/icons-vue'
import {
  getWarehouseListApi, addWarehouseApi, updateWarehouseApi, deleteWarehouseApi,
  getInventoryPageApi, getInventoryLogsApi, changeStockApi, getProductListApi
} from '@/api/trade'

const activeTab = ref('warehouse')
const submitting = ref(false)

// ========== 仓库管理 ==========
const whLoading = ref(false)
const whList = ref([])
const whSearch = reactive({ keyword: '', type: null })
const whDialogVisible = ref(false)
const isEditWh = ref(false)
const whFormRef = ref(null)

const defaultWhForm = { id: null, warehouseCode: '', warehouseName: '', type: 1, country: '中国', city: '', address: '', manager: '', capacity: 10000, status: 1 }
const whForm = reactive({ ...defaultWhForm })

const whRules = {
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  country: [{ required: true, message: '请输入国家', trigger: 'blur' }]
}

const loadWarehouses = async () => {
  whLoading.value = true
  try {
    const res = await getWarehouseListApi()
    let list = res.data || []
    if (whSearch.keyword) {
      const kw = whSearch.keyword.toLowerCase()
      list = list.filter(w =>
        (w.warehouseCode || '').toLowerCase().includes(kw) ||
        (w.warehouseName || '').toLowerCase().includes(kw) ||
        (w.country || '').toLowerCase().includes(kw)
      )
    }
    if (whSearch.type != null) {
      list = list.filter(w => w.type === whSearch.type)
    }
    whList.value = list
  } finally {
    whLoading.value = false
  }
}

const handleAddWh = () => { Object.assign(whForm, defaultWhForm); isEditWh.value = false; whDialogVisible.value = true }
const handleEditWh = (row) => { Object.assign(whForm, defaultWhForm, row); isEditWh.value = true; whDialogVisible.value = true }

const handleSubmitWh = async () => {
  await whFormRef.value.validate()
  submitting.value = true
  try {
    if (isEditWh.value) {
      await updateWarehouseApi(whForm)
      ElMessage.success('更新成功')
    } else {
      await addWarehouseApi(whForm)
      ElMessage.success('新增成功')
    }
    whDialogVisible.value = false
    loadWarehouses()
    loadWarehouseOptions()
  } finally {
    submitting.value = false
  }
}

const handleDeleteWh = async (row) => {
  await ElMessageBox.confirm(`确定删除仓库「${row.warehouseName}」吗？`, '提示', { type: 'warning' })
  await deleteWarehouseApi(row.id)
  ElMessage.success('删除成功')
  loadWarehouses()
  loadWarehouseOptions()
}

// ========== 库存总览 ==========
const invLoading = ref(false)
const invList = ref([])
const invSearch = reactive({ productId: null, warehouseId: null, lowStock: false })
const invPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const isLowStock = (row) => (row.availableQuantity ?? 0) <= (row.warningQuantity ?? 0)

const loadInventory = async () => {
  invLoading.value = true
  try {
    const res = await getInventoryPageApi({
      productId: invSearch.productId,
      warehouseId: invSearch.warehouseId,
      lowStock: invSearch.lowStock,
      pageNum: invPagination.pageNum,
      pageSize: invPagination.pageSize
    })
    invList.value = res.data.list
    invPagination.total = res.data.total
  } finally {
    invLoading.value = false
  }
}

// ========== 出入库 ==========
const stockDialogVisible = ref(false)
const stockFormRef = ref(null)
const defaultStockForm = { productId: null, warehouseId: null, changeType: 1, quantity: 1, remark: '' }
const stockForm = reactive({ ...defaultStockForm })

const stockRules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }]
}

const handleOpenStockChange = () => { Object.assign(stockForm, defaultStockForm); stockDialogVisible.value = true }

const handleRowStockChange = (row, changeType) => {
  Object.assign(stockForm, defaultStockForm, { productId: row.productId, warehouseId: row.warehouseId, changeType })
  stockDialogVisible.value = true
}

const handleSubmitStock = async () => {
  await stockFormRef.value.validate()
  submitting.value = true
  try {
    await changeStockApi(stockForm)
    ElMessage.success('库存变动成功')
    stockDialogVisible.value = false
    loadInventory()
  } finally {
    submitting.value = false
  }
}

// ========== 变动记录 ==========
const logLoading = ref(false)
const logList = ref([])
const logSearch = reactive({ changeType: null })
const logPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const changeTypeText = (t) => ({ 1: '入库', 2: '出库', 3: '锁定', 4: '解锁' }[t] || '未知')
const changeTypeTag = (t) => ({ 1: 'success', 2: 'danger', 3: 'warning', 4: 'info' }[t] || 'info')

const loadLogs = async () => {
  logLoading.value = true
  try {
    const res = await getInventoryLogsApi({
      changeType: logSearch.changeType,
      pageNum: logPagination.pageNum,
      pageSize: logPagination.pageSize
    })
    logList.value = res.data.list
    logPagination.total = res.data.total
  } finally {
    logLoading.value = false
  }
}

// ========== 下拉选项 ==========
const productList = ref([])
const warehouseList = ref([])

const loadProductOptions = async () => {
  const res = await getProductListApi()
  productList.value = res.data || []
}

const loadWarehouseOptions = async () => {
  const res = await getWarehouseListApi()
  warehouseList.value = res.data || []
}

onMounted(() => {
  loadWarehouses()
  loadInventory()
  loadLogs()
  loadProductOptions()
  loadWarehouseOptions()
})
</script>

<style scoped>
.page-container { width: 100%; }
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
