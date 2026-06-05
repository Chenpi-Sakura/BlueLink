<template>
  <nav class="bottom-nav">
    <div 
      v-for="item in navItems" 
      :key="item.path"
      class="nav-item"
      :class="{ active: isActive(item.path) }"
      @click="navigateTo(item.path)"
    >
      <component :is="item.icon" />
      <span class="text-[10px] font-medium">{{ item.label }}</span>
    </div>
  </nav>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { h } from 'vue'

const route = useRoute()
const router = useRouter()

const createIcon = (paths) => {
  return {
    render() {
      return h('svg', {
        width: '24',
        height: '24',
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '2',
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round'
      }, paths.map(p => h('path', { d: p })))
    }
  }
}

const navItems = [
  {
    path: '/home',
    label: '首页',
    icon: createIcon([
      'M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z',
      'M9 22V12H15V22'
    ])
  },
  {
    path: '/chat',
    label: '对话',
    icon: createIcon([
      'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z'
    ])
  },
  {
    path: '/graph',
    label: '图谱',
    icon: createIcon([
      'M12 5a3 3 0 1 1 0-6 3 3 0 0 1 0 6z',
      'M5 19a3 3 0 1 1 0-6 3 3 0 0 1 0 6z',
      'M19 19a3 3 0 1 1 0-6 3 3 0 0 1 0 6z',
      'M12 8v8',
      'M8.5 16.5l7 0'
    ])
  },
  {
    path: '/reader',
    label: '阅读',
    icon: createIcon([
      'M4 19.5A2.5 2.5 0 0 1 6.5 17H20',
      'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z'
    ])
  }
]

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
.bottom-nav {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 70px;
  background: rgba(253, 251, 247, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-around;
  align-items: center;
  z-index: 90;
  padding-bottom: 10px;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 12px;
  transition: all 0.2s ease;
  color: #A39F98;
}

.nav-item.active {
  color: #002FA7;
}
</style>
