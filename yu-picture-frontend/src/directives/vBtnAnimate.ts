import { gsap } from 'gsap'
import type { Directive } from 'vue'

const prefersReducedMotion = (): boolean => {
  if (typeof window === 'undefined') return false
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

const EASE_OUT = 'power2.out'
const EASE_IN = 'power2.in'

const enterTimeline = (el: HTMLElement) => {
  gsap.killTweensOf(el)
  gsap.to(el, {
    scale: 1.03,
    y: -1,
    boxShadow: '0 8px 28px rgba(18,15,23,0.28)',
    duration: 0.3,
    ease: EASE_OUT,
  })
}

const leaveTimeline = (el: HTMLElement) => {
  gsap.killTweensOf(el)
  gsap.to(el, {
    scale: 1,
    y: 0,
    boxShadow: '0 2px 8px rgba(18,15,23,0.12)',
    duration: 0.25,
    ease: EASE_IN,
  })
}

const pressAnimation = (el: HTMLElement) => {
  gsap.killTweensOf(el)
  gsap.timeline()
    .to(el, {
      scale: 0.96,
      y: 1,
      boxShadow: '0 1px 3px rgba(18,15,23,0.06)',
      duration: 0.1,
      ease: EASE_IN,
    })
    .to(el, {
      scale: 1.03,
      y: -1,
      boxShadow: '0 8px 28px rgba(18,15,23,0.28)',
      duration: 0.25,
      ease: 'elastic.out(1, 0.4)',
    })
}

/**
 * v-btn-animate — GSAP-powered micro-interactions for buttons.
 *
 * Adds hover lift, press bounce, and respects prefers-reduced-motion.
 * Usage: <a-button v-btn-animate type="primary">Click</a-button>
 */
export const vBtnAnimate: Directive<HTMLElement> = {
  mounted(el: HTMLElement) {
    if (prefersReducedMotion()) return

    el.style.willChange = 'transform'

    const onEnter = () => enterTimeline(el)
    const onLeave = () => leaveTimeline(el)
    const onPress = () => {
      if (el.matches(':disabled')) return
      pressAnimation(el)
    }

    el.addEventListener('mouseenter', onEnter)
    el.addEventListener('mouseleave', onLeave)
    el.addEventListener('mousedown', onPress)

    const cleanup = () => {
      el.removeEventListener('mouseenter', onEnter)
      el.removeEventListener('mouseleave', onLeave)
      el.removeEventListener('mousedown', onPress)
      gsap.killTweensOf(el)
    }
    ;(el as HTMLElement & { __btnAnimateCleanup?: () => void }).__btnAnimateCleanup = cleanup
  },

  unmounted(el: HTMLElement) {
    const extended = el as HTMLElement & { __btnAnimateCleanup?: () => void }
    extended.__btnAnimateCleanup?.()
  },
}
