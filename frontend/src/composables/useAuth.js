import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { login as loginApi, register as registerApi, logoutApi } from '@/api'
import { toast } from '@/utils/ui'

const TOKEN_KEY = 'token'
const USERNAME_KEY = 'username'
const NICKNAME_KEY = 'nickname'

const token = ref(localStorage.getItem(TOKEN_KEY) || '')
const username = ref(localStorage.getItem(USERNAME_KEY) || '')
const nickname = ref(localStorage.getItem(NICKNAME_KEY) || '')

const isLoggedIn = computed(() => !!token.value)

function setAuth(t, u, n) {
  token.value = t
  username.value = u
  nickname.value = n
  localStorage.setItem(TOKEN_KEY, t)
  localStorage.setItem(USERNAME_KEY, u)
  localStorage.setItem(NICKNAME_KEY, n)
}

export function clearAuth() {
  token.value = ''
  username.value = ''
  nickname.value = ''
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
  localStorage.removeItem(NICKNAME_KEY)
}

export function getToken() {
  return token.value || localStorage.getItem(TOKEN_KEY) || ''
}

export function useAuth() {
  const router = useRouter()

  async function login(usernameVal, password) {
    const res = await loginApi(usernameVal, password)
    if (res.data.code === 200) {
      const { token: t, username: u, nickname: n } = res.data.data
      setAuth(t, u, n)
      toast.success(`欢迎回来，${n || u}`)
      return res
    }
    throw new Error(res.data.message || '登录失败')
  }

  async function register(usernameVal, password, nicknameVal) {
    const res = await registerApi(usernameVal, password, nicknameVal)
    if (res.data.code === 200) {
      const { token: t, username: u, nickname: n } = res.data.data
      setAuth(t, u, n)
      toast.success('注册成功！欢迎使用 TrafficAI')
      return res
    }
    throw new Error(res.data.message || '注册失败')
  }

  async function logout() {
    try { await logoutApi() } catch (e) { /* ignore */ }
    clearAuth()
    toast.success('已退出登录')
    router.push('/login')
  }

  return {
    token: computed(() => token.value),
    username: computed(() => username.value),
    nickname: computed(() => nickname.value),
    isLoggedIn,
    login,
    register,
    logout
  }
}
