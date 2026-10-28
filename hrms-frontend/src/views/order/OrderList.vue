<template>
  <div class="page-container page-stack">
    <!-- 页头 -->
    <PageHeader
      title="销售订单"
      description="订单记录、发票管理与线下交易台账，一站式跟进成交到开票全过程"
      icon="ShoppingCart"
    >
      <template #actions>
        <el-button v-if="activeTab === 'order'" type="primary" :icon="Plus" @click="handleAdd">新建订单</el-button>
        <el-button v-else-if="activeTab === 'invoice'" type="primary" :icon="Plus" @click="openInvoiceDialog()">从订单开票</el-button>
        <el-button v-else type="primary" :icon="Plus" @click="openDealDialog()">新增线下交易</el-button>
        <el-button v-if="activeTab === 'order'" :icon="Upload" @click="orderImportRef.open()">Excel 导入</el-button>
        <el-button v-else-if="activeTab === 'invoice'" :icon="Upload" @click="invoiceImportRef.open()">Excel 导入</el-button>
        <el-button v-else :icon="Upload" @click="dealImportRef.open()">Excel 导入</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="activeTab" class="trade-tabs" @tab-change="handleTabChange">
      <!-- ========== 订单记录 ========== -->
      <el-tab-pane name="order">
        <template #label>
          <span class="tab-label"><el-icon><Tickets /></el-icon>订单记录<span class="tab-count">{{ orderPagination.total }}</span></span>
        </template>

        <div class="tab-body">
          <div class="search-card">
            <el-form :inline="true" :model="searchForm">
              <el-form-item label="关键字">
                <el-input v-model="searchForm.keyword" placeholder="订单号" clearable style="width: 200px" @keyup.enter="handleOrderSearch" />
              </el-form-item>
              <el-form-item label="订单状态">
                <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
                  <el-option v-for="(label, s) in statusMap" :key="s" :label="label" :value="Number(s)" />
                </el-select>
              </el-form-item>
              <el-form-item label="排序">
                <el-select v-model="orderSort" style="width: 170px" @change="handleOrderSearch">
                  <el-option v-for="opt in orderSortOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
                </el-select>
              </el-form-item>
              <el-form-item class="search-submit">
                <el-button type="primary" :icon="Search" @click="handleOrderSearch">搜索</el-button>
                <el-button :icon="Refresh" @click="handleOrderReset">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <div v-loading="loading" class="card-list">
            <div v-for="row in orderData" :key="row.order.id" class="record-card order-card">
              <div class="card-head">
                <div class="head-main">
                  <span class="record-no" @click="handleViewItems(row)">{{ row.order.orderNo }}</span>
                  <el-tag :type="statusTag(row.order.status)" size="small" effect="dark">{{ statusMap[row.order.status] }}</el-tag>
                  <el-tag :type="payTag(row.order.paymentStatus)" size="small" effect="plain">{{ payMap[row.order.paymentStatus] }}</el-tag>
                </div>
                <div class="head-amount">${{ formatNum(row.order.totalAmountUsd) }}</div>
              </div>

              <div class="card-meta">
                <span><el-icon><User /></el-icon>{{ row.customerName || '-' }}</span>
                <span><el-icon><Location /></el-icon>{{ row.customerCountry || '-' }}</span>
                <span><el-icon><Calendar /></el-icon>{{ row.order.orderDate }}</span>
                <span><el-icon><Box /></el-icon>{{ row.order.totalQuantity }} 件</span>
              </div>

              <!-- 六阶段进度条 -->
              <div class="progress" :class="{ 'is-cancelled': row.order.status === 9 }">
                <div v-for="(step, i) in orderSteps" :key="step.value" class="progress-item"
                     :class="{ done: stepDone(row.order, i), current: row.order.status === step.value }">
                  <div class="progress-dot"><el-icon v-if="stepDone(row.order, i)"><Check /></el-icon></div>
                  <div class="progress-label">{{ step.label }}</div>
                  <div v-if="i < orderSteps.length - 1" class="progress-line" :class="{ done: stepDone(row.order, i + 1) }"></div>
                </div>
              </div>

              <div class="card-foot">
                <span class="foot-tip">{{ row.order.remark || '暂无订单备注' }}</span>
                <div class="foot-actions">
                  <el-button type="primary" link :icon="View" @click="handleViewItems(row)">明细</el-button>
                  <el-button v-if="row.order.status === 0" type="success" link @click="handleStatus(row, 1)">确认</el-button>
                  <el-button v-if="row.order.status === 1" type="warning" link @click="handleStatus(row, 2)">备货</el-button>
                  <el-button v-if="row.order.status === 2" type="primary" link @click="handleStatus(row, 3)">发货</el-button>
                  <el-button v-if="row.order.status === 3" type="warning" link @click="handleStatus(row, 4)">签收</el-button>
                  <el-button v-if="row.order.status === 4" type="success" link @click="handleStatus(row, 5)">完成</el-button>
                  <el-button v-if="(row.order.status ?? 0) <= 2" type="info" link @click="handleStatus(row, 9)">取消</el-button>
                  <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
                </div>
              </div>
            </div>

            <el-empty v-if="!loading && orderData.length === 0" description="暂无订单记录" />
          </div>

          <div class="page-pagination">
            <el-pagination
              v-model:current-page="orderPagination.pageNum"
              v-model:page-size="orderPagination.pageSize"
              :total="orderPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleOrderSizeChange"
              @current-change="loadOrders"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- ========== 发票管理 ========== -->
      <el-tab-pane name="invoice">
        <template #label>
          <span class="tab-label"><el-icon><Document /></el-icon>发票管理<span class="tab-count">{{ invoicePagination.total }}</span></span>
        </template>

        <div class="tab-body">
          <div class="search-card">
            <el-form :inline="true" :model="invoiceSearchForm">
              <el-form-item label="关键字">
                <el-input v-model="invoiceSearchForm.keyword" placeholder="发票号 / 订单号 / 客户" clearable style="width: 230px" @keyup.enter="handleInvoiceSearch" />
              </el-form-item>
              <el-form-item label="开票状态">
                <el-select v-model="invoiceSearchForm.status" placeholder="全部" clearable style="width: 130px">
                  <el-option v-for="(label, s) in invoiceStatusMap" :key="s" :label="label" :value="Number(s)" />
                </el-select>
              </el-form-item>
              <el-form-item class="search-submit">
                <el-button type="primary" :icon="Search" @click="handleInvoiceSearch">搜索</el-button>
                <el-button :icon="Refresh" @click="handleInvoiceReset">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <div v-loading="invoiceLoading" class="card-list">
            <div v-for="row in invoiceData" :key="row.id" class="record-card invoice-card">
              <div class="card-head">
                <div class="head-main">
                  <span class="record-no">{{ row.invoiceNo }}</span>
                  <el-tag :type="invoiceStatusTag(row.status)" size="small" effect="dark">{{ invoiceStatusMap[row.status] }}</el-tag>
                  <el-tag size="small" effect="plain">{{ row.invoiceType }}</el-tag>
                </div>
                <div class="head-amount">{{ moneySymbol(row.currency) }}{{ formatNum(row.amount) }}</div>
              </div>

              <div class="card-meta">
                <span><el-icon><User /></el-icon>{{ row.customerName || '-' }}</span>
                <span><el-icon><Tickets /></el-icon>{{ row.orderNo || '未关联订单' }}</span>
                <span><el-icon><Calendar /></el-icon>{{ row.issueDate || '待开票' }}</span>
              </div>

              <div class="card-body-grid">
                <div class="info"><span class="info-label">发票抬头</span><span class="info-value">{{ row.title }}</span></div>
                <div class="info"><span class="info-label">税号</span><span class="info-value">{{ row.taxNo || '-' }}</span></div>
                <div class="info"><span class="info-label">备注</span><span class="info-value">{{ row.remark || '-' }}</span></div>
              </div>

              <div class="card-foot">
                <span class="foot-tip">开票金额 {{ moneySymbol(row.currency) }}{{ formatNum(row.amount) }}</span>
                <div class="foot-actions">
                  <el-button v-if="row.status === 0" type="warning" link @click="handleInvoiceStatus(row, 1)">开始开票</el-button>
                  <el-button v-if="row.status === 1" type="success" link @click="handleInvoiceStatus(row, 2)">标记已开票</el-button>
                  <el-button v-if="row.status === 0" type="primary" link @click="openInvoiceDialog(row)">编辑</el-button>
                  <el-button v-if="row.status !== 3" type="info" link @click="handleInvoiceStatus(row, 3)">作废</el-button>
                  <el-button type="danger" link :icon="Delete" @click="handleInvoiceDelete(row)">删除</el-button>
                </div>
              </div>
            </div>

            <el-empty v-if="!invoiceLoading && invoiceData.length === 0" description="暂无发票，可点击右上角从订单开票" />
          </div>

          <div class="page-pagination">
            <el-pagination
              v-model:current-page="invoicePagination.pageNum"
              v-model:page-size="invoicePagination.pageSize"
              :total="invoicePagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleInvoiceSizeChange"
              @current-change="loadInvoices"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- ========== 线下交易 ========== -->
      <el-tab-pane name="deal">
        <template #label>
          <span class="tab-label"><el-icon><Money /></el-icon>线下交易<span class="tab-count">{{ dealPagination.total }}</span></span>
        </template>

        <div class="tab-body">
          <div class="search-card">
            <el-form :inline="true" :model="dealSearchForm">
              <el-form-item label="关键字">
                <el-input v-model="dealSearchForm.keyword" placeholder="编号 / 客户 / 商品" clearable style="width: 220px" @keyup.enter="handleDealSearch" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="dealSearchForm.status" placeholder="全部" clearable style="width: 120px">
                  <el-option v-for="(label, s) in dealStatusMap" :key="s" :label="label" :value="Number(s)" />
                </el-select>
              </el-form-item>
              <el-form-item label="成交日期">
                <el-date-picker
                  v-model="dealSearchForm.dateRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  style="width: 260px"
                />
              </el-form-item>
              <el-form-item class="search-submit">
                <el-button type="primary" :icon="Search" @click="handleDealSearch">搜索</el-button>
                <el-button :icon="Refresh" @click="handleDealReset">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <div v-loading="dealLoading" class="card-list">
            <div v-for="row in dealData" :key="row.id" class="record-card deal-card">
              <div class="card-head">
                <div class="head-main">
                  <span class="record-no">{{ row.dealNo }}</span>
                  <el-tag :type="dealStatusTag(row.status)" size="small" effect="dark">{{ dealStatusMap[row.status] }}</el-tag>
                  <el-tag size="small" effect="plain">{{ payMethodMap[row.payMethod] || '其他' }}</el-tag>
                </div>
                <div class="head-amount">{{ moneySymbol(row.currency) }}{{ formatNum(row.amount) }}</div>
              </div>

              <div class="card-meta">
                <span><el-icon><User /></el-icon>{{ row.customerName }}{{ row.customerCountry ? ' · ' + row.customerCountry : '' }}</span>
                <span><el-icon><Calendar /></el-icon>{{ row.dealDate }}</span>
                <span><el-icon><Postcard /></el-icon>{{ row.voucher || '无凭证' }}</span>
              </div>

              <div class="card-body-grid">
                <div class="info"><span class="info-label">商品/服务</span><span class="info-value">{{ row.itemName }}</span></div>
                <div class="info"><span class="info-label">收款方式</span><span class="info-value">{{ payMethodMap[row.payMethod] || '其他' }}</span></div>
                <div class="info"><span class="info-label">备注</span><span class="info-value">{{ row.remark || '-' }}</span></div>
              </div>

              <div class="card-foot">
                <span class="foot-tip">成交金额 {{ moneySymbol(row.currency) }}{{ formatNum(row.amount) }}</span>
                <div class="foot-actions">
                  <el-button v-if="row.status === 0" type="success" link @click="handleDealStatus(row, 1)">确认成交</el-button>
                  <el-button v-if="row.status === 0" type="primary" link @click="openDealDialog(row)">编辑</el-button>
                  <el-button v-if="row.status !== 9" type="info" link @click="handleDealStatus(row, 9)">取消</el-button>
                  <el-button type="danger" link :icon="Delete" @click="handleDealDelete(row)">删除</el-button>
                </div>
              </div>
            </div>

            <el-empty v-if="!dealLoading && dealData.length === 0" description="暂无线下交易记录" />
          </div>

          <div class="page-pagination">
            <el-pagination
              v-model:current-page="dealPagination.pageNum"
              v-model:page-size="dealPagination.pageSize"
              :total="dealPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleDealSizeChange"
              @current-change="loadDeals"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

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

    <!-- 发票对话框（新增 / 编辑） -->
    <el-dialog v-model="invoiceDialogVisible" :title="invoiceForm.id ? '编辑发票' : '从订单开票'" width="620px" destroy-on-close>
      <el-form ref="invoiceFormRef" :model="invoiceForm" :rules="invoiceRules" label-width="90px">
        <el-form-item v-if="!invoiceForm.id" label="关联订单" prop="orderId">
          <el-select
            v-model="invoiceForm.orderId"
            placeholder="输入订单号搜索"
            filterable
            remote
            reserve-keyword
            :remote-method="searchOrders"
            :loading="orderSearchLoading"
            style="width: 100%"
            @change="handleOrderSelect"
          >
            <el-option
              v-for="o in orderOptions"
              :key="o.order.id"
              :label="`${o.order.orderNo} · ${o.customerName || ''} · $${formatNum(o.order.totalAmountUsd)}`"
              :value="o.order.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="关联订单">
          <el-input :model-value="invoiceForm.orderNo" disabled />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="发票号" prop="invoiceNo">
              <el-input v-model="invoiceForm.invoiceNo" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发票类型">
              <el-select v-model="invoiceForm.invoiceType" style="width: 100%">
                <el-option label="普通发票" value="普通发票" />
                <el-option label="专用发票" value="专用发票" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="发票抬头" prop="title">
          <el-input v-model="invoiceForm.title" placeholder="公司名称" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="税号">
              <el-input v-model="invoiceForm.taxNo" placeholder="纳税人识别号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="币种">
              <el-select v-model="invoiceForm.currency" style="width: 100%">
                <el-option label="USD 美元" value="USD" />
                <el-option label="CNY 人民币" value="CNY" />
                <el-option label="EUR 欧元" value="EUR" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开票金额" prop="amount">
              <el-input-number v-model="invoiceForm.amount" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="invoiceForm.remark" placeholder="如合同号、开票说明" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="invoiceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="invoiceSubmitting" @click="submitInvoice">保存</el-button>
      </template>
    </el-dialog>

    <!-- 线下交易对话框（新增 / 编辑） -->
    <el-dialog v-model="dealDialogVisible" :title="dealForm.id ? '编辑线下交易' : '新增线下交易'" width="620px" destroy-on-close>
      <el-form ref="dealFormRef" :model="dealForm" :rules="dealRules" label-width="90px">
        <el-form-item label="客户" prop="customerSel">
          <el-select
            v-model="dealForm.customerSel"
            placeholder="选择或直接输入客户名称"
            filterable
            allow-create
            default-first-option
            style="width: 100%"
            @change="handleDealCustomerChange"
          >
            <el-option v-for="c in customerList" :key="c.id" :label="`${c.companyName} (${c.country || ''})`" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="商品/服务" prop="itemName">
          <el-input v-model="dealForm.itemName" placeholder="本次线下成交的商品或服务" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="币种">
              <el-select v-model="dealForm.currency" style="width: 100%">
                <el-option label="USD 美元" value="USD" />
                <el-option label="CNY 人民币" value="CNY" />
                <el-option label="EUR 欧元" value="EUR" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成交金额" prop="amount">
              <el-input-number v-model="dealForm.amount" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="成交日期" prop="dealDate">
              <el-date-picker v-model="dealForm.dealDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收款方式">
              <el-select v-model="dealForm.payMethod" style="width: 100%">
                <el-option v-for="(label, m) in payMethodMap" :key="m" :label="label" :value="Number(m)" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="凭证">
          <el-input v-model="dealForm.voucher" placeholder="收据号、转账流水号或凭证链接" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="dealForm.remark" placeholder="成交背景、约定事项等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dealDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dealSubmitting" @click="submitDeal">保存</el-button>
      </template>
    </el-dialog>

    <ImportDialog ref="orderImportRef" module="order" label="销售订单" tip="按「外部单号」分组生成订单：同一单号的多行会合并为一张订单；客户编码、产品SKU、仓库编码必须已存在" @success="loadOrders" />
    <ImportDialog ref="invoiceImportRef" module="invoice" label="发票" tip="关联订单号必须已存在；发票号留空则自动生成，已存在的发票号会自动跳过" @success="loadInvoices" />
    <ImportDialog ref="dealImportRef" module="offline-deal" label="线下交易" tip="交易编号留空则自动生成；客户名称、商品/服务、成交日期、金额为必填" @success="loadDeals" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, Plus, Delete, View, Check, Upload,
  Tickets, Document, Money, User, Location, Calendar, Box, Postcard
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import ImportDialog from '@/components/ImportDialog.vue'
import {
  getOrderPageApi, createOrderApi, updateOrderStatusApi, deleteOrderApi,
  getOrderItemsApi, getCustomerListApi, getProductListApi, getWarehouseListApi,
  getInvoicePageApi, createInvoiceApi, updateInvoiceApi, updateInvoiceStatusApi, deleteInvoiceApi,
  getOfflineDealPageApi, addOfflineDealApi, updateOfflineDealApi, updateOfflineDealStatusApi, deleteOfflineDealApi
} from '@/api/trade'

