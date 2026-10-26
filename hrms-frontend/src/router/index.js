import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/portal',
    name: 'Portal',
    component: () => import('@/views/Landing.vue'),
    meta: { title: '首页', requiresAuth: false }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/home',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      {
        path: 'employee',
        name: 'Employee',
        component: () => import('@/views/employee/EmployeeList.vue'),
        meta: { title: '员工管理', icon: 'User', requiresAdmin: true }
      },
      {
        path: 'department',
        name: 'Department',
        component: () => import('@/views/department/DepartmentList.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding', requiresAdmin: true }
      },
      {
        path: 'attendance',
        name: 'Attendance',
        component: () => import('@/views/attendance/AttendanceList.vue'),
        meta: { title: '考勤管理', icon: 'Calendar' }
      },
      {
        path: 'salary',
        name: 'Salary',
        component: () => import('@/views/salary/SalaryList.vue'),
        meta: { title: '薪资管理', icon: 'Money' }
      },
      {
        path: 'performance',
        name: 'Performance',
        component: () => import('@/views/performance/PerformanceList.vue'),
        meta: { title: '绩效管理', icon: 'TrendCharts' }
      },
      {
        path: 'reminder',
        name: 'Reminder',
        component: () => import('@/views/reminder/ReminderList.vue'),
        meta: { title: '到期提醒', icon: 'Bell', requiresAdmin: true }
      },
      {
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/system/AuditLogList.vue'),
        meta: { title: '审计日志', icon: 'Document', requiresAdmin: true }
      },
      {
        path: 'analytics',
        name: 'Analytics',
        component: () => import('@/views/analytics/Analytics.vue'),
        meta: { title: '智能分析', icon: 'DataAnalysis', requiresAdmin: true }
      },
      {
        path: 'business',
        name: 'Business',
        component: () => import('@/views/business/BusinessAnalysis.vue'),
        meta: { title: '营业分析', icon: 'Histogram', requiresAdmin: true }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '销售订单', icon: 'Tickets', requiresAdmin: true }
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('@/views/product/ProductList.vue'),
        meta: { title: '产品管理', icon: 'Goods', requiresAdmin: true }
      },
      {
        path: 'warehouse',
        name: 'Warehouse',
        component: () => import('@/views/warehouse/WarehouseList.vue'),
        meta: { title: '仓库库存', icon: 'Box', requiresAdmin: true }
      },
      {
        path: 'customer',
        name: 'Customer',
        component: () => import('@/views/customer/CustomerList.vue'),
        meta: { title: '境外客户', icon: 'UserFilled', requiresAdmin: true }
      },
      {
        path: 'supplier',
        name: 'Supplier',
        component: () => import('@/views/supplier/SupplierList.vue'),
        meta: { title: '供应商', icon: 'Van', requiresAdmin: true }
      },
      {
        path: 'shipment',
        name: 'Shipment',
        component: () => import('@/views/shipment/ShipmentList.vue'),
        meta: { title: '物流跟踪', icon: 'Ship', requiresAdmin: true }
      },
      {
        path: 'payment',
        name: 'Payment',
        component: () => import('@/views/payment/PaymentList.vue'),
        meta: { title: '收付款', icon: 'Wallet', requiresAdmin: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = userStore.token

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 星跃出海国际贸易有限公司`
  }

  if (to.meta.requiresAuth && !token) {
    // 需要登录但未登录，先进品牌首页
    next({ path: '/portal', query: { redirect: to.fullPath } })
  } else if ((to.path === '/login' || to.path === '/portal') && token) {
    // 已登录访问登录页/品牌首页，直接进系统首页
    next({ path: '/home' })
  } else if (to.meta.requiresAdmin && userStore.role !== 'admin') {
    // P045 角色权限：非管理员访问管理页面，弹回首页
    ElMessage.warning('您没有权限访问该页面')
    next({ path: '/home' })
  } else {
    next()
  }
})

export default router
