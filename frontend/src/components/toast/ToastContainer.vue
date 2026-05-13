<template>
  <Teleport to="body">
    <div class="lite-toast-container">
      <Toast
          v-for="item in toasts"
          :key="item.id"
          :id="item.id"
          :message="item.message"
          :type="item.type"
          :duration="item.duration"
          @destroy="remove(item.id)"
      />
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import Toast from './Toast.vue'

const toasts = ref([])
let seed = 0

function add(options) {
  const id = `toast_${Date.now()}_${seed++}`
  toasts.value.push({ id, ...options })
  return id
}

function remove(id) {
  const index = toasts.value.findIndex(t => t.id === id)
  if (index > -1) toasts.value.splice(index, 1)
}

function clear() {
  toasts.value = []
}

defineExpose({ add, remove, clear })
</script>

<style scoped>
/* 中上方定位 */
.lite-toast-container {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  pointer-events: none;
}

.lite-toast-container > * {
  pointer-events: auto;
}

/* 小屏幕适配 */
@media (max-width: 640px) {
  .lite-toast-container {
    top: 12px;
    left: 12px;
    right: 12px;
    transform: none;
  }
}
</style>