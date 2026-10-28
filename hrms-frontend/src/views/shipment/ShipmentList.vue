<template>
  <div class="page-container page-stack">
    <!-- 页头 -->
    <PageHeader
      title="发货管理"
      description="管理发货单与物流轨迹，跟踪运输状态直至签收"
      icon="Van"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增发货</el-button>
      </template>
    </PageHeader>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="发货单号 / 追踪号" clearable style="width: 220px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="物流状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="(label, s) in statusMap" :key="s" :label="label" :value="Number(s)" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-submit">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 发货单列表 -->
    <div class="table-card">
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

      <div class="pagination page-pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="loadData"
        />
      </div>
    </div>

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

    <!-- 物流轨迹（动态航线） -->
    <el-drawer
      v-model="trackVisible"
      :title="`物流轨迹 · ${trackShipmentNo}`"
      size="640px"
      @closed="stopTrackAnim"
    >
      <div v-loading="trackLoading">
        <!-- 视图切换：简略版（航线条）/ 详细版（淘宝式物流详情） -->
        <div class="track-toolbar">
          <el-radio-group v-model="trackView" size="small">
            <el-radio-button value="simple">简略版</el-radio-button>
            <el-radio-button value="detail">详细版</el-radio-button>
          </el-radio-group>
          <div class="track-toolbar-right">
            <el-tag v-if="trackSource === '17track'" size="small" type="success" effect="plain">17TRACK 实时</el-tag>
            <span v-else class="track-toolbar-tip">本地轨迹</span>
            <el-button
              size="small"
              :icon="Refresh"
              :loading="liveLoading"
              @click="handleSyncLive"
            >同步实时轨迹</el-button>
            <el-button
              size="small"
              :loading="liveLoading"
              @click="handleQueryByNo"
            >查其他单号</el-button>
          </div>
        </div>

        <!-- ===== 简略版 ===== -->
        <template v-if="trackView === 'simple'">
          <!-- 动态航线地图 -->
          <div class="track-map-box">
            <div class="track-map-head">
              <span class="track-route">
                <el-icon><Location /></el-icon>
                {{ trackRoute.from || '起运港' }} → {{ trackRoute.to || '目的港' }}
              </span>
              <span class="track-badge" :class="`s${trackStatus}`">
                <i v-if="trackStatus === 2" class="live-dot"></i>
                {{ SHIPMENT_STATUS[trackStatus] || '未知' }}
              </span>
            </div>
            <div ref="trackMapRef" class="track-map"></div>
            <div class="track-progress">
              <el-progress
                :percentage="Math.round(trackProgress * 100)"
                :stroke-width="10"
                :color="trackProgressColor"
              />
              <div class="track-meta">
                <span v-if="trackRoute.fromCoord == null" class="track-warn">起运港未识别坐标，暂时无法绘制航线</span>
                <span v-if="trackEta" class="track-eta">预计到达 {{ trackEta }}</span>
                <span v-if="trackStatus === 2" class="track-live">
                  <i class="live-dot"></i> 实时追踪中 · 每 15 秒自动刷新
                </span>
              </div>
            </div>
          </div>

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
        </template>

        <!-- ===== 详细版（淘宝式物流详情） ===== -->
        <template v-else>
          <div class="tb-summary">
            <div class="tb-summary-main">
              <div class="tb-status" :class="`s${trackStatus}`">
                <el-icon><Van /></el-icon>
                {{ SHIPMENT_STATUS[trackStatus] || '未知' }}
              </div>
              <div class="tb-route">{{ trackRoute.from || '起运港' }} → {{ trackRoute.to || '目的港' }}</div>
              <div class="tb-sub">
                承运商 {{ trackCarrier || '-' }} · 运单号 {{ trackTrackingNo || '-' }}
              </div>
            </div>
            <div class="tb-eta-box">
              <div class="tb-eta-label">预计送达</div>
              <div class="tb-eta-value">{{ trackEta || '待定' }}</div>
            </div>
          </div>

          <el-empty v-if="!trackLoading && tracks.length === 0" description="暂无轨迹节点" />
          <div v-else class="tb-list">
            <div
              v-for="(t, i) in tracksDesc"
              :key="t.id"
              class="tb-item"
              :class="{ latest: i === 0 }"
            >
              <div class="tb-rail">
                <span class="tb-dot" :class="{ active: i === 0 }"></span>
                <span v-if="i < tracksDesc.length - 1" class="tb-line"></span>
              </div>
              <div class="tb-content">
                <div class="tb-node">
                  {{ t.node }}
                  <el-tag v-if="i === 0" size="small" type="primary" effect="plain">最新</el-tag>
                </div>
                <div class="tb-desc" v-if="t.content">{{ t.content }}</div>
                <div class="tb-meta">
                  <span v-if="t.location" class="tb-loc">
                    <el-icon><Location /></el-icon>{{ t.location }}
                  </span>
                  <span>{{ formatTime(t.trackTime) }}</span>
                  <span v-if="t.operator">操作人：{{ t.operator }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { Search, Refresh, Plus, Edit, Delete, Location, Van } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import worldGeo from '@/assets/geo/world.json'
import { getCoord } from '@/utils/geoCoord'
import {
  getShipmentPageApi, addShipmentApi, updateShipmentApi,
  updateShipmentStatusApi, deleteShipmentApi, getOrderPageApi, getShipmentTracksApi,
  getShipmentLiveTracksApi, queryLiveTrackByNoApi
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
// 轨迹数据来源：local 本地轨迹 / trackingmore 第三方实时
const trackSource = ref('local')
const liveLoading = ref(false)

// 轨迹视图：simple 航线图 / detail 淘宝式物流详情
const trackView = ref('simple')
// 最新节点在上（淘宝式倒序）
const tracksDesc = computed(() => [...tracks.value].reverse())
const trackCarrier = computed(() => trackShipment.value?.carrier || '')
const trackTrackingNo = computed(() => trackShipment.value?.trackingNo || '')

// ===== 动态航线地图 =====
const SHIPMENT_STATUS = { 1: '待发货', 2: '运输中', 3: '已到达', 4: '已签收' }
// 各状态在航线上的基准进度
const BASE_PROGRESS = { 1: 0.03, 2: 0.18, 3: 0.95, 4: 1 }

const trackMapRef = ref(null)
const trackShipment = ref(null)
const trackProgress = ref(0)
let trackMap = null
let trackMapRegistered = false
let trackAnimTimer = null
let trackPollTimer = null
let trackCurveCache = null

const trackStatus = computed(() => Number(trackShipment.value?.status || 1))
const trackEta = computed(() => {
  const eta = trackShipment.value?.eta
  return eta ? String(eta).slice(0, 10) : ''
})
const trackRoute = computed(() => {
  const s = trackShipment.value || {}
  return {
    from: s.portFrom,
    to: s.portTo,
    fromCoord: getCoord(s.portFrom),
    toCoord: getCoord(s.portTo)
  }
})
const trackProgressColor = computed(() => {
  if (trackStatus.value === 4) return '#22c55e'
  if (trackStatus.value === 3) return '#3b82f6'
  return '#22d3ee'
})

// 二次贝塞尔采样：控制点沿法向偏移，形成跨洋弧线
const buildCurve = (p0, p2, samples = 60) => {
  const mid = [(p0[0] + p2[0]) / 2, (p0[1] + p2[1]) / 2]
  const dx = p2[0] - p0[0]
  const dy = p2[1] - p0[1]
  const ctrl = [mid[0] - dy * 0.18, mid[1] + dx * 0.18]
  const pts = []
  for (let i = 0; i <= samples; i++) {
    const t = i / samples
    const mt = 1 - t
    pts.push([
      mt * mt * p0[0] + 2 * mt * t * ctrl[0] + t * t * p2[0],
      mt * mt * p0[1] + 2 * mt * t * ctrl[1] + t * t * p2[1]
    ])
  }
  return pts
}

const initTrackMap = () => {
  if (!trackMapRef.value) return
  if (!trackMapRegistered) {
    echarts.registerMap('world', worldGeo)
    trackMapRegistered = true
  }
  trackMap?.dispose()
  trackMap = echarts.init(trackMapRef.value)
  trackMap.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(56, 189, 248, 0.4)',
      textStyle: { color: '#e2e8f0', fontSize: 12 }
    },
    geo: {
      map: 'world',
      roam: true,
      zoom: 1.05,
      itemStyle: { areaColor: '#e8eef7', borderColor: '#c3cfe0', borderWidth: 0.6 },
      emphasis: { itemStyle: { areaColor: '#d7e3f5' }, label: { show: false } },
      silent: true
    },
    series: [
      {
        id: 'rest', type: 'lines', coordinateSystem: 'geo', polyline: true, data: [], silent: true,
        lineStyle: { color: '#94a3b8', width: 2, type: 'dashed' }, zlevel: 1
      },
      {
        id: 'passed', type: 'lines', coordinateSystem: 'geo', polyline: true, data: [], silent: true,
        lineStyle: { color: '#22d3ee', width: 3, shadowBlur: 10, shadowColor: 'rgba(34, 211, 238, 0.65)' }, zlevel: 2
      },
      {
        id: 'flow', type: 'lines', coordinateSystem: 'geo', polyline: true, data: [],
        lineStyle: { width: 0 },
        effect: { show: true, period: 3, trailLength: 0.25, symbol: 'circle', symbolSize: 4, color: '#38bdf8' },
        silent: true, zlevel: 3
      },
      {
        id: 'current', type: 'effectScatter', coordinateSystem: 'geo', data: [],
        rippleEffect: { brushType: 'stroke', scale: 3.5 },
        symbolSize: 12,
        itemStyle: { color: '#22d3ee', shadowBlur: 12, shadowColor: '#22d3ee' },
        label: { show: true, position: 'top', formatter: '当前位置', color: '#0284c7', fontSize: 10 },
        zlevel: 4
      },
      {
        id: 'ends', type: 'scatter', coordinateSystem: 'geo', data: [],
        symbolSize: 9,
        itemStyle: { color: '#64748b' },
        label: { show: true, position: 'bottom', formatter: (p) => p.data.name, color: '#475569', fontSize: 10 },
        zlevel: 4
      }
    ]
  })
  renderTrackMap()
}

