<template>
  <div class="page-container">
    <el-card shadow="hover">
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="关键字">
            <el-input v-model="searchForm.keyword" placeholder="发货单号 / 追踪号" clearable style="width: 220px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="物流状态">
            <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
              <el-option v-for="(label, s) in statusMap" :key="s" :label="label" :value="Number(s)" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增发货</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="发货单号" width="160">
          <template #default="{ row }">{{ row.shipment.shipmentNo }}</template>
        </el-table-column>
        <el-table-column prop="orderNo" label="关联订单" width="170" />
        <el-table-column label="发货日期" width="105">
          <template #default="{ row }">{{ row.shipment.shipDate }}</template>
        </el-table-column>
        <el-table-column label="运输方式" width="95" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.shipment.transportType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="承运商" width="130">
          <template #default="{ row }">{{ row.shipment.carrier || '-' }}</template>
        </el-table-column>
        <el-table-column label="追踪号" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.shipment.trackingNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="起运港 → 目的港" min-width="170">
          <template #default="{ row }">{{ row.shipment.portFrom || '-' }} → {{ row.shipment.portTo || '-' }}</template>
        </el-table-column>
        <el-table-column label="预计到港" width="105">
          <template #default="{ row }">{{ row.shipment.eta || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.shipment.status)" size="small">{{ statusMap[row.shipment.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="nextStatus(row.shipment.status)" type="primary" link @click="handleStatus(row, nextStatus(row.shipment.status))">
              {{ statusMap[nextStatus(row.shipment.status)] }}
            </el-button>
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑发货单' : '新增发货'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="关联订单" prop="orderId">
          <el-select v-model="form.orderId" placeholder="选择订单" filterable style="width: 100%" :disabled="isEdit">
            <el-option v-for="o in orderList" :key="o.id" :label="o.orderNo" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="发货日期" prop="shipDate">
              <el-date-picker v-model="form.shipDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运输方式">
              <el-select v-model="form.transportType" style="width: 100%">
                <el-option label="海运" value="海运" />
                <el-option label="空运" value="空运" />
                <el-option label="铁路" value="铁路" />
                <el-option label="快递" value="快递" />
                <el-option label="卡车" value="卡车" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="承运商">
              <el-input v-model="form.carrier" placeholder="如 DHL / COSCO" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="追踪号">
              <el-input v-model="form.trackingNo" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="集装箱号">
              <el-input v-model="form.containerNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运费(USD)">
              <el-input-number v-model="form.freightCostUsd" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="起运港">
              <el-input v-model="form.portFrom" placeholder="如 上海港" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目的港">
              <el-input v-model="form.portTo" placeholder="如 洛杉矶港" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="预计离港">
              <el-date-picker v-model="form.etd" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计到港">
              <el-date-picker v-model="form.eta" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getShipmentPageApi, addShipmentApi, updateShipmentApi,
  updateShipmentStatusApi, deleteShipmentApi, getOrderPageApi
} from '@/api/trade'

const statusMap = { 1: '待发货', 2: '运输中', 3: '清关中', 4: '已签收' }
const statusTag = (s) => ({ 1: 'info', 2: 'primary', 3: 'warning', 4: 'success' }[s] || 'info')
const nextStatus = (s) => ({ 1: 2, 2: 3, 3: 4 }[s] || null)

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const searchForm = reactive({ keyword: '', status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const defaultForm = {
  id: null, orderId: null, shipDate: new Date().toISOString().slice(0, 10),
  transportType: '海运', carrier: '', trackingNo: '', containerNo: '',
  portFrom: '', portTo: '', etd: null, eta: null, freightCostUsd: 0, remark: ''
}
const form = reactive({ ...defaultForm })

const rules = {
  orderId: [{ required: true, message: '请选择关联订单', trigger: 'change' }],
  shipDate: [{ required: true, message: '请选择发货日期', trigger: 'change' }]
}

const orderList = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getShipmentPageApi({ ...searchForm, pageNum: pagination.pageNum, pageSize: pagination.pageSize })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNum = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.status = null; handleSearch() }

const handleAdd = () => { Object.assign(form, defaultForm); isEdit.value = false; dialogVisible.value = true }
const handleEdit = (row) => { Object.assign(form, defaultForm, row.shipment); isEdit.value = true; dialogVisible.value = true }

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateShipmentApi(form)
      ElMessage.success('更新成功')
    } else {
      await addShipmentApi(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleStatus = async (row, status) => {
  await ElMessageBox.confirm(
    `确定将发货单「${row.shipment.shipmentNo}」变更为「${statusMap[status]}」吗？`,
    '提示',
    { type: 'warning' }
  )
  await updateShipmentStatusApi(row.shipment.id, status)
  ElMessage.success('状态更新成功')
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除发货单「${row.shipment.shipmentNo}」吗？`, '提示', { type: 'warning' })
  await deleteShipmentApi(row.shipment.id)
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

<style scoped>
.page-container { width: 100%; }
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