const statusMap = { 0: '待确认', 1: '已确认', 2: '备货中', 3: '已发货', 4: '已签收', 5: '已完成', 9: '已取消' }
const payMap = { 0: '未付款', 1: '部分付款', 2: '已付清' }
const statusTag = (s) => ({ 0: 'info', 1: 'primary', 2: 'warning', 3: 'warning', 4: 'success', 5: 'success', 9: 'danger' }[s] || 'info')
const payTag = (s) => ({ 0: 'danger', 1: 'warning', 2: 'success' }[s] || 'info')

const invoiceStatusMap = { 0: '待开票', 1: '开票中', 2: '已开票', 3: '已作废' }
const invoiceStatusTag = (s) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }[s] || 'info')
const dealStatusMap = { 0: '待确认', 1: '已确认', 9: '已取消' }
const dealStatusTag = (s) => ({ 0: 'info', 1: 'success', 9: 'danger' }[s] || 'info')
const payMethodMap = { 1: '银行转账', 2: '现金', 3: '微信', 4: '支付宝', 9: '其他' }

const moneySymbol = (currency) => ({ USD: '$', CNY: '¥', EUR: '€' }[currency] || '')
const formatNum = (n) => (n == null ? '0.00' : Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }))

const activeTab = ref('order')
const orderImportRef = ref(null)
const invoiceImportRef = ref(null)
const dealImportRef = ref(null)
const customerList = ref([])
const productList = ref([])
const warehouseList = ref([])

