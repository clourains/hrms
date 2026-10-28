import { request } from '@/utils/request'

// ========== 产品 ==========
export const getProductPageApi = (data) => request.post('/product/page', data)
// includeDisabled = true 时连下架产品一起返回，出入库/订单下拉用（P021）
export const getProductListApi = (includeDisabled) =>
  request.get('/product/list', includeDisabled === undefined ? {} : { includeDisabled })
export const getProductDetailApi = (id) => request.get(`/product/${id}`)
export const addProductApi = (data) => request.post('/product', data)
export const updateProductApi = (data) => request.put('/product', data)
export const deleteProductApi = (id) => request.delete(`/product/${id}`)

// ========== 仓库 ==========
export const getWarehousePageApi = (data) => request.post('/warehouse/page', data)
// onlyEnabled 不传 = 只取启用的仓库（下拉框用）；传 false = 全部（仓库管理页用）
export const getWarehouseListApi = (onlyEnabled) =>
  request.get('/warehouse/list', onlyEnabled === undefined ? {} : { onlyEnabled })
// 仓库容量与地点可视化：容量 + 当前库存占用 + 使用率
export const getWarehouseOverviewApi = () => request.get('/warehouse/overview')
export const addWarehouseApi = (data) => request.post('/warehouse', data)
export const updateWarehouseApi = (data) => request.put('/warehouse', data)
export const deleteWarehouseApi = (id) => request.delete(`/warehouse/${id}`)

// ========== 库存 ==========
export const getInventoryPageApi = (data) => request.post('/inventory/page', data)
export const getInventoryLogsApi = (data) => request.post('/inventory/logs', data)
export const changeStockApi = (data) => request.post('/inventory/change', data)
export const getInventoryWarningApi = () => request.get('/inventory/warning')
export const getInventoryWarningCountApi = () => request.get('/inventory/warning/count')
export const updateWarningQuantityApi = (id, warningQuantity) =>
  request.put(`/inventory/${id}/warning`, { warningQuantity })
// 仓间调拨 / 盘点（P019）
export const transferStockApi = (data) => request.post('/inventory/transfer', data)
export const checkStockApi = (data) => request.post('/inventory/check', data)

// ========== 库存盘点单（P019） ==========
export const getStockCheckPageApi = (data) => request.post('/stock-check/page', data)
export const getStockCheckDetailApi = (id) => request.get(`/stock-check/${id}`)
// 建单时按仓库带出当前库存行
export const getStockCheckRowsApi = (warehouseId) => request.get('/stock-check/inventory-rows', { warehouseId })
export const createStockCheckApi = (data) => request.post('/stock-check', data)
export const updateStockCheckApi = (data) => request.put('/stock-check', data)
export const confirmStockCheckApi = (id) => request.post(`/stock-check/${id}/confirm`)
export const cancelStockCheckApi = (id) => request.post(`/stock-check/${id}/cancel`)

// ========== 客户 ==========
export const getCustomerPageApi = (data) => request.post('/customer/page', data)
export const getCustomerListApi = () => request.get('/customer/list')
export const addCustomerApi = (data) => request.post('/customer', data)
export const updateCustomerApi = (data) => request.put('/customer', data)
export const deleteCustomerApi = (id) => request.delete(`/customer/${id}`)

// ========== 供应商 ==========
export const getSupplierPageApi = (data) => request.post('/supplier/page', data)
export const getSupplierListApi = () => request.get('/supplier/list')
export const addSupplierApi = (data) => request.post('/supplier', data)
export const updateSupplierApi = (data) => request.put('/supplier', data)
export const deleteSupplierApi = (id) => request.delete(`/supplier/${id}`)

// ========== 销售订单 ==========
export const getOrderPageApi = (data) => request.post('/order/page', data)
export const getOrderDetailApi = (id) => request.get(`/order/${id}`)
export const getOrderItemsApi = (orderId) => request.get(`/order/items/${orderId}`)
export const createOrderApi = (data) => request.post('/order', data)
export const updateOrderApi = (data) => request.put('/order', data)
export const updateOrderStatusApi = (id, status) => request.put(`/order/status/${id}/${status}`)
export const deleteOrderApi = (id) => request.delete(`/order/${id}`)

// ========== 发票管理 ==========
export const getInvoicePageApi = (data) => request.post('/invoice/page', data)
// 从销售订单开票（body 需带 orderId）
export const createInvoiceApi = (data) => request.post('/invoice', data)
export const updateInvoiceApi = (data) => request.put('/invoice', data)
export const updateInvoiceStatusApi = (id, status) => request.put(`/invoice/status/${id}/${status}`)
export const deleteInvoiceApi = (id) => request.delete(`/invoice/${id}`)

// ========== 线下交易台账 ==========
export const getOfflineDealPageApi = (data) => request.post('/offline-deal/page', data)
export const addOfflineDealApi = (data) => request.post('/offline-deal', data)
export const updateOfflineDealApi = (data) => request.put('/offline-deal', data)
export const updateOfflineDealStatusApi = (id, status) => request.put(`/offline-deal/status/${id}/${status}`)
export const deleteOfflineDealApi = (id) => request.delete(`/offline-deal/${id}`)

// ========== 物流发货 ==========
export const getShipmentPageApi = (data) => request.post('/shipment/page', data)
export const getShipmentTracksApi = (id) => request.get(`/shipment/${id}/tracks`)

// 第三方实时轨迹（17TRACK）：按发货单运单号实时查询
export const getShipmentLiveTracksApi = (id) => request.get(`/shipment/${id}/liveTracks`)

// 按任意运单号查询 17TRACK 实时轨迹
export const queryLiveTrackByNoApi = (trackingNo) => request.get('/shipment/liveTrackByNo', { trackingNo })
export const addShipmentApi = (data) => request.post('/shipment', data)
export const updateShipmentApi = (data) => request.put('/shipment', data)
export const updateShipmentStatusApi = (id, status) => request.put(`/shipment/status/${id}/${status}`)
export const deleteShipmentApi = (id) => request.delete(`/shipment/${id}`)

// ========== 收付款 ==========
export const getPaymentPageApi = (data) => request.post('/payment/page', data)
export const addPaymentApi = (data) => request.post('/payment', data)
export const deletePaymentApi = (id) => request.delete(`/payment/${id}`)

// ========== 营业分析 ==========
export const getBusinessOverviewApi = () => request.get('/business/overview')
export const getMonthlySalesApi = () => request.get('/business/monthly-sales')
export const getTopCustomersApi = () => request.get('/business/top-customers')
export const getTopProductsApi = () => request.get('/business/top-products')
export const getCountryDistributionApi = () => request.get('/business/country-distribution')
export const getInventoryValueApi = () => request.get('/business/inventory-value')
export const getProfitRankingApi = () => request.get('/business/profit-ranking')
