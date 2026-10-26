import { defineStore } from 'pinia'
import { ref } from 'vue'

// ========== 颜色工具 ==========
const hexToRgb = (hex) => {
  const n = parseInt(hex.replace('#', ''), 16)
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}

const rgbToHex = (r, g, b) => {
  return '#' + [r, g, b].map(v => Math.round(v).toString(16).padStart(2, '0')).join('')
}

// c1 占比 w，c2 占比 1-w
const mix = (c1, c2, w) => {
  const a = hexToRgb(c1)
  const b = hexToRgb(c2)
  return rgbToHex(
    a[0] + (b[0] - a[0]) * w,
    a[1] + (b[1] - a[1]) * w,
    a[2] + (b[2] - a[2]) * w
  )
}

// 主题色预设（art-design-pro 风格色板）
export const PRIMARY_PRESETS = [
  '#409EFF', // 默认蓝
  '#0FDC78', // 翠绿
  '#7C3AED', // 紫罗兰
  '#14B8A6', // 青碧
  '#E6A23C', // 琥珀
  '#F56C6C', // 珊瑚红
  '#EC4899', // 玫粉
  '#6366F1'  // 靛蓝
]

const MODE_KEY = 'hrms-theme-mode'
const PRIMARY_KEY = 'hrms-theme-primary'
const LOCK_KEY = 'hrms-locked'

/**
 * 全局应用状态：主题（暗黑/主题色）、锁屏、页签刷新信号
 * 主题偏好持久化到 localStorage，锁屏状态持久化到 sessionStorage
 */
export const useAppStore = defineStore('app', () => {
  const isDark = ref(localStorage.getItem(MODE_KEY) === 'dark')
  const primaryColor = ref(localStorage.getItem(PRIMARY_KEY) || '#409EFF')
  const locked = ref(sessionStorage.getItem(LOCK_KEY) === '1')

  // 设置抽屉开关（由 Layout 头部的齿轮图标打开）
  const settingsVisible = ref(false)

  // 页面刷新信号：RouteTabs 右键"刷新"时自增，Layout 监听后重挂载 router-view
  const refreshKey = ref(0)

  // 把暗黑 class 和主题色 CSS 变量应用到 <html>
  const applyTheme = () => {
    const el = document.documentElement
    el.classList.toggle('dark', isDark.value)
    el.style.setProperty('--el-color-primary', primaryColor.value)
    // Element Plus 的 light-1~9 / dark-2 派生色：亮色模式向白混合，暗色模式向深底混合
    const base = isDark.value ? '#141414' : '#ffffff'
    for (let i = 1; i <= 9; i++) {
      el.style.setProperty(`--el-color-primary-light-${i}`, mix(base, primaryColor.value, i / 10))
    }
    const darkBase = isDark.value ? '#ffffff' : '#141414'
    el.style.setProperty('--el-color-primary-dark-2', mix(darkBase, primaryColor.value, 0.2))
  }

  const setDark = (v) => {
    isDark.value = !!v
    localStorage.setItem(MODE_KEY, isDark.value ? 'dark' : 'light')
    applyTheme()
  }

  const toggleDark = () => setDark(!isDark.value)

  const setPrimary = (color) => {
    primaryColor.value = color
    localStorage.setItem(PRIMARY_KEY, color)
    applyTheme()
  }

  const lock = () => {
    locked.value = true
    sessionStorage.setItem(LOCK_KEY, '1')
  }

  const unlock = () => {
    locked.value = false
    sessionStorage.removeItem(LOCK_KEY)
  }

  const reloadView = () => {
    refreshKey.value++
  }

  // store 首次实例化（App.vue 启动时）即生效，保证登录页也能应用主题
  applyTheme()

  return {
    isDark,
    primaryColor,
    locked,
    settingsVisible,
    refreshKey,
    setDark,
    toggleDark,
    setPrimary,
    lock,
    unlock,
    reloadView
  }
})