// 按当前进度重绘：已走实线 + 未走虚线 + 流动光点 + 位置涟漪
const renderTrackMap = () => {
  if (!trackMap) return
  const { fromCoord, toCoord } = trackRoute.value
  if (!fromCoord || !toCoord) {
    trackMap.clear()
    return
  }
  if (!trackCurveCache) trackCurveCache = buildCurve(fromCoord, toCoord)
  const pts = trackCurveCache
  const total = pts.length - 1
  const idx = Math.max(0, Math.min(total, Math.round(trackProgress.value * total)))
  const passed = pts.slice(0, Math.max(2, idx + 1))
  const rest = pts.slice(idx)
  const cur = pts[idx]

  trackMap.setOption({
    series: [
      { id: 'rest', data: [{ coords: rest }] },
      { id: 'passed', data: [{ coords: passed }] },
      { id: 'flow', data: [{ coords: pts }] },
      { id: 'current', data: [{ value: [...cur, 1], name: '当前位置' }] },
      {
        id: 'ends',
        data: [
          { value: [...fromCoord, 1], name: trackRoute.value.from },
          { value: [...toCoord, 1], name: trackRoute.value.to }
        ]
      }
    ]
  })
}

// 运输中：让当前位置沿航线持续前进；其余状态停在基准点
const startTrackAnim = () => {
  if (trackAnimTimer) {
    clearInterval(trackAnimTimer)
    trackAnimTimer = null
  }
  const status = trackStatus.value
  trackProgress.value = BASE_PROGRESS[status] ?? 0.05
  renderTrackMap()
  if (status === 2) {
    trackAnimTimer = setInterval(() => {
      if (trackProgress.value >= 0.96) return
      trackProgress.value = Math.min(0.96, trackProgress.value + 0.0022)
      renderTrackMap()
    }, 80)
  }
}

