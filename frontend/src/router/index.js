import { createRouter, createWebHistory } from 'vue-router'
import { useAnalysisStore } from '@/stores/analysis'

const routes = [
    {
        path: '/',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' }
    },
    {
        path: '/analyze',
        name: 'Analyze',
        component: () => import('@/views/Analyze.vue'),
        meta: { title: '视频分析' }
    },
    {
        path: '/image-detect',
        name: 'ImageDetect',
        component: () => import('@/views/ImageDetect.vue'),
        meta: { title: '图片检测' }
    },
    {
        path: '/history',
        name: 'History',
        component: () => import('@/views/History.vue'),
        meta: { title: '历史记录' }
    },
    {
        path: '/result/:taskId',
        name: 'Result',
        component: () => import('@/views/Result.vue'),
        meta: { title: '分析结果' },
        props: true
    },
    {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据仪表盘' }
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

    next()
})

export default router