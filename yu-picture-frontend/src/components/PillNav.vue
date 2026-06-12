<template>
  <div class="pill-nav-container">
    <nav
      class="pill-nav"
      :class="className"
      aria-label="Primary"
      :style="cssVars"
    >
      <!-- Logo -->
      <RouterLink
        class="pill-logo"
        to="/"
        aria-label="Home"
        @mouseenter="handleLogoEnter"
      >
        <img
          v-if="logo"
          ref="logoImgRef"
          :src="logo"
          :alt="logoAlt"
        />
      </RouterLink>

      <!-- Desktop nav items -->
      <div ref="navItemsRef" class="pill-nav-items desktop-only">
        <ul class="pill-list" role="menubar">
          <li
            v-for="(item, i) in items"
            :key="item.href || `item-${i}`"
            role="none"
          >
            <RouterLink
              v-if="isRouterLink(item.href)"
              role="menuitem"
              :to="item.href"
              class="pill"
              :class="{ 'is-active': activeHref === item.href }"
              :aria-label="item.ariaLabel || item.label"
              @mouseenter="handleEnter(i)"
              @mouseleave="handleLeave(i)"
            >
              <span
                ref="circleRefs"
                class="hover-circle"
                aria-hidden="true"
              />
              <span class="label-stack">
                <span class="pill-label">{{ item.label }}</span>
                <span class="pill-label-hover" aria-hidden="true">{{ item.label }}</span>
              </span>
            </RouterLink>
            <a
              v-else
              role="menuitem"
              :href="item.href"
              class="pill"
              :class="{ 'is-active': activeHref === item.href }"
              :aria-label="item.ariaLabel || item.label"
              @mouseenter="handleEnter(i)"
              @mouseleave="handleLeave(i)"
            >
              <span
                ref="circleRefs"
                class="hover-circle"
                aria-hidden="true"
              />
              <span class="label-stack">
                <span class="pill-label">{{ item.label }}</span>
                <span class="pill-label-hover" aria-hidden="true">{{ item.label }}</span>
              </span>
            </a>
          </li>
        </ul>
      </div>

      <!-- Mobile hamburger -->
      <button
        class="mobile-menu-button mobile-only"
        @click="toggleMobileMenu"
        aria-label="Toggle menu"
      >
        <span
          v-for="i in 2"
          :key="i"
          ref="hamburgerLines"
          class="hamburger-line"
        />
      </button>
    </nav>

    <!-- Mobile popover -->
    <div
      ref="mobileMenuRef"
      class="mobile-menu-popover mobile-only"
      :style="cssVars"
    >
      <ul class="mobile-menu-list">
        <li
          v-for="(item, i) in items"
          :key="item.href || `mobile-${i}`"
        >
          <RouterLink
            v-if="isRouterLink(item.href)"
            :to="item.href"
            class="mobile-menu-link"
            :class="{ 'is-active': activeHref === item.href }"
            @click="closeMobileMenu"
          >
            {{ item.label }}
          </RouterLink>
          <a
            v-else
            :href="item.href"
            class="mobile-menu-link"
            :class="{ 'is-active': activeHref === item.href }"
            @click="closeMobileMenu"
          >
            {{ item.label }}
          </a>
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { gsap } from 'gsap'

export interface PillNavItem {
  label: string
  href: string
  ariaLabel?: string
}

const props = withDefaults(defineProps<{
  logo?: string
  logoAlt?: string
  items: PillNavItem[]
  activeHref?: string
  className?: string
  ease?: string
  baseColor?: string
  pillColor?: string
  hoveredPillTextColor?: string
  pillTextColor?: string
  initialLoadAnimation?: boolean
}>(), {
  logo: '',
  logoAlt: 'Logo',
  activeHref: undefined,
  className: '',
  ease: 'power3.easeOut',
  baseColor: '#fff',
  pillColor: '#120F17',
  hoveredPillTextColor: '#120F17',
  pillTextColor: undefined,
  initialLoadAnimation: true,
})

const emit = defineEmits<{
  mobileMenuClick: []
}>()

const resolvedPillTextColor = computed(() => props.pillTextColor ?? props.baseColor)

const cssVars = computed(() => ({
  '--base': props.baseColor,
  '--pill-bg': props.pillColor,
  '--hover-text': props.hoveredPillTextColor,
  '--pill-text': resolvedPillTextColor.value,
}))

const isMobileMenuOpen = ref(false)

const circleRefs = ref<(HTMLElement | null)[]>([])
const tlRefs = ref<gsap.core.Timeline[]>([])
const activeTweenRefs = ref<gsap.core.Tween[]>([])
const logoImgRef = ref<HTMLImageElement | null>(null)
const logoTweenRef = ref<gsap.core.Tween | null>(null)
const hamburgerLines = ref<(HTMLElement | null)[]>([])
const mobileMenuRef = ref<HTMLDivElement | null>(null)
const navItemsRef = ref<HTMLDivElement | null>(null)
const logoRef = ref<HTMLElement | null>(null)

