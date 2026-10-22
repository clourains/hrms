<template>
  <canvas ref="canvasRef" class="flow-bg" aria-hidden="true"></canvas>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

/*
 * 深蓝星海流体背景
 * Canvas 单循环绘制：深蓝渐变底 + 三层缓慢漂移的光晕 + 星点视差 + 流动波纹
 * 组件卸载时会取消动画帧并断开 ResizeObserver，避免内存泄漏
 */

const canvasRef = ref(null)

let ctx = null
let rafId = 0
let observer = null
let w = 0
let h = 0
let stars = []
let blobs = []
let waves = []

const BLUE = [47, 107, 255]
const GREEN = [34, 192, 122]
const GOLD = [217, 166, 60]

const rand = (min, max) => min + Math.random() * (max - min)

const buildScene = () => {
  stars = Array.from({ length: Math.round((w * h) / 9000) }, () => ({
    x: rand(0, w),
    y: rand(0, h),
    r: rand(0.4, 1.7),
    base: rand(0.18, 0.72),
    twinkle: rand(0.4, 1.6),
    phase: rand(0, Math.PI * 2),
    drift: rand(3, 14)
  }))

  const span = Math.max(w, h)
  blobs = [
    { color: BLUE, x: w * 0.22, y: h * 0.30, r: span * 0.55, ax: w * 0.12, ay: h * 0.10, sx: 0.021, sy: 0.017, alpha: 0.30 },
    { color: GREEN, x: w * 0.78, y: h * 0.70, r: span * 0.42, ax: w * 0.10, ay: h * 0.13, sx: 0.017, sy: 0.023, alpha: 0.18 },
    { color: GOLD, x: w * 0.60, y: h * 0.16, r: span * 0.36, ax: w * 0.15, ay: h * 0.08, sx: 0.013, sy: 0.019, alpha: 0.15 }
  ]

  waves = [
    { y: 0.60, amp: 26, color: BLUE, alpha: 0.16, speed: 0.22, freq: 1.1, lw: 1.4 },
    { y: 0.71, amp: 34, color: GREEN, alpha: 0.12, speed: 0.15, freq: 0.8, lw: 1.2 },
    { y: 0.82, amp: 22, color: GOLD, alpha: 0.11, speed: 0.11, freq: 1.4, lw: 1.1 }
  ]
}

const drawBlobs = (t) => {
  ctx.globalCompositeOperation = 'lighter'
  blobs.forEach((b) => {
    const cx = b.x + Math.sin(t * b.sx * 6.283) * b.ax
    const cy = b.y + Math.cos(t * b.sy * 6.283) * b.ay
    const [r, g, bl] = b.color
    const grad = ctx.createRadialGradient(cx, cy, 0, cx, cy, b.r)
    grad.addColorStop(0, `rgba(${r},${g},${bl},${b.alpha})`)
    grad.addColorStop(0.55, `rgba(${r},${g},${bl},${b.alpha * 0.35})`)
    grad.addColorStop(1, `rgba(${r},${g},${bl},0)`)
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.arc(cx, cy, b.r, 0, Math.PI * 2)
    ctx.fill()
  })
  ctx.globalCompositeOperation = 'source-over'
}

const drawStars = (t) => {
  const span = w + 40
  stars.forEach((s) => {
    const alpha = s.base * (0.55 + 0.45 * Math.sin(t * s.twinkle + s.phase))
    if (alpha <= 0.02) return
    const x = ((s.x + t * s.drift) % span) - 20
    ctx.beginPath()
    ctx.arc(x, s.y, s.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(214,228,255,${alpha.toFixed(3)})`
    ctx.fill()
  })
}

const drawWaves = (t) => {
  waves.forEach((v, i) => {
    ctx.beginPath()
    for (let x = 0; x <= w; x += 14) {
      const p = x / w
      const y = h * v.y
        + Math.sin(p * Math.PI * 2 * v.freq + t * v.speed * 6.283 + i) * v.amp
        + Math.sin(p * Math.PI * 2 * v.freq * 2.3 + t * v.speed * 4) * v.amp * 0.35
      if (x === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    }
    const [r, g, b] = v.color
    ctx.strokeStyle = `rgba(${r},${g},${b},${v.alpha})`
    ctx.lineWidth = v.lw
    ctx.stroke()
  })
}

const drawVignette = () => {
  const grad = ctx.createRadialGradient(
    w * 0.5, h * 0.45, Math.min(w, h) * 0.25,
    w * 0.5, h * 0.5, Math.max(w, h) * 0.8
  )
  grad.addColorStop(0, 'rgba(0,0,0,0)')
  grad.addColorStop(1, 'rgba(0,0,0,0.55)')
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, w, h)
}

const draw = (time) => {
  const t = time / 1000
  const base = ctx.createLinearGradient(0, 0, w * 0.7, h)
  base.addColorStop(0, '#050b1c')
  base.addColorStop(0.45, '#0a1738')
  base.addColorStop(1, '#071026')
  ctx.fillStyle = base
  ctx.fillRect(0, 0, w, h)

  drawBlobs(t)
  drawStars(t)
  drawWaves(t)
  drawVignette()
}

const loop = (time) => {
  draw(time)
  rafId = requestAnimationFrame(loop)
}

const resize = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  if (!rect.width || !rect.height) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  canvas.width = Math.round(rect.width * dpr)
  canvas.height = Math.round(rect.height * dpr)
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  w = rect.width
  h = rect.height
  buildScene()
}

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  ctx = canvas.getContext('2d')
  if (!ctx) return

  resize()
  observer = new ResizeObserver(resize)
  observer.observe(canvas)

  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    draw(0)
  } else {
    rafId = requestAnimationFrame(loop)
  }
})

onBeforeUnmount(() => {
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = 0
  }
  if (observer) {
    observer.disconnect()
    observer = null
  }
  ctx = null
  stars = []
  blobs = []
  waves = []
})
</script>

<style scoped lang="scss">
.flow-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  pointer-events: none;
}
</style>
