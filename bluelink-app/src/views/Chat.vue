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
          <h2 class="font-serif font-bold text-ink-900">AI 溯源助手</h2>
        </div>
        <button class="p-2 text-ink-600 press-effect">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
          </svg>
        </button>
      </div>
    </header>

    <main 
      ref="chatContainer"
      class="flex-1 overflow-y-auto hide-scrollbar pt-20 pb-28 px-4"
    >
      <div class="space-y-6">
        <div 
          v-for="(msg, index) in messages" 
          :key="index"
          class="message-bubble"
        >
          <template v-if="msg.type === 'user'">
            <div class="flex justify-end">
              <div class="bg-brand-accent text-white p-4 rounded-2xl rounded-tr-none text-sm max-w-[85%] leading-relaxed">
                {{ msg.content }}
              </div>
            </div>
          </template>
          <template v-else>
            <div class="space-y-4">
              <div class="bg-white p-4 rounded-2xl rounded-tl-none border border-black/5 shadow-sm text-sm text-ink-600 leading-relaxed max-w-[85%]">
                {{ msg.content }}
              </div>
              <div 
                v-if="msg.anchors"
                v-for="anchor in msg.anchors"
                :key="anchor.title"
                class="anchor-card p-4 rounded-2xl bg-white border border-brand-accent/20 shadow-lg"
                @click="openFromAnchor(anchor.documentId)"
              >
                <div class="flex items-center gap-2 mb-2">
                  <span class="w-2 h-2 rounded-full bg-brand-accent"></span>
                  <span class="text-[10px] font-bold text-brand-accent uppercase tracking-wider">溯源引力线</span>
                </div>
                <p class="text-[13px] font-bold serif text-ink-900">{{ anchor.title }}</p>
                <p class="text-[11px] text-ink-400 mt-1">—— {{ anchor.subtitle }}</p>
              </div>
            </div>
          </template>
        </div>
        
        <div v-if="isTyping" class="flex justify-start">
          <div class="bg-white p-4 rounded-2xl rounded-tl-none border border-black/5 shadow-sm">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>
    </main>

    <div class="absolute bottom-0 left-0 right-0 p-4 glass-panel border-t border-parchment-200/40">
      <div class="flex items-center gap-3">
        <button class="p-2 text-ink-600 press-effect">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14M5 12h14"/>
          </svg>
        </button>
        <div 
          ref="inputRef"
          class="flex-1 bg-white rounded-full px-4 py-3 text-sm min-h-[44px] border border-parchment-200/60 focus-within:border-brand-accent/50 outline-none"
          contenteditable="true"
          placeholder="输入你的疑问..."
          @keydown="handleKeydown"
          @input="handleInput"
        ></div>
        <button 
          class="w-10 h-10 rounded-full bg-brand-accent flex items-center justify-center text-white press-effect"
          @click="sendMessage"
          :class="{ 'opacity-50': !inputText.trim() }"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M5 12h14M12 5l7 7-7 7"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { chatHistory } from '@/data/mockData'

const router = useRouter()
const chatContainer = ref(null)
const inputRef = ref(null)
const inputText = ref('')
const isTyping = ref(false)

const messages = reactive([...chatHistory])

const goBack = () => {
  router.push('/home')
}

const handleInput = (e) => {
  inputText.value = e.target.innerText
}

const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const sendMessage = () => {
  const text = inputText.value.trim()
  if (!text) return

  messages.push({ type: 'user', content: text })
  inputText.value = ''
  if (inputRef.value) inputRef.value.innerText = ''
  
  scrollToBottom()

  isTyping.value = true
  setTimeout(() => {
    isTyping.value = false
    messages.push({
      type: 'ai',
      content: '这是一个很好的问题。让我为您定位相关的文献片段...',
      anchors: [{
        title: '《深度学习原理》第 3 章',
        subtitle: '神经网络基础与反向传播算法详解',
        documentId: 1
      }]
    })
    scrollToBottom()
  }, 1500)
}

const openFromAnchor = (docId) => {
  router.push(`/reader/${docId}`)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

const scrollToBottomAsync = async () => {
  await nextTick()
  scrollToBottom()
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.page-container {
  height: 100%;
  width: 100%;
}
</style>
