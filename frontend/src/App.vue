<template>
  <div id="app">
    <NavBar v-if="!isFullscreen" />
    <main :class="{ 'fullscreen': isFullscreen, 'with-nav': !isFullscreen }">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from '@/components/NavBar.vue'

const route = useRoute()
const isFullscreen = computed(() => route.meta?.fullscreen)
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

:root {
  --primary: #6366f1;
  --primary-dark: #4f46e5;
  --secondary: #8b5cf6;
  --accent: #06b6d4;
  --success: #10b981;
  --warning: #f59e0b;
  --danger: #ef4444;
  --bg-dark: #f5f7fa;
  --bg-card: #ffffff;
  --bg-hover: #e8ecf1;
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --border: rgba(0, 0, 0, 0.08);
  --gradient: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%);
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: var(--bg-dark);
  color: var(--text-primary);
  line-height: 1.6;
  overflow-x: hidden;
}

#app {
  min-height: 100vh;
}

.with-nav {
  margin-top: 70px;
  min-height: calc(100vh - 70px);
}

.fullscreen {
  min-height: 100vh;
}

/* 页面过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* Element Plus 浅色主题覆盖 */
.el-button--primary {
  background: var(--gradient);
  border: none;
}

.el-button--primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.el-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--text-primary);
}

.el-input__wrapper,
.el-textarea__inner {
  background: #ffffff !important;
  box-shadow: 0 0 0 1px var(--border) inset !important;
}

.el-input__inner,
.el-textarea__inner {
  color: #1e293b !important;
}

.el-slider__runway {
  background: #e2e8f0;
}

.el-slider__bar {
  background: var(--gradient);
}

.el-slider__button {
  border-color: var(--primary);
}

.el-progress-bar__outer {
  background: #e2e8f0;
}

.el-tag {
  border: 1px solid rgba(0,0,0,0.06);
}

.el-radio-button__inner {
  background: #f5f7fa;
  border-color: var(--border);
  color: var(--text-secondary);
}

.el-radio-button__original-radio:checked + .el-radio-button__inner {
  background: var(--gradient);
  border-color: transparent;
  color: #fff;
  box-shadow: none;
}

.el-table {
  --el-table-bg-color: #fff;
  --el-table-tr-bg-color: #fff;
  --el-table-header-bg-color: #f5f7fa;
  --el-table-row-hover-bg-color: #f0f2f5;
}

.el-pagination .el-pager li {
  background: #fff;
}
</style>