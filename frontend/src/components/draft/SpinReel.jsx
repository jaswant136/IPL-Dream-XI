import { useEffect, useMemo } from 'react'
import { motion, useReducedMotion } from 'framer-motion'

const ITEM = 96
const REPEATS = 8

/**
 * Roulette-style reel. It never decides anything: it spins while `spinning` and `target` is null, then eases to
 * the franchise code the BACKEND chose and calls onSettled when it lands.
 */
export default function SpinReel({ codes, target, spinning, onSettled, label = 'Franchise' }) {
  const reduce = useReducedMotion()
  const list = useMemo(() => Array.from({ length: REPEATS }, () => codes).flat(), [codes])
  const n = codes.length
  const targetIndex = target ? codes.indexOf(target) : -1
  const finalIndex = targetIndex >= 0 ? (REPEATS - 3) * n + targetIndex : 0
  const centerOffset = ITEM // window shows 3 items; the middle one is the result
  const landed = spinning && target && targetIndex >= 0

  useEffect(() => {
    if (!landed) return undefined
    const t = setTimeout(() => onSettled?.(), reduce ? 50 : 2600)
    return () => clearTimeout(t)
  }, [landed, onSettled, reduce])

  let animate = { x: centerOffset }
  let transition = { duration: 0 }
  if (spinning && !target) {
    animate = { x: [centerOffset, centerOffset - n * ITEM] }
    transition = reduce ? { duration: 0 } : { duration: 0.55, repeat: Infinity, ease: 'linear' }
  } else if (landed) {
    animate = { x: centerOffset - finalIndex * ITEM }
    transition = reduce ? { duration: 0 } : { duration: 2.5, ease: [0.12, 0.8, 0.2, 1] }
  } else if (!spinning && target && targetIndex >= 0) {
    animate = { x: centerOffset - finalIndex * ITEM }
  }

  return (
    <div role="img" aria-label={`${label} reel${target ? `, landed on ${target}` : ''}`}
      className="relative mx-auto h-28 overflow-hidden rounded-2xl border border-edge bg-ink" style={{ width: ITEM * 3, maxWidth: '100%' }}>
      <motion.div className="flex h-full items-center" animate={animate} transition={transition} style={{ width: list.length * ITEM }}>
        {list.map((code, i) => (
          <div key={i} style={{ width: ITEM }} className="flex shrink-0 items-center justify-center">
            <span className="display text-4xl text-snow/80">{code}</span>
          </div>
        ))}
      </motion.div>
      <div aria-hidden="true" className="pointer-events-none absolute inset-y-0 left-1/2 w-24 -translate-x-1/2 border-x-2 border-gold bg-gold/5" />
      <div aria-hidden="true" className="pointer-events-none absolute inset-0 bg-gradient-to-r from-ink via-transparent to-ink" />
    </div>
  )
}