/* ========== 订单记录 ========== */
const orderSteps = [
  { value: 0, label: '待确认' },
  { value: 1, label: '已确认' },
  { value: 2, label: '备货中' },
  { value: 3, label: '已发货' },
  { value: 4, label: '已签收' },
  { value: 5, label: '已完成' }
]
const stepDone = (order, index) => order.status !== 9 && order.status >= index

const loading = ref(false)
const orderData = ref([])
const searchForm = reactive({ keyword: '', status: null })
const orderPagination = reactive({ pageNum: 1, pageSize: 10, total: 0, sortField: 'orderDate', sortOrder: 'desc' })
const orderSortOptions = [
  { key: 'orderDate_desc', label: '下单日期 新→旧', field: 'orderDate', order: 'desc' },
  { key: 'orderDate_asc', label: '下单日期 旧→新', field: 'orderDate', order: 'asc' },
  { key: 'totalAmountUsd_desc', label: '金额 高→低', field: 'totalAmountUsd', order: 'desc' },
  { key: 'totalAmountUsd_asc', label: '金额 低→高', field: 'totalAmountUsd', order: 'asc' }
]
const orderSort = ref('orderDate_desc')

const loadOrders = async () => {
  loading.value = true
  try {
    const sort = orderSortOptions.find(o => o.key === orderSort.value)
    const res = await getOrderPageApi({
      ...searchForm,
      sortField: sort ? sort.field : null,
      sortOrder: sort ? sort.order : null,
      pageNum: orderPagination.pageNum,
      pageSize: orderPagination.pageSize
    })
    orderData.value = res.data.list
    orderPagination.total = res.data.total
  } finally {
    loading.value = false
  }
}
const handleOrderSearch = () => { orderPagination.pageNum = 1; loadOrders() }
const handleOrderReset = () => {
  searchForm.keyword = ''
  searchForm.status = null
  orderSort.value = 'orderDate_desc'
  handleOrderSearch()
}
const handleOrderSizeChange = () => { orderPagination.pageNum = 1; loadOrders() }

