<template>
  <div class="page-container relative flex flex-col h-full overflow-hidden">
    <header class="absolute top-0 w-full z-30 glass-panel pt-12 pb-3 px-4 border-b border-parchment-200/40">
      <div class="flex items-center gap-3">
        <button class="p-2 -ml-2 text-ink-900 press-effect" @click="goBack">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
        </button>
        <div class="flex-1">
          <p class="text-[11px] text-brand-accent font-semibold tracking-wider">溯源阅读</p>
          <h2 class="font-serif font-bold text-ink-900 text-sm">{{ documentTitle }}</h2>
        </div>
        <button class="p-2 text-ink-600 press-effect" @click="goToGraph">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="5" r="3"/>
            <circle cx="5" cy="19" r="3"/>
            <circle cx="19" cy="19" r="3"/>
            <path d="M12 8v8M8.5 16.5l3.5-2 3.5 2"/>
          </svg>
        </button>
      </div>
    </header>

    <main 
      ref="readerContainer"
      class="flex-1 overflow-y-auto hide-scrollbar pt-24 pb-8 px-5"
      @click="handleReaderClick"
    >
      <h1 class="font-serif text-2xl font-bold text-ink-900 mb-6">{{ content.title }}</h1>
      
      <template v-for="(p, index) in content.paragraphs" :key="index">
        <div v-if="p.type === 'highlight'" class="spotlight-target" :id="'spotlight-' + index">
          <span class="text-brand-accent text-[10px] font-sans font-semibold tracking-wide">溯源锚点：{{ p.anchor }}</span>
          <div class="mt-2">
            <strong class="text-ink-900">{{ p.text }}</strong>
          </div>
        </div>
        
        <div v-else-if="p.type === 'fold'">
          <div class="fold-strip" @click.stop="toggleFold(index)">
            {{ p.label }} <span class="arrow inline-block ml-1" :style="{ transform: foldedStates[index] ? 'rotate(180deg)' : 'rotate(0)' }">▼</span>
          </div>
          <div class="folded-content" :class="{ expanded: foldedStates[index] }">
            <p class="text-[15px] text-ink-600 leading-relaxed font-serif fade-paragraph">{{ p.content }}</p>
          </div>
        </div>
        
        <p 
          v-else 
          class="text-[15px] text-ink-600 leading-relaxed font-serif mb-5"
          :class="['fade-paragraph', { 'full-opacity': !p.faded || spotlightCleared }]"
        >
          {{ p.text }}
        </p>
      </template>
      
      <div class="h-20"></div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { readerContent, documents } from '@/data/mockData'

const router = useRouter()
const route = useRoute()
const readerContainer = ref(null)
const spotlightCleared = ref(false)
const foldedStates = reactive({})

const documentId = computed(() => route.params.id)
const documentTitle = computed(() => {
  if (documentId.value) {
    const doc = documents.find(d => d.id === parseInt(documentId.value))
    return doc ? doc.title : content.value.title
  }
  return content.value.title
})

const content = computed(() => readerContent)

const goBack = () => {
  router.push('/chat')
}

const goToGraph = () => {
  router.push('/graph')
}

const toggleFold = (index) => {
  foldedStates[index] = !foldedStates[index]
}

const handleReaderClick = (e) => {
  if (!e.target.closest('.fold-strip') && !e.target.closest('.spotlight-target')) {
    clearSpotlight()
  }
}

const clearSpotlight = () => {
  spotlightCleared.value = true
  const highlights = document.querySelectorAll('.spotlight-target')
  highlights.forEach(h => {
    h.style.background = 'transparent'
    h.style.borderLeft = 'none'
    h.style.padding = '0'
    const label = h.querySelector('span')
    if (label) label.style.display = 'none'
  })
}
</script>

<style scoped>
.page-container {
  height: 100%;
  width: 100%;
}
</style>
