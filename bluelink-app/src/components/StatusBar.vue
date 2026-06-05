<template>
  <div class="status-bar">
    <span class="time font-semibold text-ink-900">{{ currentTime }}</span>
    <div class="right-icons flex items-center gap-1">
      <svg width="16" height="14" viewBox="0 0 16 14" fill="#333">
        <rect x="0" y="8" width="3" height="6" rx="1"/>
        <rect x="4" y="5" width="3" height="9" rx="1"/>
        <rect x="8" y="2" width="3" height="12" rx="1"/>
        <rect x="12" y="0" width="3" height="14" rx="1"/>
      </svg>
      <svg width="16" height="12" viewBox="0 0 24 18" fill="none" stroke="#333" stroke-width="1.5">
        <path d="M2 8 L8 8 L12 4 L16 8 L22 8"/>
        <circle cx="12" cy="14" r="2" fill="#333" stroke="none"/>
      </svg>
      <div class="flex items-center ml-1">
        <div class="w-5 h-2.5 border border-gray-700 rounded-sm relative">
          <div class="absolute inset-0.5 bg-gray-700 rounded-sm" style="width: 70%"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const currentTime = ref('9:41')

let timer = null

const updateTime = () => {
  const now = new Date()
  const hours = now.getHours().toString().padStart(2, '0')
  const minutes = now.getMinutes().toString().padStart(2, '0')
  currentTime.value = `${hours}:${minutes}`
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.status-bar { 
  height: 48px; 
  padding: 0 24px; 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  font-size: 14px; 
  font-family: -apple-system, BlinkMacSystemFont, sans-serif; 
  position: absolute; 
  top: 0; 
  left: 0; 
  right: 0; 
  z-index: 100; 
}
</style>
