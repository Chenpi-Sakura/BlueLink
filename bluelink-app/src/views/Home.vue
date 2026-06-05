<template>
  <div class="page-container relative flex flex-col h-full overflow-hidden">
    <div class="absolute top-[-5%] right-[-10%] w-[60%] h-[30%] bg-brand-accent/5 rounded-full blur-[60px] pointer-events-none z-0"></div>
    
    <header class="absolute top-0 w-full z-30 glass-panel pt-12 pb-3 px-4 border-b border-parchment-200/40">
      <div class="flex items-center gap-3">
        <button class="p-2 -ml-2 text-ink-900 press-effect">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 6H20M4 12H20M4 18H14"/>
          </svg>
        </button>
        <div class="flex-1 text-ink-400 font-sans text-[15px]">
          搜索或向 AI 提问...
        </div>
        <div class="flex items-center gap-1.5">
          <button class="flex items-center gap-1 px-3 py-1.5 bg-brand-accent-bg text-brand-accent rounded-full font-sans text-xs font-semibold press-effect">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 4L12 20M12 4L6 10M12 4L18 10"/>
            </svg>
            <span>升级</span>
          </button>
          <button class="p-1.5 text-ink-600 press-effect">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round">
              <path d="M4 6H20M4 10H20V19C20 20.1046 19.1046 21 18 21H6C4.89543 21 4 20.1046 4 19V10H4Z"/>
              <path d="M9 14H15"/>
            </svg>
          </button>
          <div class="w-8 h-8 rounded-xl bg-parchment-200 overflow-hidden ml-1 flex-shrink-0 border border-white flex items-center justify-center">
            <span class="text-ink-600 text-xs font-semibold">AI</span>
          </div>
        </div>
      </div>
    </header>

    <main 
      ref="scrollContainer"
      class="flex-1 overflow-y-auto hide-scrollbar pt-28 relative z-10"
      @scroll="handleScroll"
    >
      <div class="px-5 pb-6">
        <h1 class="font-serif text-3xl font-bold text-ink-900 tracking-wide">{{ greeting }}</h1>
      </div>

      <div class="masonry-grid font-serif text-ink-900">
        <DocumentCard 
          v-for="(doc, index) in documents" 
          :key="doc.id"
          :doc="doc"
          :index="index"
        />
        <div class="h-24"></div>
      </div>
    </main>

    <div class="absolute bottom-20 left-0 w-full px-5 z-40">
      <div class="glass-pill rounded-full p-2 pl-4 flex items-center justify-between shadow-float">
        <button class="p-2 text-ink-600 hover:text-brand-accent transition-colors press-effect" @click="goToChat">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
        </button>
        <div class="mx-2">
          <div class="relative w-12 h-12 bg-parchment-100 rounded-full flex items-center justify-center shadow-sm border border-parchment-200 text-ink-400">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M12 2V14C12 15.6569 10.6569 17 9 17H15C13.3431 17 12 15.6569 12 14V2Z" fill="currentColor" opacity="0.3"/>
              <rect x="9" y="3" width="6" height="12" rx="3" fill="currentColor" opacity="0.3"/>
              <path d="M5 11V12C5 15.866 8.13401 19 12 19V19C15.866 19 19 15.866 19 12V11"/>
              <path d="M12 19V22"/>
              <path d="M9 22H15"/>
            </svg>
          </div>
        </div>
        <button class="p-2 text-ink-600 hover:text-brand-accent transition-colors press-effect">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="4"/>
            <path d="M12 8V16M8 12H16"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { documents } from '@/data/mockData'
import DocumentCard from '@/components/DocumentCard.vue'

const router = useRouter()
const scrollContainer = ref(null)
const headerRef = ref(null)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const handleScroll = () => {
  const header = document.querySelector('#page-home header') || document.querySelector('header')
  if (scrollContainer.value && header) {
    header.style.boxShadow = scrollContainer.value.scrollTop > 10 
      ? '0 4px 20px -2px rgba(44, 43, 41, 0.05)' 
      : 'none'
  }
}

const goToChat = () => {
  router.push('/chat')
}
</script>

<style scoped>
.page-container {
  height: 100%;
  width: 100%;
}
</style>
