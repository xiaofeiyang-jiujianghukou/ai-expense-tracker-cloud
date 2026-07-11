import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '../utils/auth'
import { login as loginApi, register as registerApi, type LoginRequest, type RegisterRequest, type UserVO } from '../api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const user = ref<UserVO | null>(null)

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    token.value = res.data.token
    user.value = res.data.user
    setToken(res.data.token)
  }

  async function register(data: RegisterRequest) {
    await registerApi(data)
  }

  function logout() {
    token.value = null
    user.value = null
    removeToken()
  }

  return { token, user, login, register, logout }
})
