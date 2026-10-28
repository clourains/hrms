<template>
  <div class="page-container page-stack">
    <!-- 页头 -->
    <PageHeader
      title="仓库与库存"
      description="管理仓库档案、库存总览与出入库流水，支持调拨与盘点"
      icon="House"
    />

    <div class="table-card">
      <el-tabs v-model="activeTab">
        <!-- ========== 仓库管理 ========== -->
        <el-tab-pane label="仓库管理" name="warehouse">
          <div class="search-card">
            <el-form :inline="true" :model="whSearch">
              <el-form-item label="关键字">
                <el-input v-model="whSearch.keyword" placeholder="编码 / 名称 / 国家" clearable style="width: 200px" @keyup.enter="handleWhSearch" />
              </el-form-item>
              <el-form-item label="类型">
                <el-select v-model="whSearch.type" placeholder="全部" clearable style="width: 130px">
                  <el-option label="国内仓" :value="1" />
                  <el-option label="海外仓" :value="2" />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="whSearch.status" placeholder="全部" clearable style="width: 110px">
                  <el-option label="启用" :value="1" />
                  <el-option label="停用" :value="0" />
                </el-select>
              </el-form-item>
              <el-form-item class="search-submit">
                <el-button type="primary" :icon="Search" @click="handleWhSearch">搜索</el-button>
                <el-button :icon="Refresh" @click="resetWhSearch">重置</el-button>
                <el-button type="primary" :icon="Plus" @click="handleAddWh">新增仓库</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 容量与地点可视化 -->
          <div class="wh-visual">
            <div class="wh-visual-head">
              <span class="wh-visual-title">仓储容量与全球分布</span>
              <span class="wh-visual-sub">
                共 {{ whOverview.length }} 个仓库 · 总容量 {{ totalCapacity.toLocaleString() }} 件 ·
                已用 {{ totalStock.toLocaleString() }} 件（整体使用率 {{ overallUsage }}%）
                <em v-if="overCapacityCount" class="wh-cap-warn">· {{ overCapacityCount }} 个仓库库存已超出登记容量，建议核对容量配置</em>
              </span>
            </div>
            <el-row :gutter="16">
              <el-col :xs="24" :lg="12">
                <div class="wh-cap-list">
                  <div v-for="w in whOverview" :key="w.id" class="wh-cap-item">
                    <div class="wh-cap-top">
                      <span class="wh-cap-name">
                        <el-tag :type="w.type === 2 ? 'warning' : 'primary'" size="small" effect="plain">
                          {{ w.type === 2 ? '海外仓' : '国内仓' }}
                        </el-tag>
                        <span class="wh-cap-label">{{ w.warehouseName }}</span>
                        <span class="wh-cap-loc">{{ w.country }} · {{ w.city }}</span>
                      </span>
                      <span class="wh-cap-num" :class="{ danger: Number(w.usageRate) >= 100 }">
                        {{ Number(w.stockQty || 0).toLocaleString() }} / {{ w.capacity ? Number(w.capacity).toLocaleString() : '—' }}
                        <b>{{ w.usageRate }}%</b>
                        <el-tag v-if="Number(w.usageRate) > 100" type="danger" size="small" effect="plain">超容</el-tag>
                      </span>
                    </div>
                    <el-progress
                      :percentage="Math.min(100, Number(w.usageRate) || 0)"
                      :stroke-width="9"
                      :color="usageColor(w.usageRate)"
                      :show-text="false"
                    />
                  </div>
                  <el-empty v-if="!whOverview.length" description="暂无仓库数据" :image-size="60" />
                </div>
              </el-col>
              <el-col :xs="24" :lg="12">
                <div ref="whMapRef" class="wh-map"></div>
              </el-col>
            </el-row>
          </div>

          <el-table v-loading="whLoading" :data="whList" border stripe @sort-change="handleWhSort">
            <el-table-column prop="warehouseCode" label="仓库编码" width="120" sortable="custom" />
            <el-table-column prop="warehouseName" label="仓库名称" min-width="150" sortable="custom" />
            <el-table-column prop="type" label="类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.type === 2 ? 'warning' : 'primary'">{{ row.type === 2 ? '海外仓' : '国内仓' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="country" label="国家" width="100" sortable="custom" />
            <el-table-column prop="city" label="城市" width="110" />
            <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
            <el-table-column prop="manager" label="负责人" width="100" />
            <el-table-column prop="capacity" label="容量(件)" width="110" align="right" sortable="custom">
              <template #default="{ row }">{{ row.capacity ? row.capacity.toLocaleString() : '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" align="center" sortable="custom">
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

          <div class="pagination page-pagination">
            <el-pagination
              v-model:current-page="whPagination.pageNum"
              v-model:page-size="whPagination.pageSize"
              :total="whPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleWhSizeChange"
              @current-change="loadWarehouses"
            />
          </div>
        </el-tab-pane>

        <!-- ========== 库存总览 ========== -->
        <el-tab-pane name="inventory">
          <template #label>
            <span class="tab-with-badge">
              库存总览
              <!-- 只在有库存不足时出现，正常不显示 -->
              <template v-if="lowStockCount > 0">
                <span class="low-badge">{{ lowStockCount }}</span>
                <span class="low-hint" @click.stop="gotoLowStock">库存不足，点击查看</span>
              </template>
            </span>
          </template>
          <div class="search-card">
            <el-form :inline="true" :model="invSearch">
              <el-form-item label="产品">
                <el-select v-model="invSearch.productId" placeholder="全部产品" clearable filterable style="width: 200px">
                  <el-option v-for="p in productList" :key="p.id" :label="productLabel(p)" :value="p.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="仓库">
                <el-select v-model="invSearch.warehouseId" placeholder="全部仓库" clearable filterable style="width: 180px">
                  <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-checkbox v-model="invSearch.lowStock" @change="handleInvSearch">只看低库存</el-checkbox>
              </el-form-item>
              <el-form-item class="search-submit">
                <el-button :icon="Sort" @click="handleOpenTransfer">仓间调拨</el-button>
                <el-button :icon="Tickets" @click="openScDrawer">盘点单</el-button>
                <el-button type="warning" :icon="Box" @click="handleOpenStockChange">出入库登记</el-button>
                <el-button type="primary" :icon="Search" @click="handleInvSearch">搜索</el-button>
                <el-button :icon="Refresh" @click="resetInvSearch">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table v-loading="invLoading" :data="invList" border stripe @sort-change="handleInvSort">
            <el-table-column prop="productName" label="产品名称" min-width="150" />
            <el-table-column prop="sku" label="SKU" width="120" sortable="custom" />
            <el-table-column prop="warehouseName" label="仓库" min-width="130" />
            <el-table-column prop="warehouseType" label="仓库类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.warehouseType === 2 ? 'warning' : 'primary'" size="small">{{ row.warehouseType === 2 ? '海外仓' : '国内仓' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="库存数量" width="110" align="right" sortable="custom" />
            <el-table-column prop="lockedQuantity" label="锁定数量" width="110" align="right" sortable="custom" />
            <el-table-column prop="availableQuantity" label="可用数量" width="110" align="right" sortable="custom">
              <template #default="{ row }">
                <span :style="{ color: isLowStock(row) ? 'var(--el-color-danger)' : 'var(--el-text-color-primary)', fontWeight: isLowStock(row) ? 600 : 400 }">
                  {{ row.availableQuantity }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="warningQuantity" label="预警值" width="120" align="right" sortable="custom">
              <template #default="{ row }">
                <span>{{ row.warningQuantity }}</span>
                <el-button type="primary" link :icon="EditPen" class="value-edit" @click="handleEditWarning(row)" />
              </template>
            </el-table-column>
            <el-table-column label="库存状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="isLowStock(row)" type="danger" size="small">库存不足</el-tag>
                <el-tag v-else type="success" size="small">正常</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleRowStockChange(row, 1)">入库</el-button>
                <el-button type="warning" link @click="handleRowStockChange(row, 2)">出库</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination page-pagination">
            <el-pagination
              v-model:current-page="invPagination.pageNum"
              v-model:page-size="invPagination.pageSize"
              :total="invPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleInvSizeChange"
              @current-change="loadInventory"
            />
          </div>
        </el-tab-pane>

        <!-- ========== 变动记录 ========== -->
        <el-tab-pane label="变动记录" name="logs">
          <div class="search-card">
            <el-form :inline="true" :model="logSearch">
              <el-form-item label="变动类型">
                <el-select v-model="logSearch.changeType" placeholder="全部" clearable style="width: 130px">
                  <el-option label="入库" :value="1" />
                  <el-option label="出库" :value="2" />
                  <el-option label="锁定" :value="3" />
                  <el-option label="解锁" :value="4" />
                </el-select>
              </el-form-item>
              <el-form-item class="search-submit">
                <el-button type="primary" :icon="Search" @click="handleLogSearch">搜索</el-button>
                <el-button :icon="Refresh" @click="resetLogSearch">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table v-loading="logLoading" :data="logList" border stripe @sort-change="handleLogSort">
            <el-table-column prop="createTime" label="时间" width="180" sortable="custom" />
            <el-table-column prop="productName" label="产品" min-width="140" />
            <el-table-column prop="sku" label="SKU" width="110" />
            <el-table-column prop="warehouseName" label="仓库" min-width="130" />
            <el-table-column prop="changeType" label="类型" width="90" align="center" sortable="custom">
              <template #default="{ row }">
                <el-tag :type="changeTypeTag(row.changeType)" size="small">{{ changeTypeText(row.changeType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="changeQuantity" label="变动数量" width="110" align="right" sortable="custom">
              <template #default="{ row }">
                <span :style="{ color: changeColor(row.changeType), fontWeight: 600 }">
                  {{ changePrefix(row.changeType) }}{{ row.changeQuantity }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="afterQuantity" label="变动后库存" width="110" align="right" />
            <el-table-column prop="afterLockedQuantity" label="变动后锁定" width="110" align="right" />
            <el-table-column prop="orderNo" label="关联订单" width="160" />
            <el-table-column prop="operatorName" label="操作人" width="100" />
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
          </el-table>

          <div class="pagination page-pagination">
            <el-pagination
              v-model:current-page="logPagination.pageNum"
              v-model:page-size="logPagination.pageSize"
              :total="logPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleLogSizeChange"
              @current-change="loadLogs"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 仓库编辑对话框 -->
    <el-dialog v-model="whDialogVisible" :title="isEditWh ? '编辑仓库' : '新增仓库'" width="600px" destroy-on-close>
      <el-form ref="whFormRef" :model="whForm" :rules="whRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="仓库编码" prop="warehouseCode">
              <el-input v-model="whForm.warehouseCode" placeholder="如 WH-CN-01" :disabled="isEditWh" maxlength="50" show-word-limit />
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
          <el-input v-model="whForm.warehouseName" maxlength="100" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="国家" prop="country">
              <el-input v-model="whForm.country" placeholder="如 中国 / 美国" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市" prop="city">
              <el-input v-model="whForm.city" placeholder="如 洛杉矶" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="whForm.address" maxlength="200" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="负责人" prop="manager">
              <el-input v-model="whForm.manager" maxlength="50" />
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
            <el-option v-for="p in productList" :key="p.id" :label="productLabel(p)" :value="p.id" />
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
        <el-form-item label="关联订单号">
          <el-input v-model="stockForm.orderNo" placeholder="选填，如 SO20261024-0001" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stockForm.remark" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="如 采购入库 / 订单出库" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitStock">确定</el-button>
      </template>
    </el-dialog>

    <!-- 仓间调拨 -->
    <el-dialog v-model="transferDialogVisible" title="仓间调拨" width="560px" destroy-on-close>
      <el-form ref="transferFormRef" :model="transferForm" :rules="transferRules" label-width="110px">
        <el-form-item label="产品" prop="productId">
          <el-select v-model="transferForm.productId" placeholder="选择产品" filterable style="width: 100%">
            <el-option v-for="p in productList" :key="p.id" :label="productLabel(p)" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="调出仓库" prop="fromWarehouseId">
          <el-select v-model="transferForm.fromWarehouseId" placeholder="选择调出仓库" filterable style="width: 100%">
            <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="调入仓库" prop="toWarehouseId">
          <el-select v-model="transferForm.toWarehouseId" placeholder="选择调入仓库" filterable style="width: 100%">
            <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="调拨数量" prop="quantity">
          <el-input-number v-model="transferForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="transferForm.remark" type="textarea" :rows="2" maxlength="150" show-word-limit placeholder="如 国内仓补货" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitTransfer">确定调拨</el-button>
      </template>
    </el-dialog>

    <!-- ========== 盘点单列表（P019） ========== -->
    <el-drawer v-model="scDrawerVisible" title="库存盘点单" size="1020px" destroy-on-close>
      <div class="table-card">
        <div class="search-card">
          <el-form :inline="true" :model="scSearch">
            <el-form-item label="单号">
              <el-input v-model="scSearch.keyword" placeholder="盘点单号" clearable style="width: 180px" @keyup.enter="handleScSearch" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="scSearch.status" placeholder="全部" clearable style="width: 120px">
                <el-option label="草稿" :value="1" />
                <el-option label="已确认" :value="2" />
                <el-option label="已作废" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item class="search-submit">
              <el-button type="primary" :icon="Search" @click="handleScSearch">搜索</el-button>
              <el-button :icon="Refresh" @click="resetScSearch">重置</el-button>
              <el-button type="primary" :icon="Plus" @click="handleOpenCreateSc">新建盘点单</el-button>
            </el-form-item>
          </el-form>
        </div>

      <el-table v-loading="scLoading" :data="scList" border stripe>
        <el-table-column prop="checkNo" label="盘点单号" width="170" />
        <el-table-column prop="warehouseName" label="仓库" min-width="130" show-overflow-tooltip />
        <el-table-column prop="checkDate" label="盘点日期" width="110" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="scStatusTag(row.status)" size="small">{{ scStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="itemCount" label="明细" width="70" align="right" />
        <el-table-column prop="diffRows" label="差异行" width="80" align="right" />
        <el-table-column label="差异合计" width="100" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.diffSum === 0 ? 'var(--el-text-color-secondary)' : (row.diffSum > 0 ? 'var(--el-color-success)' : 'var(--el-color-danger)'), fontWeight: 600 }">
              {{ row.diffSum > 0 ? '+' : '' }}{{ row.diffSum }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="制单人" width="90" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleOpenScDetail(row)">{{ row.status === 1 ? '编辑' : '查看' }}</el-button>
            <el-button v-if="row.status === 1" type="success" link @click="handleConfirmSc(row)">确认</el-button>
            <el-button v-if="row.status === 1" type="danger" link @click="handleCancelSc(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>

        <div class="pagination page-pagination">
          <el-pagination
            v-model:current-page="scPagination.pageNum"
            v-model:page-size="scPagination.pageSize"
            :total="scPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="handleScSizeChange"
            @current-change="loadScList"
          />
        </div>
      </div>
    </el-drawer>

    <!-- ========== 盘点单编辑/详情（P019） ========== -->
    <el-dialog v-model="scDialogVisible" :title="scDialogTitle" width="940px" destroy-on-close>
      <el-form :inline="true" :model="scForm" label-width="80px">
        <el-form-item label="盘点仓库">
          <el-select v-model="scForm.warehouseId" placeholder="选择仓库" filterable style="width: 200px"
                     :disabled="scMode === 'view'" @change="handleScWarehouseChange">
            <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点日期">
          <el-date-picker v-model="scForm.checkDate" type="date" value-format="YYYY-MM-DD"
                          style="width: 150px" :disabled="scMode === 'view'" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="scForm.remark" style="width: 240px" maxlength="200" show-word-limit
                    :disabled="scMode === 'view'" placeholder="如 月末全仓盘点" />
        </el-form-item>
      </el-form>

      <div class="check-toolbar">
        <span class="check-tip">
          共 {{ scItems.length }} 行，差异 <strong>{{ scDiffRows }}</strong> 行<template v-if="scOnlyDiff">（当前显示 {{ scViewItems.length }} 行）</template>，合计
          <strong :style="{ color: scDiffSum === 0 ? 'var(--el-text-color-secondary)' : (scDiffSum > 0 ? 'var(--el-color-success)' : 'var(--el-color-danger)') }">
            {{ scDiffSum > 0 ? '+' : '' }}{{ scDiffSum }}
          </strong>
        </span>
        <div class="check-toolbar-right">
          <el-switch v-if="scItems.length" v-model="scOnlyDiff" active-text="只看差异行" size="small" />
          <el-button v-if="scMode !== 'view'" :icon="Refresh" :loading="scRowsLoading" @click="handleLoadScRows">
            按当前库存带出
          </el-button>
        </div>
      </div>

      <el-table v-loading="scRowsLoading" :data="scViewItems" border stripe max-height="380"
                :empty-text="scTableEmptyText">
        <el-table-column prop="productName" label="产品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="sku" label="SKU" width="120" />
        <el-table-column prop="bookQuantity" label="账面数量" width="100" align="right" />
        <el-table-column label="实盘数量" width="150" align="center">
          <template #default="{ row }">
            <el-input-number v-if="scMode !== 'view'" v-model="row.actualQuantity" :min="0" size="small"
                             controls-position="right" style="width: 110px" />
            <span v-else>{{ row.actualQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差异" width="90" align="right">
          <template #default="{ row }">
            <span :style="{ color: scItemDiff(row) === 0 ? 'var(--el-text-color-secondary)' : (scItemDiff(row) > 0 ? 'var(--el-color-success)' : 'var(--el-color-danger)'), fontWeight: 600 }">
              {{ scItemDiff(row) > 0 ? '+' : '' }}{{ scItemDiff(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="已锁定" width="90" align="right">
          <template #default="{ row }">{{ row.lockedQuantity ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="行备注" min-width="150">
          <template #default="{ row }">
            <el-input v-if="scMode !== 'view'" v-model="row.remark" size="small" maxlength="200" placeholder="选填" />
            <span v-else>{{ row.remark }}</span>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="scDialogVisible = false">关闭</el-button>
        <template v-if="scMode !== 'view'">
          <el-button :loading="scSubmitting" @click="handleSubmitSc(false)">保存草稿</el-button>
          <el-button type="primary" :loading="scSubmitting" @click="handleSubmitSc(true)">保存并确认</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { Search, Refresh, Plus, Edit, EditPen, Delete, Box, Sort, Tickets } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import worldGeo from '@/assets/geo/world.json'
import { getCoord } from '@/utils/geoCoord'
import {
  getWarehousePageApi, getWarehouseListApi, addWarehouseApi, updateWarehouseApi, deleteWarehouseApi,
  getInventoryPageApi, getInventoryLogsApi, changeStockApi, getProductListApi,
  updateWarningQuantityApi, transferStockApi,
  getStockCheckPageApi, getStockCheckDetailApi, getStockCheckRowsApi,
  createStockCheckApi, updateStockCheckApi, confirmStockCheckApi, cancelStockCheckApi,
  getInventoryWarningCountApi, getWarehouseOverviewApi
} from '@/api/trade'

const activeTab = ref('warehouse')
const submitting = ref(false)
const route = useRoute()

const productLabel = (p) => `${p.name} (${p.sku})${p.status === 0 ? ' · 已下架' : ''}`
const isLowStock = (row) => (row.availableQuantity ?? 0) <= (row.warningQuantity ?? 0)

// ========== 库存不足提醒 ==========
// 只在有不足时渲染红字/数字，正常（0 条）完全不显示
const lowStockCount = ref(0)

const loadLowStockCount = async () => {
  try {
    const res = await getInventoryWarningCountApi()
    lowStockCount.value = Number(res.data || 0)
  } catch (e) {
    lowStockCount.value = 0
  }
}

// 点红字：切到库存总览并自动勾上"只看低库存"
const gotoLowStock = () => {
  activeTab.value = 'inventory'
  invSearch.lowStock = true
  handleInvSearch()
}
const changeTypeText = (t) => ({ 1: '入库', 2: '出库', 3: '锁定', 4: '解锁' }[t] || '未知')
const changeTypeTag = (t) => ({ 1: 'success', 2: 'danger', 3: 'warning', 4: 'info' }[t] || 'info')
const changeColor = (t) => ({ 1: 'var(--el-color-success)', 2: 'var(--el-color-danger)', 3: 'var(--el-color-warning)', 4: 'var(--el-text-color-secondary)' }[t] || 'var(--el-text-color-primary)')
const changePrefix = (t) => (t === 2 || t === 4 ? '-' : '+')

// ========== 仓库容量与地点可视化 ==========
const whOverview = ref([])
const whMapRef = ref(null)
let whMap = null
let whMapRegistered = false
let lastWhKey = ''
let whRefreshTimer = null

const totalCapacity = computed(() => whOverview.value.reduce((s, w) => s + Number(w.capacity || 0), 0))
const totalStock = computed(() => whOverview.value.reduce((s, w) => s + Number(w.stockQty || 0), 0))
const overallUsage = computed(() => {
  if (!totalCapacity.value) return 0
  return Math.round((totalStock.value / totalCapacity.value) * 1000) / 10
})

const overCapacityCount = computed(() => whOverview.value.filter((w) => Number(w.usageRate) > 100).length)

const usageColor = (rate) => {
  const r = Number(rate) || 0
  if (r >= 90) return '#ef4444'
  if (r >= 70) return '#f59e0b'
  return '#22c55e'
}

const initWhMap = () => {
  if (!whMapRef.value) return
  if (!whMapRegistered) {
    echarts.registerMap('world', worldGeo)
    whMapRegistered = true
  }
  whMap?.dispose()
  whMap = echarts.init(whMapRef.value)

  const points = whOverview.value
    .map((w) => {
      const coord = getCoord(w.city, w.country)
      if (!coord) return null
      return {
        name: w.warehouseName,
        value: [...coord, Number(w.stockQty || 0)],
        capacity: Number(w.capacity || 0),
        usageRate: w.usageRate,
        city: w.city,
        country: w.country,
        type: w.type
      }
    })
    .filter(Boolean)

  whMap.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(56, 189, 248, 0.4)',
      textStyle: { color: '#e2e8f0', fontSize: 12 },
      formatter: (p) => {
        if (p.seriesType !== 'scatter') return p.name
        const d = p.data
        return `<b>${d.name}</b><br/>${d.country} · ${d.city}<br/>`
          + `容量：${d.capacity.toLocaleString()} 件<br/>`
          + `库存：${Number(d.value[2]).toLocaleString()} 件<br/>`
          + `使用率：${d.usageRate}%`
      }
    },
    geo: {
      map: 'world',
      roam: true,
      zoom: 1.15,
      itemStyle: { areaColor: '#e8eef7', borderColor: '#c3cfe0', borderWidth: 0.6 },
      emphasis: { itemStyle: { areaColor: '#d7e3f5' }, label: { show: false } }
    },
    series: [{
      name: '仓库',
      type: 'scatter',
      coordinateSystem: 'geo',
      data: points,
      symbolSize: (val, params) => {
        const cap = params?.data?.capacity || 0
        return Math.max(10, Math.min(26, 8 + Math.sqrt(cap) / 6))
      },
      itemStyle: {
        color: (p) => (p.data.type === 2 ? '#f59e0b' : '#2563eb'),
        shadowBlur: 8,
        shadowColor: 'rgba(37, 99, 235, 0.45)'
      },
      label: {
        show: true,
        position: 'right',
        formatter: (p) => p.data.name,
        color: '#475569',
        fontSize: 10
      },
      emphasis: { scale: 1.3 }
    }]
  })
}

const loadWhOverview = async (silent = false) => {
  try {
    const res = await getWarehouseOverviewApi()
    const list = Array.isArray(res.data) ? res.data : []
    const key = list.map((w) => `${w.id}:${w.stockQty}:${w.capacity}`).join(',')
    whOverview.value = list
    // 静默刷新时仅在容量/库存发生变化时重绘，避免打断用户正在查看的地图缩放
    if (!silent || key !== lastWhKey) {
      lastWhKey = key
      await nextTick()
      initWhMap()
    }
  } catch (e) {
    whOverview.value = []
  }
}

const handleWhResize = () => whMap?.resize()

// ========== 仓库管理（后端分页 + 排序） ==========
const whLoading = ref(false)
const whList = ref([])
const whSearch = reactive({ keyword: '', type: null, status: null })
const whPagination = reactive({ pageNum: 1, pageSize: 10, total: 0, sortField: null, sortOrder: null })
const whDialogVisible = ref(false)
const isEditWh = ref(false)
const whFormRef = ref(null)

const defaultWhForm = { id: null, warehouseCode: '', warehouseName: '', type: 1, country: '中国', city: '', address: '', manager: '', capacity: 10000, status: 1 }
const whForm = reactive({ ...defaultWhForm })

// 新增和编辑都必须填满：仓库编码、类型、名称、国家、城市、详细地址、负责人
// 早期编辑时只校验前三项，等于留了个「新增必填 → 编辑清空」的后门，这里统一成一套规则
const whRules = {
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择仓库类型', trigger: 'change' }],
  country: [{ required: true, message: '请输入国家', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  manager: [{ required: true, message: '请输入负责人', trigger: 'blur' }]
}

const loadWarehouses = async () => {
  whLoading.value = true
  try {
    const res = await getWarehousePageApi({
      keyword: whSearch.keyword,
      type: whSearch.type,
      status: whSearch.status,
      sortField: whPagination.sortField,
      sortOrder: whPagination.sortOrder,
      pageNum: whPagination.pageNum,
      pageSize: whPagination.pageSize
    })
    whList.value = res.data.list
    whPagination.total = res.data.total
    // 删到空页时回退一页
    if (whList.value.length === 0 && whPagination.pageNum > 1) {
      whPagination.pageNum -= 1
      await loadWarehouses()
    }
  } finally {
    whLoading.value = false
  }
}

const handleWhSearch = () => { whPagination.pageNum = 1; loadWarehouses() }

const resetWhSearch = () => {
  whSearch.keyword = ''
  whSearch.type = null
  whSearch.status = null
  whPagination.sortField = null
  whPagination.sortOrder = null
  handleWhSearch()
}

// 排序：字段名交给后端白名单，编码/名称按数字大小排（P004）
const handleWhSort = ({ prop, order }) => {
  whPagination.sortField = order ? prop : null
  whPagination.sortOrder = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : null
  handleWhSearch()
}

// 切换每页条数时回到第 1 页（P031）
const handleWhSizeChange = () => { whPagination.pageNum = 1; loadWarehouses() }

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
  try {
    await ElMessageBox.confirm(
      `确定删除仓库「${row.warehouseName}」吗？仓库里还有库存时不允许删除。`,
      '提示',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  await deleteWarehouseApi(row.id)
  ElMessage.success('删除成功')
  loadWarehouses()
  loadWarehouseOptions()
}

// ========== 库存总览 ==========
const invLoading = ref(false)
const invList = ref([])
const invSearch = reactive({ productId: null, warehouseId: null, lowStock: false })
const invPagination = reactive({ pageNum: 1, pageSize: 10, total: 0, sortField: null, sortOrder: null })

const loadInventory = async () => {
  invLoading.value = true
  try {
    const res = await getInventoryPageApi({
      productId: invSearch.productId,
      warehouseId: invSearch.warehouseId,
      lowStock: invSearch.lowStock,
      sortField: invPagination.sortField,
      sortOrder: invPagination.sortOrder,
      pageNum: invPagination.pageNum,
      pageSize: invPagination.pageSize
    })
    invList.value = res.data.list
    invPagination.total = res.data.total
    if (invList.value.length === 0 && invPagination.pageNum > 1) {
      invPagination.pageNum -= 1
      await loadInventory()
    }
  } finally {
    invLoading.value = false
  }
}

const handleInvSearch = () => { invPagination.pageNum = 1; loadInventory() }

const resetInvSearch = () => {
  invSearch.productId = null
  invSearch.warehouseId = null
  invSearch.lowStock = false
  invPagination.sortField = null
  invPagination.sortOrder = null
  handleInvSearch()
}

const handleInvSort = ({ prop, order }) => {
  invPagination.sortField = order ? prop : null
  invPagination.sortOrder = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : null
  handleInvSearch()
}

const handleInvSizeChange = () => { invPagination.pageNum = 1; loadInventory() }

// 预警值可编辑（P035）
const handleEditWarning = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `设置「${row.productName}」在「${row.warehouseName}」的预警值`,
      '修改预警值',
      {
        inputValue: String(row.warningQuantity ?? 0),
        inputPattern: /^\d+$/,
        inputErrorMessage: '请输入 0 或正整数',
        confirmButtonText: '保存',
        cancelButtonText: '取消'
      }
    )
    await updateWarningQuantityApi(row.id, Number(value))
    ElMessage.success('预警值已更新')
    loadInventory()
    loadLowStockCount()
  } catch (e) {
    // 取消或校验失败，忽略
  }
}

// ========== 出入库 ==========
const stockDialogVisible = ref(false)
const stockFormRef = ref(null)
const defaultStockForm = { productId: null, warehouseId: null, changeType: 1, quantity: 1, orderNo: '', remark: '' }
const stockForm = reactive({ ...defaultStockForm })

const stockRules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }]
}

const handleOpenStockChange = () => { Object.assign(stockForm, defaultStockForm); stockDialogVisible.value = true }

const handleRowStockChange = (row, changeType) => {
  Object.assign(stockForm, defaultStockForm, {
    productId: row.productId,
    warehouseId: row.warehouseId,
    changeType
  })
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
    loadLogs()
    loadLowStockCount()
  } finally {
    submitting.value = false
  }
}

// ========== 仓间调拨 ==========
const transferDialogVisible = ref(false)
const transferFormRef = ref(null)
const defaultTransferForm = { productId: null, fromWarehouseId: null, toWarehouseId: null, quantity: 1, remark: '' }
const transferForm = reactive({ ...defaultTransferForm })

const transferRules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  fromWarehouseId: [{ required: true, message: '请选择调出仓库', trigger: 'change' }],
  toWarehouseId: [
    { required: true, message: '请选择调入仓库', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (value && value === transferForm.fromWarehouseId) {
          callback(new Error('调出、调入仓库不能相同'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }]
}

const handleOpenTransfer = () => {
  Object.assign(transferForm, defaultTransferForm)
  transferDialogVisible.value = true
}

const handleSubmitTransfer = async () => {
  await transferFormRef.value.validate()
  submitting.value = true
  try {
    await transferStockApi(transferForm)
    ElMessage.success('调拨成功')
    transferDialogVisible.value = false
    loadInventory()
    loadLogs()
  } finally {
    submitting.value = false
  }
}

// ========== 盘点单（P019） ==========
const scDrawerVisible = ref(false)
const scLoading = ref(false)
const scList = ref([])
const scSearch = reactive({ keyword: '', status: null })
const scPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const scStatusText = (s) => ({ 1: '草稿', 2: '已确认', 3: '已作废' }[s] || '未知')
const scStatusTag = (s) => ({ 1: 'info', 2: 'success', 3: 'danger' }[s] || 'info')

const loadScList = async () => {
  scLoading.value = true
  try {
    const res = await getStockCheckPageApi({
      keyword: scSearch.keyword,
      status: scSearch.status,
      pageNum: scPagination.pageNum,
      pageSize: scPagination.pageSize
    })
    scList.value = res.data.list
    scPagination.total = res.data.total
  } finally {
    scLoading.value = false
  }
}

const openScDrawer = () => {
  scDrawerVisible.value = true
  scPagination.pageNum = 1
  loadScList()
}

const handleScSearch = () => { scPagination.pageNum = 1; loadScList() }
const resetScSearch = () => { scSearch.keyword = ''; scSearch.status = null; handleScSearch() }
const handleScSizeChange = () => { scPagination.pageNum = 1; loadScList() }

const scDialogVisible = ref(false)
const scMode = ref('create')
const scSubmitting = ref(false)
const scRowsLoading = ref(false)
const scForm = reactive({ id: null, warehouseId: null, checkDate: '', remark: '', status: null })
const scItems = ref([])

const scDialogTitle = computed(() => ({ create: '新建盘点单', edit: '编辑盘点单', view: '盘点单详情' }[scMode.value] || '盘点单'))
const scItemDiff = (row) => (row.actualQuantity ?? 0) - (row.bookQuantity ?? 0)
const scDiffRows = computed(() => scItems.value.filter((r) => scItemDiff(r) !== 0).length)
const scDiffSum = computed(() => scItems.value.reduce((sum, r) => sum + scItemDiff(r), 0))
// 查看已确认/已作废的单据时默认只看有差异的行，避免整个仓库的零差异行把页面刷满；
// 只影响展示，scItems 始终是完整明细，保存和提交照旧用全量。
const scOnlyDiff = ref(false)
const scViewItems = computed(() => {
  if (!scOnlyDiff.value) return scItems.value
  return scItems.value.filter((r) => scItemDiff(r) !== 0)
})
const scTableEmptyText = computed(() => {
  if (!scItems.value.length) {
    return scMode.value === 'view' ? '该盘点单没有明细' : '先选仓库，再点右上角「按当前库存带出」'
  }
  return scOnlyDiff.value ? '账实一致，没有差异行' : '暂无数据'
})

const localToday = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const handleOpenCreateSc = () => {
  scMode.value = 'create'
  Object.assign(scForm, { id: null, warehouseId: null, checkDate: localToday(), remark: '', status: null })
  scItems.value = []
  scOnlyDiff.value = false
  scDialogVisible.value = true
}

// 换仓库后旧明细的账面数就没意义了，直接清空让用户重新带出
const handleScWarehouseChange = () => { scItems.value = [] }

const handleLoadScRows = async () => {
  if (!scForm.warehouseId) {
    ElMessage.warning('请先选择仓库')
    return
  }
  scRowsLoading.value = true
  try {
    const res = await getStockCheckRowsApi(scForm.warehouseId)
    const rows = res.data.rows || []
    scItems.value = rows.map((r) => ({ ...r, actualQuantity: r.bookQuantity, remark: '' }))
    if (!rows.length) ElMessage.info('该仓库还没有库存记录')
  } finally {
    scRowsLoading.value = false
  }
}

const handleOpenScDetail = async (row) => {
  const res = await getStockCheckDetailApi(row.id)
  const d = res.data || {}
  const head = d.check || {}
  scMode.value = head.status === 1 ? 'edit' : 'view'
  Object.assign(scForm, {
    id: head.id,
    warehouseId: head.warehouseId,
    checkDate: head.checkDate,
    remark: head.remark || '',
    status: head.status
  })
  scItems.value = (d.items || []).map((i) => ({ ...i, remark: i.remark || '' }))
  // 已确认/已作废（只读）默认折叠掉零差异行，草稿进入编辑时保持全量方便改
  scOnlyDiff.value = scMode.value === 'view'
  scDialogVisible.value = true
}

const handleSubmitSc = async (confirmNow) => {
  if (!scForm.warehouseId) {
    ElMessage.warning('请先选择仓库')
    return
  }
  if (!scItems.value.length) {
    ElMessage.warning('请先点「按当前库存带出」生成明细')
    return
  }
  scSubmitting.value = true
  try {
    const payload = {
      id: scMode.value === 'create' ? null : scForm.id,
      warehouseId: scForm.warehouseId,
      checkDate: scForm.checkDate,
      remark: scForm.remark,
      items: scItems.value.map((i) => ({
        productId: i.productId,
        actualQuantity: i.actualQuantity ?? 0,
        remark: i.remark
      }))
    }
    let id = payload.id
    if (scMode.value === 'create') {
      const res = await createStockCheckApi(payload)
      id = res.data
    } else {
      await updateStockCheckApi(payload)
    }

    if (confirmNow) {
      const res = await confirmStockCheckApi(id)
      const d = res.data || {}
      ElMessage.success(`盘点已确认：调整 ${d.adjusted ?? 0} 行，差异合计 ${d.diffSum > 0 ? '+' : ''}${d.diffSum ?? 0}`)
    } else {
      ElMessage.success('已保存为草稿，库存未改动')
    }

    scDialogVisible.value = false
    loadScList()
    loadInventory()
    loadLogs()
  } finally {
    scSubmitting.value = false
  }
}

const handleConfirmSc = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认盘点单「${row.checkNo}」？确认后会按差异调整库存并写入流水，且不能撤销。`,
      '确认盘点',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  const res = await confirmStockCheckApi(row.id)
  const d = res.data || {}
  ElMessage.success(`已确认：调整 ${d.adjusted ?? 0} 行，差异合计 ${d.diffSum > 0 ? '+' : ''}${d.diffSum ?? 0}`)
  loadScList()
  loadInventory()
  loadLogs()
}

const handleCancelSc = async (row) => {
  try {
    await ElMessageBox.confirm(`作废盘点单「${row.checkNo}」？作废后不影响库存。`, '作废盘点单', { type: 'warning' })
  } catch (e) {
    return
  }
  await cancelStockCheckApi(row.id)
  ElMessage.success('已作废')
  loadScList()
}

// ========== 变动记录 ==========
const logLoading = ref(false)
const logList = ref([])
const logSearch = reactive({ changeType: null })
const logPagination = reactive({ pageNum: 1, pageSize: 10, total: 0, sortField: null, sortOrder: null })

const loadLogs = async () => {
  logLoading.value = true
  try {
    const res = await getInventoryLogsApi({
      changeType: logSearch.changeType,
      sortField: logPagination.sortField,
      sortOrder: logPagination.sortOrder,
      pageNum: logPagination.pageNum,
      pageSize: logPagination.pageSize
    })
    logList.value = res.data.list
    logPagination.total = res.data.total
    if (logList.value.length === 0 && logPagination.pageNum > 1) {
      logPagination.pageNum -= 1
      await loadLogs()
    }
  } finally {
    logLoading.value = false
  }
}

const handleLogSearch = () => { logPagination.pageNum = 1; loadLogs() }

const resetLogSearch = () => {
  logSearch.changeType = null
  logPagination.sortField = null
  logPagination.sortOrder = null
  handleLogSearch()
}

const handleLogSort = ({ prop, order }) => {
  logPagination.sortField = order ? prop : null
  logPagination.sortOrder = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : null
  handleLogSearch()
}

const handleLogSizeChange = () => { logPagination.pageNum = 1; loadLogs() }

// ========== 下拉选项 ==========
const productList = ref([])
const warehouseList = ref([])

const loadProductOptions = async () => {
  // 带上已下架产品，否则库存行点入库时产品框会显示成裸 ID（P021）
  const res = await getProductListApi(true)
  productList.value = res.data || []
}

const loadWarehouseOptions = async () => {
  const res = await getWarehouseListApi()
  warehouseList.value = res.data || []
}

// 顶部铃铛点"查看全部"会带 ?tab=inventory&lowStock=1 进来，这里接住
// 兼容旧链接：?tab=warning 一律落到"库存总览 + 只看低库存"
const applyTabFromQuery = () => {
  const tab = route.query.tab
  if (!tab) return
  if (tab === 'warning') {
    activeTab.value = 'inventory'
    invSearch.lowStock = true
    handleInvSearch()
    return
  }
  if (['warehouse', 'inventory', 'logs'].includes(tab)) {
    activeTab.value = tab
    if (tab === 'inventory' && route.query.lowStock === '1') {
      invSearch.lowStock = true
      handleInvSearch()
    }
  }
}

// 用 fullPath 监听：已在"库存总览"时再点铃铛，tab 不变但 lowStock 变了，也要生效
watch(() => route.fullPath, applyTabFromQuery)

onMounted(() => {
  loadWarehouses()
  loadInventory()
  loadLogs()
  loadProductOptions()
  loadWarehouseOptions()
  loadLowStockCount()
  loadWhOverview()
  applyTabFromQuery()
  window.addEventListener('resize', handleWhResize)
  // 订单/出入库变化后容量数据自动更新
  whRefreshTimer = setInterval(() => loadWhOverview(true), 60000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleWhResize)
  if (whRefreshTimer) clearInterval(whRefreshTimer)
  whMap?.dispose()
  whMap = null
})
</script>

<style scoped>
/* 标签页内搜索卡与表格的间距 */
.search-card {
  margin-bottom: 16px;
}
.value-edit { margin-left: 4px; padding: 0; }
.check-toolbar { display: flex; justify-content: space-between; align-items: center; margin: 8px 0 12px; }
.check-tip { color: var(--el-text-color-regular); font-size: 14px; }
.check-tip strong { margin: 0 4px; }
.check-toolbar-right { display: flex; align-items: center; gap: 12px; }
/* 库存不足提醒：只在有不足时出现 */
.tab-with-badge { display: inline-flex; align-items: center; gap: 6px; }
.low-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #F56C6C;
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  font-weight: 600;
}
.low-hint {
  color: #F56C6C;
  font-size: 12px;
  font-weight: 400;
  cursor: pointer;
  border-bottom: 1px dashed #F56C6C;
  line-height: 1.2;
}
.low-hint:hover { color: #c45656; border-bottom-color: #c45656; }

/* 仓库容量与地点可视化 */
.wh-visual {
  margin-bottom: 16px;
  padding: 14px 16px 4px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: linear-gradient(180deg, #f8fbff, #f3f7fd);
}
.wh-visual-head {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.wh-visual-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.wh-visual-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.wh-cap-warn {
  font-style: normal;
  color: #f59e0b;
}
.wh-cap-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 12px;
}
.wh-cap-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}
.wh-cap-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.wh-cap-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
}
.wh-cap-loc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}
.wh-cap-num {
  font-size: 12px;
  color: var(--el-text-color-regular);
  white-space: nowrap;
}
.wh-cap-num b { margin-left: 6px; color: #22c55e; }
.wh-cap-num.danger b { color: #ef4444; }
.wh-map {
  width: 100%;
  height: 320px;
}
@media (max-width: 992px) {
  .wh-map { height: 260px; }
}
</style>
