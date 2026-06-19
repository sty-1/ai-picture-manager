<template>
  <div class="bg-carousel" aria-hidden="true">
    <div
      v-for="(img, i) in images"
      :key="i"
      class="bg-layer"
      :class="{ active: i === current, leaving: i === leaving }"
      :style="{ backgroundImage: `url('${img}')` }"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

interface Props {
  images?: string[]
  interval?: number
}

const props = withDefaults(defineProps<Props>(), {
  images: () => [],
  interval: 8000,
})

const current = ref(0)
const leaving = ref(-1)
let timer: ReturnType<typeof setInterval> | null = null

const next = () => {
  leaving.value = current.value
  current.value = (current.value + 1) % props.images.length
  // 动画结束后清除 leaving
  setTimeout(() => {
    leaving.value = -1
  }, 1500)
}

onMounted(() => {
  if (props.images.length > 1) {
    timer = setInterval(next, props.interval)
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.bg-carousel {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

.bg-layer {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0;
  transform: scale(1.05);
  transition: opacity 1.8s cubic-bezier(0.4, 0, 0.2, 1), transform 1.8s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: opacity, transform;
}

.bg-layer.active {
  opacity: 1;
  transform: scale(1);
}

.bg-layer.leaving {
  opacity: 0;
  transform: scale(1.02);
}

</style>
