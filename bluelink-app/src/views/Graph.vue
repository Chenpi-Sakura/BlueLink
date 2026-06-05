<template>
  <div class="page-container relative flex flex-col h-full overflow-hidden">
    <header class="absolute top-0 w-full z-30 glass-panel pt-12 pb-3 px-4 border-b border-parchment-200/40">
      <div class="flex items-center gap-3">
        <button class="p-2 -ml-2 text-ink-900 press-effect" @click="goBack">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
        </button>
        <div class="flex-1 text-center">
          <p class="text-[11px] text-brand-accent font-semibold tracking-widest">KNOWLEDGE GRAPH</p>
          <h2 class="font-serif font-bold text-ink-900 text-sm">知识星图</h2>
        </div>
        <button class="p-2 text-ink-600 press-effect">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="11" cy="11" r="8"/>
            <path d="M21 21l-4.35-4.35"/>
          </svg>
        </button>
      </div>
    </header>

    <main 
      ref="graphCanvas"
      class="flex-1 overflow-hidden relative"
    >
      <div 
        v-for="conn in graphData.connections" 
        :key="conn.join('-')"
        class="connector"
        :class="{ active: activeNode && (conn[0] === activeNode || conn[1] === activeNode) }"
        :style="getLineStyle(conn)"
      ></div>
      
      <div 
        v-for="node in graphData.nodes" 
        :key="node.id"
        class="knowledge-node"
        :class="{ focused: activeNode === node.id }"
        :style="{ left: node.x + 'px', top: (node.y + 60) + 'px' }"
        @click="focusNode(node.id)"
      >
        {{ node.label }}
      </div>
    </main>

    <div 
      class="absolute bottom-0 left-0 right-0 h-44 bg-white rounded-t-[30px] shadow-lg z-40 transition-transform duration-400 ease-out"
      :style="{ transform: panelVisible ? 'translateY(0)' : 'translateY(100%)' }"
    >
      <div class="w-12 h-1 bg-ink-400/30 rounded-full mx-auto mt-3 mb-4"></div>
      <div class="px-6">
        <p class="text-[11px] text-brand-accent font-semibold tracking-wider mb-2">{{ panelData.tag }}</p>
        <h3 class="font-serif font-bold text-lg text-ink-900 mb-3">{{ panelData.title }}</h3>
        <p class="text-sm text-ink-600 leading-relaxed mb-4">{{ panelData.desc }}</p>
        <button class="w-full py-3 bg-brand-accent text-white rounded-2xl font-semibold text-sm press-effect" @click="goToReader">
          查看溯源原文 →
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { knowledgeGraph } from '@/data/mockData'

const router = useRouter()
const graphCanvas = ref(null)
const activeNode = ref(null)
const panelVisible = ref(false)

const graphData = knowledgeGraph
const panelData = reactive({
  tag: '溯源线索',
  title: '选择一个知识点',
  desc: '点击图谱中的节点，查看相关文献与逻辑引力线。'
})

const goBack = () => {
  router.push('/home')
}

const goToReader = () => {
  router.push('/reader')
}

const getLineStyle = (conn) => {
  const fromNode = graphData.nodes.find(n => n.id === conn[0])
  const toNode = graphData.nodes.find(n => n.id === conn[1])
  
  const x1 = fromNode.x + 26
  const y1 = fromNode.y + 26 + 60
  const x2 = toNode.x + 26
  const y2 = toNode.y + 26 + 60
  
  const length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2))
  const angle = Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI
  
  return {
    left: x1 + 'px',
    top: y1 + 'px',
    width: length + 'px',
    transform: `rotate(${angle}deg)`
  }
}

const focusNode = (nodeId) => {
  activeNode.value = nodeId
  panelVisible.value = true
  
  const info = graphData.nodeInfo[nodeId]
  if (info) {
    panelData.title = info.title
    panelData.desc = info.desc
  }
}
</script>

<style scoped>
.page-container {
  height: 100%;
  width: 100%;
}
</style>