let resizeHandler: (() => void) | null = null

const isRouterLink = (href: string) => {
  return href && !(
    href.startsWith('http://') ||
    href.startsWith('https://') ||
    href.startsWith('//') ||
    href.startsWith('mailto:') ||
    href.startsWith('tel:') ||
    href.startsWith('#')
  )
}

const layout = () => {
  nextTick(() => {
    circleRefs.value.forEach((circle) => {
      if (!circle?.parentElement) return

      const pill = circle.parentElement
      const rect = pill.getBoundingClientRect()
      const w = rect.width
      const h = rect.height
      const R = ((w * w) / 4 + h * h) / (2 * h)
      const D = Math.ceil(2 * R) + 2
      const delta = Math.ceil(R - Math.sqrt(Math.max(0, R * R - (w * w) / 4))) + 1
      const originY = D - delta

      circle.style.width = `${D}px`
      circle.style.height = `${D}px`
      circle.style.bottom = `-${delta}px`

      gsap.set(circle, {
        xPercent: -50,
        scale: 0,
        transformOrigin: `50% ${originY}px`,
      })

      const label = pill.querySelector('.pill-label') as HTMLElement | null
      const white = pill.querySelector('.pill-label-hover') as HTMLElement | null

      if (label) gsap.set(label, { y: 0 })
      if (white) gsap.set(white, { y: h + 12, opacity: 0 })

      const index = circleRefs.value.indexOf(circle)
      if (index === -1) return

      tlRefs.value[index]?.kill()
      const tl = gsap.timeline({ paused: true })

      tl.to(circle, { scale: 1.2, xPercent: -50, duration: 2, ease: props.ease, overwrite: 'auto' }, 0)

      if (label) {
        tl.to(label, { y: -(h + 8), duration: 2, ease: props.ease, overwrite: 'auto' }, 0)
      }

      if (white) {
        gsap.set(white, { y: Math.ceil(h + 100), opacity: 0 })
        tl.to(white, { y: 0, opacity: 1, duration: 2, ease: props.ease, overwrite: 'auto' }, 0)
      }

      tlRefs.value[index] = tl
    })
  })
}

const handleEnter = (i: number) => {
  const tl = tlRefs.value[i]
  if (!tl) return
  activeTweenRefs.value[i]?.kill()
  activeTweenRefs.value[i] = tl.tweenTo(tl.duration(), {
    duration: 0.3,
    ease: props.ease,
    overwrite: 'auto',
  })
}

const handleLeave = (i: number) => {
  const tl = tlRefs.value[i]
  if (!tl) return
  activeTweenRefs.value[i]?.kill()
  activeTweenRefs.value[i] = tl.tweenTo(0, {
    duration: 0.2,
    ease: props.ease,
    overwrite: 'auto',
  })
}

const handleLogoEnter = () => {
  const img = logoImgRef.value
  if (!img) return
  logoTweenRef.value?.kill()
  gsap.set(img, { rotate: 0 })
  logoTweenRef.value = gsap.to(img, {
    rotate: 360,
    duration: 0.2,
    ease: props.ease,
    overwrite: 'auto',
  })
}

const toggleMobileMenu = () => {
  const newState = !isMobileMenuOpen.value
  isMobileMenuOpen.value = newState

  const lines = hamburgerLines.value
  if (lines.length >= 2) {
    if (newState) {
      gsap.to(lines[0], { rotation: 45, y: 3, duration: 0.3, ease: props.ease })
      gsap.to(lines[1], { rotation: -45, y: -3, duration: 0.3, ease: props.ease })
    } else {
      gsap.to(lines[0], { rotation: 0, y: 0, duration: 0.3, ease: props.ease })
      gsap.to(lines[1], { rotation: 0, y: 0, duration: 0.3, ease: props.ease })
    }
  }

  const menu = mobileMenuRef.value
  if (menu) {
    if (newState) {
      gsap.set(menu, { visibility: 'visible' })
      gsap.fromTo(
        menu,
        { opacity: 0, y: 10, scaleY: 1 },
        {
          opacity: 1, y: 0, scaleY: 1, duration: 0.3,
          ease: props.ease, transformOrigin: 'top center',
        }
      )
    } else {
      gsap.to(menu, {
        opacity: 0, y: 10, scaleY: 1, duration: 0.2,
        ease: props.ease, transformOrigin: 'top center',
        onComplete: () => {
          gsap.set(menu, { visibility: 'hidden' })
        },
      })
    }
  }

  emit('mobileMenuClick')
}

const closeMobileMenu = () => {
  isMobileMenuOpen.value = false
}

