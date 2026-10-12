// 学习路径地铁图的布局计算：纯函数，不碰 DOM，方便单测（test/metro-layout.test.mjs）。
//
// 映射关系：
//  - 一个技能大类（domain）= 一条线路，横着画；
//  - 一个学习阶段 = 一个区间（一列），已掌握的技能统一放在最左边的"已到达"区（stage 0）；
//  - 所有线路最后汇入右边的终点站（目标岗位）。
// 同一列里同一条线有好几个站（同阶段可并行学的同类技能）时，横向排开，列也跟着变宽。
// 画布大小按数据现算，组件外面套横向滚动，所以岗位有几个阶段、几个大类都画得下。

export const LINE_COLORS = ['#ff4d5e', '#22c27a', '#3d7bff', '#ff9f1c', '#ff7ab6', '#2ec4c4', '#b4e04a', '#c98b4b']

const SLOT_W = 150 // 一个站点占的宽度（含站名）
const ROW_H = 124 // 两条线之间的距离（站点 + 两行站名）
const TOP = 96 // 顶部留给区间标签
const LEFT = 24
const TERMINUS_GAP = 56
const TERMINUS_W = 170
const RIGHT = 24
const LABEL_H = 78 // 站点圆心到站名底部
const BADGE_OFFSET = 50 // 线路编号方块在第一个站左边多远

function domainOf(s) {
  return s.domain || '其他'
}

function isMastered(s) {
  return !!s.mastered || s.stage === 0
}

/** 每个技能大类分到一条线：近期就要学的线排上面，只剩已掌握站点的线排最下面。 */
export function assignLines(stations) {
  const stats = new Map()
  for (const s of stations) {
    const d = domainOf(s)
    if (!stats.has(d)) stats.set(d, { domain: d, count: 0, firstTodoStage: Infinity })
    const rec = stats.get(d)
    rec.count++
    if (!isMastered(s)) rec.firstTodoStage = Math.min(rec.firstTodoStage, s.stage)
  }
  return [...stats.values()]
    .sort((a, b) => a.firstTodoStage - b.firstTodoStage || b.count - a.count || a.domain.localeCompare(b.domain, 'zh'))
    .map((rec, i) => ({ domain: rec.domain, number: i + 1, color: LINE_COLORS[i % LINE_COLORS.length] }))
}

/** 下一站：指定的技能还没掌握就用它，否则取最早阶段里 JD 权重最高的那个。 */
export function pickNextStation(stations, nextSkillId) {
  const todo = stations.filter((s) => !isMastered(s))
  if (!todo.length) return null
  const explicit = nextSkillId != null && todo.find((s) => s.skillId === nextSkillId)
  if (explicit) return explicit
  const firstStage = Math.min(...todo.map((s) => s.stage))
  return todo
    .filter((s) => s.stage === firstStage)
    .reduce((best, s) => ((s.weight ?? 0) > (best.weight ?? 0) ? s : best))
}

/**
 * @param fitWidth 外层卡片的宽度。数据少、自然宽度不够时，把多出来的宽度按站位数分给各列，
 *                 让线路图撑满卡片；比自然宽度还窄时不压缩，交给外层横向滚动。
 */
export function layoutMetro(stations, { nextSkillId = null, fitWidth = 0 } = {}) {
  const lines = assignLines(stations)
  const rowOf = new Map(lines.map((l, i) => [l.domain, i]))
  const next = pickNextStation(stations, nextSkillId)

  // 按 (阶段, 线路) 分组，组内按权重从高到低
  const stages = [...new Set(stations.map((s) => (isMastered(s) ? 0 : s.stage)))].sort((a, b) => a - b)
  const groups = new Map()
  for (const s of stations) {
    const stage = isMastered(s) ? 0 : s.stage
    const key = `${stage}|${domainOf(s)}`
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(s)
  }
  for (const g of groups.values()) g.sort((a, b) => (b.weight ?? 0) - (a.weight ?? 0))

  const columns = stages.map((stage) => {
    let slots = 1
    let total = 0
    for (const l of lines) {
      const n = groups.get(`${stage}|${l.domain}`)?.length ?? 0
      slots = Math.max(slots, n)
      total += n
    }
    return {
      stage,
      slots,
      label: stage === 0 ? '已到达' : `第 ${stage} 区`,
      sub: stage === 0 ? 'ARRIVED' : `阶段 ${stage}${total > 1 ? ' · 可并行' : ''}`,
    }
  })
  const totalSlots = columns.reduce((n, c) => n + c.slots, 0)
  const naturalWidth = LEFT + totalSlots * SLOT_W + TERMINUS_GAP + TERMINUS_W + RIGHT
  const extraPerSlot = totalSlots && fitWidth > naturalWidth ? (fitWidth - naturalWidth) / totalSlots : 0
  const slotW = SLOT_W + extraPerSlot

  let cursor = LEFT
  for (const col of columns) {
    col.x = cursor
    col.width = col.slots * slotW
    cursor += col.width
  }

  const y0 = TOP + 26
  const rowY = (i) => y0 + i * ROW_H
  const yLast = rowY(Math.max(lines.length - 1, 0))

  const stops = []
  for (const col of columns) {
    for (const l of lines) {
      const group = groups.get(`${col.stage}|${l.domain}`) || []
      const offset = ((col.slots - group.length) * slotW) / 2
      group.forEach((s, j) => {
        stops.push({
          ...s,
          domain: l.domain,
          x: col.x + offset + (j + 0.5) * slotW,
          y: rowY(rowOf.get(l.domain)),
          line: l,
          state: isMastered(s) ? 'mastered' : next && s.skillId === next.skillId ? 'next' : 'todo',
        })
      })
    }
  }

  const terminusX = cursor + TERMINUS_GAP
  const terminusH = Math.max(150, yLast - y0 + 90)
  const terminus = {
    x: terminusX,
    y: (y0 + yLast) / 2 - terminusH / 2,
    width: TERMINUS_W,
    height: terminusH,
  }

  const lineGeoms = lines.map((l, i) => {
    const xs = stops.filter((s) => s.domain === l.domain).map((s) => s.x)
    return { ...l, y: rowY(i), x1: Math.min(...xs) - BADGE_OFFSET, x2: terminusX + 12 }
  })

  return {
    width: terminusX + TERMINUS_W + RIGHT,
    height: Math.max(yLast + LABEL_H, terminus.y + terminus.height) + 16,
    // 站名标签的宽度：拉伸后的站位很宽时也不让标签无限变宽
    slotW: Math.min(slotW, 200),
    columns,
    lines: lineGeoms,
    stops,
    terminus,
    next,
  }
}