/* ========== 订单新建 ========== */
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)

const defaultItem = () => ({ productId: null, warehouseId: null, quantity: 1, unitPriceUsd: 0 })
const form = reactive({ customerId: null, orderDate: new Date().toISOString().slice(0, 10), exchangeRate: 7.2, remark: '', items: [defaultItem()] })
const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择订单日期', trigger: 'change' }]
}

// 仓库必选：不填的话后端既不锁库存也不扣库存，整条链路静默失效
const validItems = computed(() => form.items.filter(i => i.productId && i.warehouseId && i.quantity > 0))
const missingWarehouse = computed(() => form.items.some(i => i.productId && i.quantity > 0 && !i.warehouseId))
const totalQuantity = computed(() => validItems.value.reduce((sum, i) => sum + (i.quantity || 0), 0))
const totalAmount = computed(() => validItems.value.reduce((sum, i) => sum + (i.quantity || 0) * (i.unitPriceUsd || 0), 0))

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
    ElMessage.warning('请至少添加一条有效明细（选择产品、仓库并填写数量）')
    return
  }
  if (missingWarehouse.value) {
    ElMessage.warning('每条明细都要选择发货仓库，否则下单不会锁定库存、发货也不会扣减库存')
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
    loadOrders()
  } finally {
    submitting.value = false
  }
}

