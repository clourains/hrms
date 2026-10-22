<template>
  <div class="landing">
    <FlowBackground />

    <div class="landing-inner">
      <div class="brand-plate">
        <img :src="logo" alt="星跃出海国际贸易有限公司" />
      </div>

      <h1 class="company-name">星跃出海国际贸易有限公司</h1>
      <p class="system-name">人力资源管理系统 · 业务系统</p>
      <p class="slogan">国货出海 · 星跃未来</p>

      <el-button class="enter-btn" size="large" @click="enterSystem">
        进入系统
        <el-icon class="enter-icon"><Right /></el-icon>
      </el-button>
    </div>

    <div class="landing-footer">© 2026 星跃出海国际贸易有限公司 · STARLEAP GLOBAL</div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import FlowBackground from '@/components/FlowBackground.vue'
import logo from '@/assets/logo.png'

const router = useRouter()

const enterSystem = () => {
  router.push('/login')
}
</script>

<style scoped lang="scss">
.landing {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #050b1c;
}

.landing-inner {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  text-align: center;
}

.brand-plate {
  width: 200px;
  padding: 16px 18px 12px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.45), 0 0 60px rgba(217, 166, 60, 0.18);
  animation: rise 0.9s cubic-bezier(0.22, 1, 0.36, 1) both;

  img {
    display: block;
    width: 100%;
  }
}

.company-name {
  margin: 34px 0 0;
  font-size: 34px;
  font-weight: 600;
  letter-spacing: 4px;
  color: #fff;
  text-shadow: 0 2px 20px rgba(4, 12, 34, 0.8);
  animation: rise 0.9s 0.12s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.system-name {
  margin: 14px 0 0;
  font-size: 17px;
  letter-spacing: 6px;
  color: #e0bc6a;
  animation: rise 0.9s 0.22s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.slogan {
  margin: 10px 0 0;
  font-size: 13px;
  letter-spacing: 8px;
  color: rgba(255, 255, 255, 0.45);
  animation: rise 0.9s 0.3s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.enter-btn {
  margin-top: 40px;
  height: 48px;
  padding: 0 40px;
  font-size: 16px;
  letter-spacing: 6px;
  border: none;
  color: #fff;
  background: linear-gradient(135deg, #1e3f8f 0%, #10245a 100%);
  box-shadow: 0 12px 32px rgba(16, 36, 90, 0.6), inset 0 0 0 1px rgba(217, 166, 60, 0.5);
  transition: transform 0.25s, box-shadow 0.25s;
  animation: rise 0.9s 0.38s cubic-bezier(0.22, 1, 0.36, 1) both;

  &:hover,
  &:focus {
    color: #fff;
    transform: translateY(-2px);
    background: linear-gradient(135deg, #2450ad 0%, #132a68 100%);
    box-shadow: 0 16px 38px rgba(16, 36, 90, 0.7), inset 0 0 0 1px rgba(217, 166, 60, 0.8);
  }
}

.enter-icon {
  margin-left: 6px;
}

.landing-footer {
  position: absolute;
  z-index: 1;
  bottom: 22px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.3);
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
