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

      <el-table v-loading="loading" :data="tableData" border stripe @sort-change="handleSort">
        <el-table-column label="发货单号" width="170" prop="shipmentNo" sortable="custom">
          <template #default="{ row }">{{ row.shipment.shipmentNo }}</template>
        </el-table-column>
        <el-table-column prop="orderNo" label="关联订单" width="170" />
        <el-table-column label="发货日期" width="115" prop="shipDate" sortable="custom">
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
        <el-table-column label="预计到港" width="115" prop="eta" sortable="custom">
          <template #default="{ row }">{{ row.shipment.eta || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="95" align="center" prop="status" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.shipment.status)" size="small">{{ statusMap[row.shipment.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <el-button v-if="nextStatus(row.shipment.status)" type="primary" link @click="handleStatus(row, nextStatus(row.shipment.status))">
              {{ statusMap[nextStatus(row.shipment.status)] }}
            </el-button>
            <el-button link :icon="Location" @click="handleOpenTracks(row)">轨迹</el-button>
            <el-tooltip :disabled="row.shipment.status !== 4" content="已签收的单据不能编辑" placement="top">
              <span>
                <el-button type="primary" link :icon="Edit" :disabled="row.shipment.status === 4" @click="handleEdit(row)">编辑</el-button>
              </span>
            </el-tooltip>
            <el-tooltip :disabled="row.shipment.status < 2" content="已发货的单据不能删除" placement="top">
              <span>
                <el-button type="danger" link :icon="Delete" :disabled="row.shipment.status >= 2" @click="handleDelete(row)">删除</el-button>
              </span>
            </el-tooltip>
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
        @size-change="handleSizeChange"
        @current-change="loadData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑发货单' : '新增发货'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-alert
          v-if="isEdit && editStatus >= 2"
          type="info"
          :closable="false"
          show-icon
          title="该发货单已发货，只能修改承运商、跟踪号、集装箱号、起运/目的港、ETD、ETA、运费、备注；发货日期和运输方式已锁定。修改会记入物流轨迹。"
          style="margin-bottom: 14px"
        />
        <el-form-item label="关联订单" prop="orderId">
          <el-select
            v-model="form.orderId"
            placeholder="输入订单号搜索"
            filterable
            remote
            reserve-keyword
            :remote-method="searchOrders"
            :loading="orderLoading"
            :disabled="isEdit"
            style="width: 100%"
          >
            <el-option v-for="o in orderList" :key="o.id" :label="o.orderNo" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="发货日期" prop="shipDate">
              <el-date-picker v-model="form.shipDate" type="date" value-format="YYYY-MM-DD"
                              :disabled="editStatus >= 2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运输方式" prop="transportType">
              <el-select v-model="form.transportType" :disabled="editStatus >= 2" style="width: 100%">
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
            <el-form-item label="承运商" prop="carrier">
              <el-input v-model="form.carrier" placeholder="如 DHL / COSCO" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="追踪号">
              <el-input v-model="form.trackingNo" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="集装箱号">
              <el-input v-model="form.containerNo" maxlength="50" show-word-limit />
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
            <el-form-item label="起运港" prop="portFrom">
              <el-input v-model="form.portFrom" placeholder="如 上海港" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目的港" prop="portTo">
              <el-input v-model="form.portTo" placeholder="如 洛杉矶港" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="预计离港" prop="etd">
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
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 物流轨迹（P018） -->
    <el-drawer v-model="trackVisible" :title="`物流轨迹 · ${trackShipmentNo}`" size="420px">
      <div v-loading="trackLoading">
        <el-empty v-if="!trackLoading && tracks.length === 0" description="暂无轨迹节点" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="t in tracks"
            :key="t.id"
            :timestamp="formatTime(t.trackTime)"
            :type="trackType(t.status)"
            placement="top"
          >
            <div class="track-node">{{ t.node }}</div>
            <div class="track-line" v-if="t.location">地点：{{ t.location }}</div>
            <div class="track-line" v-if="t.content">{{ t.content }}</div>
            <div class="track-line track-op">操作人：{{ t.operator || '-' }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Location } from '@element-plus/icons-vue'
import {
  getShipmentPageApi, addShipmentApi, updateShipmentApi,
  updateShipmentStatusApi, deleteShipmentApi, getOrderPageApi, getShipmentTracksApi
} from '@/api/trade'

const statusMap = { 1: '待发货', 2: '运输中', 3: '清关中', 4: '已签收' }
const statusTag = (s) => ({ 1: 'info', 2: 'primary', 3: 'warning', 4: 'success' }[s] || 'info')
const nextStatus = (s) => ({ 1: 2, 2: 3, 3: 4 }[s] || null)
const trackType = (s) => ({ 1: 'info', 2: 'primary', 3: 'warning', 4: 'success' }[s] || 'info')
const formatTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
// 编辑中的单据状态：>=2 表示已发货，只开放物流执行字段
const editStatus = ref(1)
const formRef = ref(null)