const handleStatus = async (row, status) => {
  const actionText = statusMap[status]
  await ElMessageBox.confirm(`确定将订单「${row.order.orderNo}」变更为「${actionText}」吗？`, '提示', { type: 'warning' })
  await updateOrderStatusApi(row.order.id, status)
  ElMessage.success('操作成功')
  loadOrders()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除订单「${row.order.orderNo}」吗？`, '提示', { type: 'warning' })
  await deleteOrderApi(row.order.id)
  ElMessage.success('删除成功')
  loadOrders()
}

/* ========== 明细抽屉 ========== */
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

/* ========== 发票管理 ========== */
const invoiceLoading = ref(false)
const invoiceData = ref([])
const invoiceSearchForm = reactive({ keyword: '', status: null })
const invoicePagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const loadInvoices = async () => {
  invoiceLoading.value = true
  try {
    const res = await getInvoicePageApi({
      ...invoiceSearchForm,
      pageNum: invoicePagination.pageNum,
      pageSize: invoicePagination.pageSize
    })
    invoiceData.value = res.data.list
    invoicePagination.total = res.data.total
  } finally {
    invoiceLoading.value = false
  }
}
const handleInvoiceSearch = () => { invoicePagination.pageNum = 1; loadInvoices() }
const handleInvoiceReset = () => {
  invoiceSearchForm.keyword = ''
  invoiceSearchForm.status = null
  handleInvoiceSearch()
}
const handleInvoiceSizeChange = () => { invoicePagination.pageNum = 1; loadInvoices() }

const invoiceDialogVisible = ref(false)
const invoiceSubmitting = ref(false)
const invoiceFormRef = ref(null)
const invoiceForm = reactive({
  id: null, orderId: null, orderNo: '', invoiceNo: '', title: '', taxNo: '',
  invoiceType: '普通发票', currency: 'USD', amount: null, remark: ''
})
const invoiceRules = {
  orderId: [{ required: true, message: '请选择关联订单', trigger: 'change' }],
  title: [{ required: true, message: '请填写发票抬头', trigger: 'blur' }],
  amount: [{ required: true, message: '请填写开票金额', trigger: 'blur' }]
}

// 订单远程搜索，用于「从订单开票」
const orderOptions = ref([])
const orderSearchLoading = ref(false)
const searchOrders = async (keyword) => {
  orderSearchLoading.value = true
  try {
    const res = await getOrderPageApi({ keyword, pageNum: 1, pageSize: 20 })
    orderOptions.value = res.data.list || []
  } finally {
    orderSearchLoading.value = false
  }
}

const openInvoiceDialog = async (row) => {
  Object.assign(invoiceForm, {
    id: null, orderId: null, orderNo: '', invoiceNo: '', title: '', taxNo: '',
    invoiceType: '普通发票', currency: 'USD', amount: null, remark: ''
  })
  if (row) {
    Object.assign(invoiceForm, {
      id: row.id,
      orderId: row.orderId,
      orderNo: row.orderNo,
      invoiceNo: row.invoiceNo,
      title: row.title,
      taxNo: row.taxNo || '',
      invoiceType: row.invoiceType || '普通发票',
      currency: row.currency || 'USD',
      amount: row.amount,
      remark: row.remark || ''
    })
  } else {
    await searchOrders('')
  }
  invoiceDialogVisible.value = true
}

const handleOrderSelect = (orderId) => {
  const option = orderOptions.value.find(o => o.order.id === orderId)
  if (!option) return
  invoiceForm.orderNo = option.order.orderNo
  invoiceForm.title = invoiceForm.title || option.customerName || ''
  invoiceForm.currency = option.order.currency || 'USD'
  invoiceForm.amount = option.order.totalAmountUsd
}

const submitInvoice = async () => {
  await invoiceFormRef.value.validate()
  invoiceSubmitting.value = true
  try {
    if (invoiceForm.id) {
      await updateInvoiceApi({
        id: invoiceForm.id,
        invoiceNo: invoiceForm.invoiceNo,
        title: invoiceForm.title,
        taxNo: invoiceForm.taxNo,
        invoiceType: invoiceForm.invoiceType,
        currency: invoiceForm.currency,
        amount: invoiceForm.amount,
        remark: invoiceForm.remark
      })
      ElMessage.success('发票已更新')
    } else {
      await createInvoiceApi({
        orderId: invoiceForm.orderId,
        invoiceNo: invoiceForm.invoiceNo || null,
        title: invoiceForm.title,
        taxNo: invoiceForm.taxNo,
        invoiceType: invoiceForm.invoiceType,
        currency: invoiceForm.currency,
        amount: invoiceForm.amount,
        remark: invoiceForm.remark
      })
      ElMessage.success('开票申请已创建')
    }
    invoiceDialogVisible.value = false
    loadInvoices()
  } finally {
    invoiceSubmitting.value = false
  }
}

const handleInvoiceStatus = async (row, status) => {
  const actionText = invoiceStatusMap[status]
  await ElMessageBox.confirm(`确定将发票「${row.invoiceNo}」标记为「${actionText}」吗？`, '提示', { type: 'warning' })
  await updateInvoiceStatusApi(row.id, status)
  ElMessage.success('操作成功')
  loadInvoices()
}

const handleInvoiceDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除发票「${row.invoiceNo}」吗？`, '提示', { type: 'warning' })
  await deleteInvoiceApi(row.id)
  ElMessage.success('删除成功')
  loadInvoices()
}

