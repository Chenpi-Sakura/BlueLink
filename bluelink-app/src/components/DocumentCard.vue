<template>
  <div 
    class="masonry-item rounded-3xl p-5 shadow-card border border-parchment-100/50 relative overflow-hidden group cursor-pointer press-effect"
    @click="handleClick"
  >
    <div class="font-sans text-[11px] text-ink-400 mb-3 flex items-center justify-between">
      <span class="tracking-wider">{{ doc.date }}</span>
    </div>
    <h3 
      class="font-serif font-bold leading-tight mb-3 text-ink-900"
      :class="index % 2 === 0 ? 'text-lg' : 'text-[17px]'"
    >
      {{ doc.title }}
    </h3>
    <div v-if="index % 2 === 1" class="w-8 h-[1px] bg-parchment-200 mb-3"></div>
    <p class="text-[13px] text-ink-600 leading-relaxed">{{ doc.excerpt }}</p>
    <div 
      v-if="doc.tags && doc.tags.length > 0" 
      class="inline-block px-2.5 py-1 bg-parchment-50 rounded-lg font-sans text-[11px] text-ink-400 mt-3"
    >
      {{ doc.tags.map(t => '#' + t).join(' ') }}
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  doc: {
    type: Object,
    required: true
  },
  index: {
    type: Number,
    default: 0
  }
})

const router = useRouter()

const handleClick = () => {
  router.push(`/reader/${props.doc.id}`)
}
</script>
