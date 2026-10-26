<template>
  <teleport to="body">
    <transition name="lock-fade">
      <div v-if="appStore.locked" class="lock-screen">
        <div class="lock-time">{{ timeText }}</div>
        <div class="lock-date">{{ dateText }}</div>

        <div class="lock-card">
          <el-avatar :size="64" class="lock-avatar">
            {{ userStore.username ? userStore.username.charAt(0).toUpperCase() : 'U' }}
          </el-avatar>
          <div class="lock-user">{{ userStore.userInfo.realName || userStore.username }}</div>
          <div class="lock-hint">屏幕已锁定，请输入密码解锁</div>

          <el-input
            v-model="password"
            type="password"
            size="large"
            placeholder="请输入登录密码"
            show-password
            class="lock-input"
            @keyup.enter="handleUnlock"
          />
          <el-button
            type="primary"
            size="large"
            class="lock-btn"
            :loading="loading"
            @click="handleUnlock"
          >
            解 锁
          </el-button>
          <el-button link class="lock-relogin" @click="handleRelogin">使用其他账号登录</el-button>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import { loginApi } from '@/api/auth'

const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const password = ref('')
const loading = ref(false)

// 锁屏时钟
const now = ref(new Date())
let timer = null
const timeText = computed(() => {
  const d = now.value
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
})
const dateText = computed(() => {
  const d = now.value
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week}`
})

const handleUnlock = async () => {
  if (!password.value) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    // 复用登录接口校验密码（登录不依赖验证码）
    const res = await loginApi({
      username: userStore.username,
      password: password.value
    })
    if (res.code === 200) {
      appStore.unlock()
      password.value = ''
      ElMessage.success('解锁成功')
    } else {
      ElMessage.error(res.message || '密码错误')
    }
  } catch (e) {
    ElMessage.error('密码错误')
  } finally {
    loading.value = false
  }
}

const handleRelogin = async () => {
  appStore.unlock()
  await userStore.logout()
  router.push('/login')
}

onMounted(() => {
  timer = setInterval(() => {
    now.value = new Date()
  }, 1000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style scoped lang="scss">
.lock-screen {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(1200px 600px at 20% 10%, rgba(64, 158, 255, 0.18), transparent 60%),
    radial-gradient(900px 500px at 80% 90%, rgba(124, 58, 237, 0.16), transparent 60%),
    #0d1017;
  backdrop-filter: blur(8px);
}

.lock-time {
  font-size: 72px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 4px;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}

.lock-date {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.65);
  margin: 10px 0 40px;
}

.lock-card {
  width: 320px;
  padding: 28px 28px 22px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(12px);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.lock-avatar {
  background: var(--el-color-primary);
  color: #fff;
  font-size: 26px;
}

.lock-user {
  margin-top: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.lock-hint {
  margin: 6px 0 18px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
}

.lock-input {
  width: 100%;
}

.lock-btn {
  width: 100%;
  margin-top: 14px;
}

.lock-relogin {
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.6);

  &:hover {
    color: #fff;
  }
}

.lock-fade-enter-active,
.lock-fade-leave-active {
  transition: opacity 0.2s ease;
}

.lock-fade-enter-from,
.lock-fade-leave-to {
  opacity: 0;
}
</style>