/* ========== 线下交易 ========== */
const dealLoading = ref(false)
const dealData = ref([])
const dealSearchForm = reactive({ keyword: '', status: null, dateRange: [] })
const dealPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const loadDeals = async () => {
  dealLoading.value = true
  try {
    const params = {
      keyword: dealSearchForm.keyword,
      status: dealSearchForm.status,
      pageNum: dealPagination.pageNum,
      pageSize: dealPagination.pageSize
    }
    if (dealSearchForm.dateRange && dealSearchForm.dateRange.length === 2) {
      params.startDate = dealSearchForm.dateRange[0]
      params.endDate = dealSearchForm.dateRange[1]
    }
    const res = await getOfflineDealPageApi(params)
    dealData.value = res.data.list
    dealPagination.total = res.data.total
  } finally {
    dealLoading.value = false
  }
}
const handleDealSearch = () => { dealPagination.pageNum = 1; loadDeals() }
const handleDealReset = () => {
  dealSearchForm.keyword = ''
  dealSearchForm.status = null
  dealSearchForm.dateRange = []
  handleDealSearch()
}
const handleDealSizeChange = () => { dealPagination.pageNum = 1; loadDeals() }

const dealDialogVisible = ref(false)
const dealSubmitting = ref(false)
const dealFormRef = ref(null)
const dealForm = reactive({
  id: null, dealNo: '', customerSel: null, customerId: null, customerName: '', customerCountry: '',
  itemName: '', currency: 'USD', amount: null, dealDate: new Date().toISOString().slice(0, 10),
  payMethod: 1, voucher: '', remark: ''
})
const dealRules = {
  customerSel: [{ required: true, message: '请选择或输入客户', trigger: 'change' }],
  itemName: [{ required: true, message: '请填写商品/服务', trigger: 'blur' }],
  amount: [{ required: true, message: '请填写成交金额', trigger: 'blur' }],
  dealDate: [{ required: true, message: '请选择成交日期', trigger: 'change' }]
}

