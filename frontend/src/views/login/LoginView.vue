<template>
  <div class="login-page">
    <div class="login-card">
      <h1>AI Expense Tracker</h1>
      <p class="subtitle">智能个人财务管理系统</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width:100%">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <p class="register-link">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  email: '',
  password: ''
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}
.login-card {
  width: 360px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  border-radius: 8px;
}
h1 {
  text-align: center;
  font-size: 22px;
  margin-bottom: 4px;
}
.subtitle {
  text-align: center;
  color: #999;
  margin-bottom: 24px;
}
.register-link {
  text-align: center;
  color: #999;
  font-size: 13px;
}
</style>
