<template>
  <div class="login-container">
    <div class="login-left">
      <FlowBackground />
      <div class="brand-info">
        <div class="brand-plate">
          <img :src="logo" alt="星跃出海国际贸易有限公司" />
        </div>
        <h1 class="brand-title">星跃出海国际贸易有限公司</h1>
        <p class="brand-subtitle">人力资源管理系统 · 业务系统</p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon :size="24"><User /></el-icon>
            <span>员工管理</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24"><Calendar /></el-icon>
            <span>考勤管理</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24"><Money /></el-icon>
            <span>薪资管理</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24"><DataAnalysis /></el-icon>
            <span>智能分析</span>
          </div>
        </div>
      </div>
    </div>
    <div class="login-right">
      <div class="login-form-wrapper">
        <h2 class="login-title">欢迎登录</h2>
        <p class="login-desc">请输入您的账号信息</p>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="captcha" v-if="showCaptcha">
            <div class="captcha-wrapper">
              <el-input
                v-model="loginForm.captcha"
                placeholder="验证码"
                size="large"
                style="flex: 1"
              />
              <div class="captcha-img" @click="refreshCaptcha">
                <img :src="captchaImg" alt="验证码" />
              </div>
            </div>
          </el-form-item>

          <el-form-item>
            <div class="login-options">
              <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
              <a href="#" class="forgot-password">忘记密码?</a>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <p>还没有账号? <a href="#">立即注册</a></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import FlowBackground from '@/components/FlowBackground.vue'
import logo from '@/assets/logo.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)
const showCaptcha = ref(false)
const captchaImg = ref('')

const loginForm = reactive({
  username: 'admin',
  password: '123456',
  captcha: '',
  rememberMe: false
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await userStore.login({
        username: loginForm.username,
        password: loginForm.password
      })
      if (res.code === 200) {
        ElMessage.success('登录成功')
        const redirect = route.query.redirect || '/home'
        router.push(redirect)
      } else {
        ElMessage.error(res.message || '用户名或密码错误')
      }
    } catch (error) {
      console.error('登录失败:', error)
      ElMessage.error(error.message || '登录失败，请检查用户名和密码')
    } finally {
      loading.value = false
    }
  })
}

const refreshCaptcha = () => {
  // 刷新验证码
  captchaImg.value = `/api/auth/captcha?t=${Date.now()}`
}
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

.login-left {
  flex: 1;
  background: #050b1c;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.brand-info {
  color: #fff;
  text-align: center;
  position: relative;
  z-index: 1;
}

.brand-plate {
  width: 140px;
  margin: 0 auto 28px;
  padding: 12px 14px 9px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.45), 0 0 46px rgba(217, 166, 60, 0.16);

  img {
    display: block;
    width: 100%;
  }
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 14px;
  letter-spacing: 3px;
}

.brand-subtitle {
  font-size: 15px;
  color: #e0bc6a;
  margin-bottom: 0;
  letter-spacing: 4px;
}

.brand-features {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 28px;
  max-width: 400px;
  margin: 42px auto 0;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  opacity: 0.95;

  .el-icon {
    font-size: 36px;
  }
}

.login-right {
  width: 500px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-form-wrapper {
  width: 380px;
}

.login-title {
  font-size: 32px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.login-desc {
  font-size: 14px;
  color: #909399;
  margin-bottom: 40px;
}

.login-form {
  .el-form-item {
    margin-bottom: 24px;
  }
}

.captcha-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #dcdfe6;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.forgot-password {
  color: #409EFF;
  font-size: 14px;
  text-decoration: none;

  &:hover {
    color: #66b1ff;
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  letter-spacing: 4px;
  margin-top: 8px;
}

.login-footer {
  text-align: center;
  margin-top: 30px;
  font-size: 14px;
  color: #909399;

  a {
    color: #409EFF;
    text-decoration: none;

    &:hover {
      color: #66b1ff;
    }
  }
}
</style>
