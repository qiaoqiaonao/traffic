<template>
  <Transition
      appear
      @after-leave="onAfterLeave"
      @before-leave="onBeforeLeave"
  >
    <div
        v-show="visible"
        :class="['lite-toast', `lite-toast--${type}`]"
        @mouseenter="onMouseEnter"
        @mouseleave="onMouseLeave"
        @click="close"
    >
      <!-- 图标 -->
      <div class="lite-toast__icon">
        <svg v-if="type === 'success'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
          <polyline points="22 4 12 14.01 9 11.01"/>
        </svg>
        <svg v-else-if="type === 'error'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <line x1="15" y1="9" x2="9" y2="15"/>
          <line x1="9" y1="9" x2="15" y2="15"/>
        </svg>
        <svg v-else-if="type === 'warning'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
          <line x1="12" y1="9" x2="12" y2="13"/>
          <line x1="12" y1="17" x2="12.01" y2="17"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="16" x2="12" y2="12"/>
          <line x1="12" y1="8" x2="12.01" y2="8"/>
        </svg>
      </div>

      <!-- 内容 -->
      <div class="lite-toast__content">{{ message }}</div>

      <!-- 关闭按钮 -->
      <button class="lite-toast__close" @click.stop="close">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <line x1="18" y1="6" x2="6" y2="18"/>
          <line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
      </button>

      <!-- 底部进度条 -->
      <div class="lite-toast__progress">
        <div class="lite-toast__progress-bar" :style="progressStyle" />
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  id: { type: String, required: true },
  message: { type: String, required: true },
  type: { type: String, default: 'info' },
  duration: { type: Number, default: 3000 },
  onClose: { type: Function, default: null }
})

const emit = defineEmits(['destroy', 'close'])

const visible = ref(false)
const progress = ref(100)
let timer = null
let progressTimer = null
const TICK = 16

const progressStyle = computed(() => ({
  width: `${progress.value}%`
}))

function startTimer() {
  if (props.duration <= 0) return
  const steps = props.duration / TICK
  const decrement = 100 / steps

  progressTimer = setInterval(() => {
    progress.value -= decrement
    if (progress.value <= 0) {
      progress.value = 0
      clearInterval(progressTimer)
    }
  }, TICK)

  timer = setTimeout(() => close(), props.duration)
}

function clearTimer() {
  if (timer) { clearTimeout(timer); timer = null }
  if (progressTimer) { clearInterval(progressTimer); progressTimer = null }
}

function close() {
  clearTimer()
  visible.value = false
}

function onMouseEnter() { clearTimer() }

function onMouseLeave() {
  if (progress.value > 0 && props.duration > 0) {
    const remaining = (progress.value / 100) * props.duration
    timer = setTimeout(() => close(), remaining)
    const steps = remaining / TICK
    const decrement = progress.value / steps
    progressTimer = setInterval(() => {
      progress.value -= decrement
      if (progress.value <= 0) {
        progress.value = 0
        clearInterval(progressTimer)
        progressTimer = null
      }
    }, TICK)
  }
}

function onBeforeLeave() { emit('close') }
function onAfterLeave() { emit('destroy'); props.onClose?.() }

onMounted(() => {
  requestAnimationFrame(() => {
    visible.value = true
    startTimer()
  })
})

onBeforeUnmount(() => { clearTimer() })
</script>

<style scoped>
.lite-toast {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 22px;
  min-width: 300px;
  max-width: 460px;
  border-radius: 14px;

  /* 两端薄荷色，中间白色对称渐变 */
  background: linear-gradient(
      90deg,
      #DEF1EE 0%,
      #eef7f5 12%,
      #ffffff 28%,
      #ffffff 72%,
      #eef7f5 88%,
      #DEF1EE 100%
  );

  backdrop-filter: blur(20px) saturate(1.05);
  -webkit-backdrop-filter: blur(20px) saturate(1.05);

  /* 淡薄荷边框，与两端底色融合 */
  border: 1px solid rgba(200, 230, 220, 0.8);
  box-shadow:
      0 8px 32px rgba(160, 200, 190, 0.2),
      0 2px 6px rgba(0, 0, 0, 0.04);

  cursor: pointer;
  user-select: none;
  overflow: hidden;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease;
  z-index: 9999;
}

.lite-toast:hover {
  transform: translateY(-2px) scale(1.01);
  box-shadow:
      0 12px 40px rgba(140, 190, 180, 0.26),
      0 4px 8px rgba(0, 0, 0, 0.06);
}

/* 类型色 - 左侧竖条 */
.lite-toast--success { border-left: 4px solid #34d399; }
.lite-toast--error   { border-left: 4px solid #f87171; }
.lite-toast--warning { border-left: 4px solid #fbbf24; }
.lite-toast--info    { border-left: 4px solid #60a5fa; }

/* 图标 */
.lite-toast__icon {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
}
.lite-toast__icon svg {
  width: 100%;
  height: 100%;
}
.lite-toast--success .lite-toast__icon { color: #059669; }
.lite-toast--error   .lite-toast__icon { color: #dc2626; }
.lite-toast--warning .lite-toast__icon { color: #d97706; }
.lite-toast--info    .lite-toast__icon { color: #2563eb; }

/* 内容 - 位于中间白色区域 */
.lite-toast__content {
  flex: 1;
  font-size: 15px;
  color: #1e293b;
  line-height: 1.6;
  word-break: break-word;
  font-weight: 500;
  padding: 0 4px; /* 轻微内边距确保不贴边 */
}

/* 关闭按钮 */
.lite-toast__close {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}
.lite-toast__close:hover {
  background: rgba(222, 241, 238, 0.7);
  color: #64748b;
}
.lite-toast__close svg {
  width: 14px;
  height: 14px;
}

/* 进度条 */
.lite-toast__progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: rgba(0, 0, 0, 0.04);
}
.lite-toast__progress-bar {
  height: 100%;
  transition: width 16ms linear;
  border-radius: 0 0 0 3px;
}
.lite-toast--success .lite-toast__progress-bar { background: #34d399; }
.lite-toast--error   .lite-toast__progress-bar { background: #f87171; }
.lite-toast--warning .lite-toast__progress-bar { background: #fbbf24; }
.lite-toast--info    .lite-toast__progress-bar { background: #60a5fa; }

/* 动画 */
.v-enter-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.v-leave-active {
  transition: all 0.3s cubic-bezier(0.7, 0, 0.84, 0);
}
.v-enter-from {
  opacity: 0;
  transform: translateY(-16px) scale(0.92);
}
.v-leave-to {
  opacity: 0;
  transform: translateY(-12px) scale(0.96);
}
</style>