import { createRouter, createWebHistory } from 'vue-router'
import { useAnalysisStore } from '@/stores/analysis'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
        meta: { title: '登录', guest: true, fullscreen: true }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('@/views/Register.vue'),
        meta: { title: '注册', guest: true, fullscreen: true }
    },
    {
        path: '/',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', requiresAuth: true }
    },
    {
        path: '/analyze',
        name: 'Analyze',
        component: () => import('@/views/Analyze.vue'),
        meta: { title: '视频分析', requiresAuth: true }
    },
    {
        path: '/image-detect',
        name: 'ImageDetect',
        component: () => import('@/views/ImageDetect.vue'),
        meta: { title: '图片检测', requiresAuth: true }
    },
    {
        path: '/history',
        name: 'History',
        component: () => import('@/views/History.vue'),
        meta: { title: '历史记录', requiresAuth: true }
    },
    {
        path: '/result/:taskId',
        name: 'Result',
        component: () => import('@/views/Result.vue'),
        meta: { title: '分析结果', requiresAuth: true },
        props: true
    },
    {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据仪表盘', requiresAuth: true }
    },
    {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        redirect: '/'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes,
    scrollBehavior() {
        return { top: 0 }
    }
})

// 路由守卫
router.beforeEach((to, from, next) => {
    // 设置页面标题
    document.title = to.meta.title ? `${to.meta.title} - TrafficAI` : 'TrafficAI'

    // 清理之前的WebSocket连接
    const store = useAnalysisStore()
    if (store.ws) {
        store.closeWebSocket()
    }

    const token = localStorage.getItem('token')

    // 需要认证的页面，检查是否登录
    if (to.meta.requiresAuth && !token) {
        next({ path: '/login', query: { redirect: to.fullPath } })
        return
    }

    // 已登录用户访问登录/注册页，跳回首页
    if (to.meta.guest && token) {
        next('/')
        return
    }

    next()
})

export default router