const searchForm = reactive({ keyword: '', status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0, sortField: null, sortOrder: null })

const defaultForm = {
  id: null, orderId: null, shipDate: new Date().toISOString().slice(0, 10),
  transportType: '海运', carrier: '', trackingNo: '', containerNo: '',
  portFrom: '', portTo: '', etd: null, eta: null, freightCostUsd: 0, remark: ''
}
const form = reactive({ ...defaultForm })

// 新增时的必填项：订单、日期、运输方式、承运商、起运港、目的港、ETD
// 追踪号/集装箱号发货后才有、ETA 会延误、运费后定，这三项在推进「运输中」时才卡
const baseRules = {
  orderId: [{ required: true, message: '请选择关联订单', trigger: 'change' }],
  shipDate: [{ required: true, message: '请选择发货日期', trigger: 'change' }]
}
const createOnlyRules = {
  transportType: [{ required: true, message: '请选择运输方式', trigger: 'change' }],
  carrier: [{ required: true, message: '请填写承运商', trigger: 'blur' }],
  portFrom: [{ required: true, message: '请填写起运港', trigger: 'blur' }],
  portTo: [{ required: true, message: '请填写目的港', trigger: 'blur' }],
  etd: [{ required: true, message: '请选择预计离港日期', trigger: 'change' }]
}
// 编辑不开必填：历史空单要能补字段、也能只改备注，否则单据改不动
const rules = computed(() => (isEdit.value ? baseRules : { ...baseRules, ...createOnlyRules }))

const orderList = ref([])
const orderLoading = ref(false)

// 轨迹
const trackVisible = ref(false)
const trackLoading = ref(false)
const tracks = ref([])
const trackShipmentNo = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getShipmentPageApi({
      ...searchForm,
      sortField: pagination.sortField,
      sortOrder: pagination.sortOrder,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data.list
    pagination.total = res.data.total
    if (tableData.value.length === 0 && pagination.pageNum > 1) {
      pagination.pageNum -= 1
      await loadData()
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNum = 1; loadData() }
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = null
  pagination.sortField = null
  pagination.sortOrder = null
  handleSearch()
}

// 排序（P004）
const handleSort = ({ prop, order }) => {
  pagination.sortField = order ? prop : null
  pagination.sortOrder = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : null
  handleSearch()
}

// 切换每页条数回到第 1 页（P031）
const handleSizeChange = () => { pagination.pageNum = 1; loadData() }

// 编辑时只允许改这些业务字段，避免把 status、createTime、deleted 一并提交回去
const EDITABLE_FIELDS = ['id', 'shipDate', 'transportType', 'carrier', 'trackingNo', 'containerNo',
  'portFrom', 'portTo', 'etd', 'eta', 'freightCostUsd', 'remark']
const pickEditable = (src) => {
  const out = {}
  EDITABLE_FIELDS.forEach((k) => { if (src[k] !== undefined) out[k] = src[k] })
  return out
}

const handleAdd = () => {
  Object.assign(form, defaultForm)
  editStatus.value = 1
  isEdit.value = false
  dialogVisible.value = true
}
const handleEdit = (row) => {
  // 关联订单只在弹窗里展示，不参与提交
  Object.assign(form, defaultForm, pickEditable(row.shipment), { orderId: row.shipment.orderId })
  editStatus.value = row.shipment.status ?? 1
  isEdit.value = true
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateShipmentApi(pickEditable(form))
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
  try {
    await ElMessageBox.confirm(
      `确定将发货单「${row.shipment.shipmentNo}」变更为「${statusMap[status]}」吗？`
        + (status === 2 ? ' 变更后会按订单明细扣减库存。' : ''),
      '提示',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  await updateShipmentStatusApi(row.shipment.id, status)
  ElMessage.success('状态更新成功')
  loadData()
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除发货单「${row.shipment.shipmentNo}」吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteShipmentApi(row.shipment.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleOpenTracks = async (row) => {
  trackShipmentNo.value = row.shipment.shipmentNo
  trackVisible.value = true
  trackLoading.value = true
  try {
    const res = await getShipmentTracksApi(row.shipment.id)
    tracks.value = res.data || []
  } finally {
    trackLoading.value = false
  }
}

// 关联订单下拉：远程搜索，不再写死只取前 100 条（P032）
const searchOrders = async (keyword) => {
  orderLoading.value = true
  try {
    const res = await getOrderPageApi({ keyword: keyword || '', pageNum: 1, pageSize: 50 })
    orderList.value = (res.data.list || []).map(item => item.order)
  } finally {
    orderLoading.value = false
  }
}

onMounted(() => {
  loadData()
  searchOrders('')
})
</script>

<style scoped>
.page-container { width: 100%; }
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.track-node { font-weight: 600; color: #303133; margin-bottom: 4px; }
.track-line { font-size: 13px; color: #606266; line-height: 20px; }
.track-op { color: #909399; }
</style>
