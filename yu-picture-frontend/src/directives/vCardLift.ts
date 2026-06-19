import { gsap } from 'gsap'
import type { Directive } from 'vue'

const prefersReducedMotion = (): boolean => {
  if (typeof window === 'undefined') return false
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

/**
 * v-card-lift — GSAP 驱动的卡片 hover 微提升。
 * 用法：<a-card v-card-lift hoverable>...</a-card>
 */
export const vCardLift: Directive<HTMLElement> = {
  mounted(el: HTMLElement) {
    if (prefersReducedMotion()) return

    el.style.willChange = 'transform'

    const onEnter = () => {
      gsap.killTweensOf(el)
      gsap.to(el, {
        scale: 1.02,
        y: -4,
        boxShadow: '0 16px 48px rgba(0,0,0,0.1)',
        duration: 0.35,
        ease: 'power2.out',
      })
    }

    const onLeave = () => {
      gsap.killTweensOf(el)
      gsap.to(el, {
        scale: 1,
        y: 0,
        boxShadow: '0 2px 16px rgba(0,0,0,0.04)',
        duration: 0.25,
        ease: 'power2.in',
      })
    }

    el.addEventListener('mouseenter', onEnter)
    el.addEventListener('mouseleave', onLeave)

    const cleanup = () => {
      el.removeEventListener('mouseenter', onEnter)
      el.removeEventListener('mouseleave', onLeave)
      gsap.killTweensOf(el)
    }
    ;(el as HTMLElement & { __cardLiftCleanup?: () => void }).__cardLiftCleanup = cleanup
  },

  unmounted(el: HTMLElement) {
    const extended = el as HTMLElement & { __cardLiftCleanup?: () => void }
    extended.__cardLiftCleanup?.()
  },
}