onMounted(() => {
  layout()

  resizeHandler = () => layout()
  window.addEventListener('resize', resizeHandler)

  if (typeof document !== 'undefined' && document.fonts?.ready) {
    document.fonts.ready.then(layout).catch(() => {})
  }

  const menu = mobileMenuRef.value
  if (menu) {
    gsap.set(menu, { visibility: 'hidden', opacity: 0, scaleY: 1 })
  }

  if (props.initialLoadAnimation) {
    const logo = logoRef.value
    const navItems = navItemsRef.value

    if (logo) {
      gsap.set(logo, { scale: 0 })
      gsap.to(logo, { scale: 1, duration: 0.6, ease: props.ease })
    }

    if (navItems) {
      gsap.set(navItems, { width: 0, overflow: 'hidden' })
      gsap.to(navItems, { width: 'auto', duration: 0.6, ease: props.ease })
    }
  }
})

onUnmounted(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
  tlRefs.value.forEach(tl => tl?.kill())
  activeTweenRefs.value.forEach(t => t?.kill())
  logoTweenRef.value?.kill()
})

// 导航项变化时重新布局
watch(() => props.items, () => {
  nextTick(() => layout())
}, { deep: true })
</script>

<style scoped>
.pill-nav-container {
  z-index: 99;
  position: relative;
}

.pill-nav {
  --nav-h: 42px;
  --pill-pad-x: 18px;
  --pill-gap: 3px;
  width: max-content;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  gap: 8px;
}

.pill-nav-items {
  position: relative;
  display: flex;
  align-items: center;
  height: var(--nav-h);
  background: var(--base, #000);
  border-radius: 9999px;
}

.pill-logo {
  width: var(--nav-h);
  height: var(--nav-h);
  border-radius: 50%;
  background: var(--base, #000);
  padding: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.pill-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.pill-list {
  list-style: none;
  display: flex;
  align-items: stretch;
  gap: var(--pill-gap);
  margin: 0;
  padding: 3px;
  height: 100%;
}

.pill-list > li {
  display: flex;
  height: 100%;
}

.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0 var(--pill-pad-x);
  background: var(--pill-bg, #fff);
  color: var(--pill-text, var(--base, #000));
  text-decoration: none;
  border-radius: 9999px;
  box-sizing: border-box;
  font-weight: 600;
  font-size: 15px;
  line-height: 0;
  white-space: nowrap;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.pill .hover-circle {
  position: absolute;
  left: 50%;
  bottom: 0;
  border-radius: 50%;
  background: var(--base, #000);
  z-index: 1;
  display: block;
  pointer-events: none;
  will-change: transform;
}

.pill .label-stack {
  position: relative;
  display: inline-block;
  line-height: 1;
  z-index: 2;
}

.pill .pill-label {
  position: relative;
  z-index: 2;
  display: inline-block;
  line-height: 1;
  will-change: transform;
}

.pill .pill-label-hover {
  position: absolute;
  left: 0;
  top: 0;
  color: var(--hover-text, #fff);
  z-index: 3;
  display: inline-block;
  will-change: transform, opacity;
}

.pill.is-active::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 12px;
  height: 12px;
  background: var(--base, #000);
  border-radius: 50px;
  z-index: 4;
}

.desktop-only {
  display: block;
}

.mobile-only {
  display: none;
}

@media (max-width: 768px) {
  .desktop-only {
    display: none;
  }
  .mobile-only {
    display: block;
  }
  .pill-nav-container {
    width: 100%;
    left: 0;
  }
  .pill-nav {
    width: 100%;
    justify-content: space-between;
    padding: 0 1rem;
    background: transparent;
  }
}

.mobile-menu-button {
  width: var(--nav-h);
  height: var(--nav-h);
  border-radius: 50%;
  background: var(--base, #000);
  border: none;
  display: none;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  padding: 0;
  position: relative;
}

@media (max-width: 768px) {
  .mobile-menu-button {
    display: flex;
  }
}

.hamburger-line {
  width: 16px;
  height: 2px;
  background: var(--pill-bg, #fff);
  border-radius: 1px;
  transition: all 0.01s ease;
  transform-origin: center;
}

.mobile-menu-popover {
  position: absolute;
  top: 3em;
  left: 1rem;
  right: 1rem;
  background: var(--base, #f0f0f0);
  border-radius: 27px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  z-index: 998;
  opacity: 0;
  transform-origin: top center;
  visibility: hidden;
}

.mobile-menu-list {
  list-style: none;
  margin: 0;
  padding: 3px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.mobile-menu-list .mobile-menu-link {
  display: block;
  padding: 12px 16px;
  color: var(--pill-text, #fff);
  background-color: var(--pill-bg, #fff);
  text-decoration: none;
  font-size: 16px;
  font-weight: 500;
  border-radius: 50px;
  transition: all 0.2s ease;
}

.mobile-menu-list .mobile-menu-link:hover {
  cursor: pointer;
  background-color: var(--base);
  color: var(--hover-text, #fff);
}
</style>