// 仅释放地图与动画（保留轨迹轮询，供详细版继续接收新节点）
const disposeTrackMap = () => {
  if (trackAnimTimer) {
    clearInterval(trackAnimTimer)
    trackAnimTimer = null
  }
  trackMap?.dispose()
  trackMap = null
  trackCurveCache = null
}

const stopTrackAnim = () => {
  if (trackPollTimer) {
    clearInterval(trackPollTimer)
    trackPollTimer = null
  }
  disposeTrackMap()
}

// 切换视图：回到简略版时重建地图并恢复动画，详细版时释放地图
watch(trackView, async (v) => {
  if (!trackVisible.value) return
  if (v === 'simple') {
    await nextTick()
    initTrackMap()
    startTrackAnim()
  } else {
    disposeTrackMap()
  }
})

const handleTrackResize = () => trackMap?.resize()

const loadData = async (silent = false) => {
  if (!silent) loading.value = true
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
      await loadData(silent)
    }
  } finally {
    if (!silent) loading.value = false
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
  trackShipment.value = row.shipment
  trackView.value = 'simple'
  trackSource.value = 'local'
  trackCurveCache = null
  trackVisible.value = true
  trackLoading.value = true
  try {
    const res = await getShipmentTracksApi(row.shipment.id)
    tracks.value = res.data || []
  } finally {
    trackLoading.value = false
  }
  await nextTick()
  initTrackMap()
  startTrackAnim()

  // 运输中/待发货/已到达：每 15 秒自动拉取最新轨迹节点，节点更新后动画进度同步前进
  if (Number(row.shipment.status) !== 4) {
    trackPollTimer = setInterval(async () => {
      // 已切换到 17TRACK 实时轨迹时，不再用本地轨迹覆盖
      if (trackSource.value === '17track') return
      try {
        const res = await getShipmentTracksApi(row.shipment.id)
        tracks.value = res.data || []
      } catch (e) {
        // 轮询失败静默处理，不打断当前动画
      }
    }, 15000)
  }
}

