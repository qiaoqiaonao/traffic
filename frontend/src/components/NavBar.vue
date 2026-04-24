<template>
  <nav class="navbar" :class="{ 'scrolled': isScrolled }">
    <div class="nav-container">
      <router-link to="/" class="logo">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 17a2 2 0 11-4 0 2 2 0 014 0zM19 17a2 2 0 11-4 0 2 2 0 014 0z"/>
          <path d="M13 16V6a1 1 0 00-1-1H4a1 1 0 00-1 1v10a1 1 0 001 1h1m8-1a1 1 0 01-1 1H9m4-1V8a1 1 0 011-1h2.586a1 1 0 01.707.293l3.414 3.414a1 1 0 01.293.707V16a1 1 0 01-1 1h-1m-6-1a1 1 0 001 1h1M5 17a2 2 0 104 0m-4 0a2 2 0 114 0m6 0a2 2 0 104 0m-4 0a2 2 0 114 0"/>
        </svg>
        <span>TrafficAI</span>
      </router-link>

      <div class="nav-links">
        <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            :class="['nav-link', { active: $route.path === item.path }]"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.name }}</span>
        </router-link>
      </div>

      <div class="nav-actions">
        <el-button
            type="primary"
            class="cta-btn"
            @click="$router.push('/analyze')"
        >
          <el-icon><Plus /></el-icon>
          新建分析
        </el-button>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const isScrolled = ref(false)

const navItems = [
  { path: '/', name: '首页', icon: 'HomeFilled' },
  { path: '/analyze', name: '视频分析', icon: 'VideoCamera' },
  { path: '/image-detect', name: '图片检测', icon: 'PictureFilled' },
  { path: '/history', name: '历史记录', icon: 'Clock' },
  { path: '/dashboard', name: '仪表盘', icon: 'DataAnalysis' }
]

const handleScroll = () => {
  isScrolled.value = window.scrollY > 20
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 70px;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid transparent;
  transition: all 0.3s;
  z-index: 1000;
}

.navbar.scrolled {
  border-color: var(--border);
  background: rgba(15, 23, 42, 0.95);
}

.nav-container {
  max-width: 1400px;
  height: 100%;
  margin: 0 auto;
  padding: 0 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
  font-weight: 700;
  text-decoration: none;
  color: var(--text-primary);
}

.logo svg {
  width: 32px;
  height: 32px;
  stroke: var(--primary);
}

.logo span {
  background: var(--gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.nav-links {
  display: flex;
  gap: 8px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  color: var(--text-secondary);
  text-decoration: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
  position: relative;
}

.nav-link:hover {
  color: var(--text-primary);
  background: rgba(99, 102, 241, 0.1);
}

.nav-link.active {
  color: var(--primary);
  background: rgba(99, 102, 241, 0.15);
}

.cta-btn {
  background: var(--gradient);
  border: none;
  padding: 10px 20px;
  font-weight: 600;
  border-radius: 10px;
}

@media (max-width: 768px) {
  .nav-links {
    display: none;
  }

  .nav-container {
    padding: 0 20px;
  }
}
</style>