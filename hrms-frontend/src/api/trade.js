import { request } from '@/utils/request'

// ========== 产品 ==========
export const getProductPageApi = (data) => request.post('/product/page', data)
export const getProductListApi = () => request.get('/product/list')
export const getProductDetailApi = (id) => request.get(`/product/${id}`)
export const addProductApi = (data) => request.post('/product', data)
export const updateProductApi = (data) => request.put('/product', data)
export const deleteProductApi = (id) => request.delete(`/product/${id}`)

// ========== 仓库 ==========
export const getWarehousePageApi = (data) => request.post('/warehouse/page', data)
export const getWarehouseListApi = () => request.get('/warehouse/list')
export const addWarehouseApi = (data) => request.post('/warehouse', data)
export const updateWarehouseApi = (data) => request.put('/warehouse', data)
export const deleteWarehouseApi = (id) => request.delete(`/warehouse/${id}`)

// ========== 库存 ==========
export const getInventoryPageApi = (data) => request.post('/inventory/page', data)
export const getInventoryLogsApi = (data) => request.post('/inventory/logs', data)
export const changeStockApi = (data) => request.post('/inventory/change', data)
export const getInventoryWarningApi = () => request.get('/inventory/warning')

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

// ========== 物流发货 ==========
export const getShipmentPageApi = (data) => request.post('/shipment/page', data)
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