// 进入 17TRACK 实时模式：停止本地轨迹轮询，避免 15 秒后把实时节点覆盖回本地数据
const stopLocalTrackPoll = () => {
  if (trackPollTimer) {
    clearInterval(trackPollTimer)
    trackPollTimer = null
  }
}

// 同步第三方实时轨迹（17TRACK）：未配置 Key / 号码无效时后端返回明确错误
const handleSyncLive = async () => {
  if (!trackShipment.value?.id) {
    return
  }
  if (!trackShipment.value?.trackingNo) {
    ElMessage.warning('该发货单未填写运单号，无法查询实时轨迹')
    return
  }
  liveLoading.value = true
  try {
    const res = await getShipmentLiveTracksApi(trackShipment.value.id)
    const nodes = (res.data?.nodes || []).map((n, i) => ({ ...n, id: `live-${i}` }))
    tracks.value = nodes
    trackSource.value = '17track'
    stopLocalTrackPoll()
    ElMessage.success(`已同步 ${nodes.length} 条 17TRACK 实时轨迹`)
  } catch (e) {
    // 请求拦截器已弹出后端返回的错误信息
  } finally {
    liveLoading.value = false
  }
}

// 按任意运单号查 17TRACK：便于用真实快递单号演示（不依赖当前发货单）
const handleQueryByNo = async () => {
  let trackingNo
  try {
    const { value } = await ElMessageBox.prompt(
      '输入要查询的快递单号（17TRACK 支持 DHL/UPS/FedEx/邮政等 3500+ 承运商）',
      '17TRACK 实时查询',
      {
        inputPlaceholder: '例如 1Z999AA10123456784',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入运单号',
        confirmButtonText: '查询',
        cancelButtonText: '取消'
      }
    )
    trackingNo = String(value).trim()
  } catch (e) {
    return
  }
  liveLoading.value = true
  try {
    const res = await queryLiveTrackByNoApi(trackingNo)
    const nodes = (res.data?.nodes || []).map((n, i) => ({ ...n, id: `live-${i}` }))
    tracks.value = nodes
    trackSource.value = '17track'
    stopLocalTrackPoll()
    ElMessage.success(`已获取 ${nodes.length} 条 17TRACK 实时轨迹`)
  } catch (e) {
    // 请求拦截器已弹出后端返回的错误信息
  } finally {
    liveLoading.value = false
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

// 新订单/新发货单：列表每 30 秒静默刷新，新增单据无需手动刷新即可出现
let listRefreshTimer = null

onMounted(() => {
  loadData()
  searchOrders('')
  window.addEventListener('resize', handleTrackResize)
  listRefreshTimer = setInterval(() => loadData(true), 30000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleTrackResize)
  if (listRefreshTimer) clearInterval(listRefreshTimer)
  stopTrackAnim()
})
</script>

<style scoped>
.track-node { font-weight: 600; color: var(--el-text-color-primary); margin-bottom: 4px; }
.track-line { font-size: 13px; color: var(--el-text-color-regular); line-height: 20px; }
.track-op { color: var(--el-text-color-secondary); }

/* 动态航线地图 */
.track-map-box {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: linear-gradient(180deg, #f8fbff, #eef4fc);
}
.track-map-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.track-route {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.track-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  background: #f1f5f9;
  color: #64748b;
}
.track-badge.s2 { background: #dbeafe; color: #1d4ed8; }
.track-badge.s3 { background: #dcfce7; color: #15803d; }
.track-badge.s4 { background: #dcfce7; color: #15803d; }
.track-map {
  width: 100%;
  height: 300px;
}
.track-progress { margin-top: 10px; }
.track-meta {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.track-live {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #0ea5e9;
}
.track-warn { color: #f59e0b; }
.live-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #0ea5e9;
  animation: livePulse 1.2s infinite;
}
@keyframes livePulse {
  0% { box-shadow: 0 0 0 0 rgba(14, 165, 233, 0.6); }
  70% { box-shadow: 0 0 0 6px rgba(14, 165, 233, 0); }
  100% { box-shadow: 0 0 0 0 rgba(14, 165, 233, 0); }
}

/* 视图切换栏 */
.track-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.track-toolbar-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.track-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 淘宝式物流详情 */
.tb-summary {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  margin-bottom: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: linear-gradient(135deg, #eef6ff, #f7fbff);
}
.tb-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 700;
  color: #0ea5e9;
}
.tb-status.s1 { color: #64748b; }
.tb-status.s3, .tb-status.s4 { color: #16a34a; }
.tb-route {
  margin-top: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.tb-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.tb-eta-box {
  text-align: right;
  white-space: nowrap;
}
.tb-eta-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.tb-eta-value {
  margin-top: 4px;
  font-size: 15px;
  font-weight: 700;
  color: #0ea5e9;
}
.tb-list {
  padding: 4px 4px 0;
}
.tb-item {
  display: flex;
  gap: 12px;
}
.tb-rail {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 14px;
  padding-top: 4px;
}
.tb-dot {
  flex: 0 0 auto;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #cbd5e1;
}
.tb-dot.active {
  width: 12px;
  height: 12px;
  background: #0ea5e9;
  box-shadow: 0 0 0 4px rgba(14, 165, 233, 0.15);
  animation: livePulse 1.6s infinite;
}
.tb-line {
  flex: 1;
  width: 2px;
  margin-top: 3px;
  background: #e2e8f0;
}
.tb-content {
  flex: 1;
  padding-bottom: 18px;
}
.tb-node {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.tb-item.latest .tb-node {
  font-size: 15px;
  color: #0ea5e9;
}
.tb-desc {
  margin-top: 4px;
  font-size: 13px;
  line-height: 20px;
  color: var(--el-text-color-regular);
}
.tb-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.tb-loc {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
</style>