const openDealDialog = (row) => {
  Object.assign(dealForm, {
    id: null, dealNo: '', customerSel: null, customerId: null, customerName: '', customerCountry: '',
    itemName: '', currency: 'USD', amount: null, dealDate: new Date().toISOString().slice(0, 10),
    payMethod: 1, voucher: '', remark: ''
  })
  if (row) {
    Object.assign(dealForm, {
      id: row.id,
      dealNo: row.dealNo,
      customerSel: row.customerId || row.customerName,
      customerId: row.customerId,
      customerName: row.customerName,
      customerCountry: row.customerCountry || '',
      itemName: row.itemName,
      currency: row.currency || 'USD',
      amount: row.amount,
      dealDate: row.dealDate,
      payMethod: row.payMethod || 1,
      voucher: row.voucher || '',
      remark: row.remark || ''
    })
  }
  dealDialogVisible.value = true
}

// 客户下拉支持直接输入新客户名称（线下临时客户不入 CRM）
const handleDealCustomerChange = (value) => {
  const customer = customerList.value.find(c => c.id === value)
  if (customer) {
    dealForm.customerId = customer.id
    dealForm.customerName = customer.companyName
    dealForm.customerCountry = customer.country || ''
  } else {
    dealForm.customerId = null
    dealForm.customerName = typeof value === 'string' ? value : ''
    dealForm.customerCountry = ''
  }
}

