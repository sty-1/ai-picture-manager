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
        scale: 1.015,
        y: -3,
        boxShadow: '0 12px 32px rgba(18,15,23,0.12)',
        duration: 0.3,
        ease: 'power2.out',
      })
    }

    const onLeave = () => {
      gsap.killTweensOf(el)
      gsap.to(el, {
        scale: 1,
        y: 0,
        boxShadow: '0 1px 2px rgba(0,0,0,0.06)',
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