const submitDeal = async () => {
  await dealFormRef.value.validate()
  if (!dealForm.customerName) {
    ElMessage.warning('请选择或输入客户名称')
    return
  }
  const payload = {
    customerId: dealForm.customerId,
    customerName: dealForm.customerName,
    customerCountry: dealForm.customerCountry,
    itemName: dealForm.itemName,
    currency: dealForm.currency,
    amount: dealForm.amount,
    dealDate: dealForm.dealDate,
    payMethod: dealForm.payMethod,
    voucher: dealForm.voucher,
    remark: dealForm.remark
  }
  dealSubmitting.value = true
  try {
    if (dealForm.id) {
      await updateOfflineDealApi({ id: dealForm.id, ...payload })
      ElMessage.success('线下交易已更新')
    } else {
      await addOfflineDealApi(payload)
      ElMessage.success('线下交易已新增')
    }
    dealDialogVisible.value = false
    loadDeals()
  } finally {
    dealSubmitting.value = false
  }
}

const handleDealStatus = async (row, status) => {
  const actionText = dealStatusMap[status]
  await ElMessageBox.confirm(`确定将线下交易「${row.dealNo}」变更为「${actionText}」吗？`, '提示', { type: 'warning' })
  await updateOfflineDealStatusApi(row.id, status)
  ElMessage.success('操作成功')
  loadDeals()
}

const handleDealDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除线下交易「${row.dealNo}」吗？`, '提示', { type: 'warning' })
  await deleteOfflineDealApi(row.id)
  ElMessage.success('删除成功')
  loadDeals()
}

/* ========== 页签切换 ========== */
const handleTabChange = (name) => {
  if (name === 'order') loadOrders()
  else if (name === 'invoice') loadInvoices()
  else loadDeals()
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
  loadOptions()
  // 三个页签都先拉一次，页签上的数量徽标才有值
  loadOrders()
  loadInvoices()
  loadDeals()
})
</script>

<style scoped>
.trade-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}
.tab-count {
  min-width: 20px;
  padding: 0 6px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
}

.tab-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.record-card {
  padding: 16px 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.record-card:hover {
  border-color: var(--el-color-primary-light-7);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.head-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.record-no {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.3px;
  color: var(--el-text-color-primary);
}
.order-card .record-no {
  color: var(--el-color-primary);
  cursor: pointer;
}
.order-card .record-no:hover {
  text-decoration: underline;
}
.head-amount {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-color-danger);
  white-space: nowrap;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.card-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.card-body-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 20px;
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
}
.info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  min-width: 0;
}
.info-label {
  color: var(--el-text-color-secondary);
}
.info-value {
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 六阶段进度条 */
.progress {
  display: flex;
  margin-top: 18px;
}
.progress-item {
  position: relative;
  flex: 1;
  text-align: center;
}
.progress-dot {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin: 0 auto 6px;
  border-radius: 50%;
  font-size: 12px;
  background: var(--el-fill-color);
  color: transparent;
  border: 1px solid var(--el-border-color-lighter);
  transition: all 0.2s;
}
.progress-item.done .progress-dot {
  background: var(--el-color-success);
  border-color: var(--el-color-success);
  color: #fff;
}
.progress-item.current .progress-dot {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: #fff;
  box-shadow: 0 0 0 3px var(--el-color-primary-light-8);
}
.progress-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.progress-item.done .progress-label,
.progress-item.current .progress-label {
  color: var(--el-text-color-primary);
  font-weight: 600;
}
.progress-line {
  position: absolute;
  top: 10px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: var(--el-border-color-lighter);
  z-index: 1;
}
.progress-line.done {
  background: var(--el-color-success);
}
.progress.is-cancelled .progress-dot {
  background: var(--el-fill-color);
  border-color: var(--el-border-color-lighter);
  color: transparent;
  box-shadow: none;
}
.progress.is-cancelled .progress-line {
  background: var(--el-border-color-lighter);
}
.progress.is-cancelled .progress-label {
  color: var(--el-text-color-disabled);
  font-weight: 400;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--el-border-color-lighter);
}
.foot-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 46%;
}
.foot-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.page-pagination {
  display: flex;
  justify-content: flex-end;
}

.items-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}
.total-info {
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.total-num {
  font-weight: 600;
  color: var(--el-color-primary);
}
.total-amount {
  font-weight: 600;
  color: var(--el-color-danger);
  font-size: 16px;
}
</style>
