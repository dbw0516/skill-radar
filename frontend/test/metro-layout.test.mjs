import assert from 'node:assert/strict'
import test from 'node:test'
import { LINE_COLORS, assignLines, layoutMetro, pickNextStation } from '../src/metro.js'

// 一份接近真实 Java后端工程师 数据的样例：4 个已掌握，阶段 1 三个、阶段 2 两个
const stations = [
  { skillId: 2, name: 'Java基础', domain: '后端语言', weight: 0.9, stage: 0, mastered: true, masteredSource: 'quiz_verified' },
  { skillId: 5, name: 'MySQL', domain: '数据库', weight: 0.45, stage: 0, mastered: true, masteredSource: 'quiz_verified' },
  { skillId: 9, name: 'Git', domain: '工程工具', weight: 0.4, stage: 0, mastered: true, masteredSource: 'self_reported' },
  { skillId: 8, name: 'Linux基础', domain: '工程工具', weight: 0.35, stage: 0, mastered: true, masteredSource: 'self_reported' },
  { skillId: 3, name: '面向对象', domain: '后端语言', weight: 0.7, stage: 1 },
  { skillId: 7, name: 'Redis', domain: '数据库', weight: 0.3, stage: 1 },
  { skillId: 10, name: 'MyBatis', domain: '后端框架', weight: 0.2, stage: 1 },
  { skillId: 6, name: 'Spring Boot', domain: '后端框架', weight: 0.75, stage: 2 },
  { skillId: 4, name: '数据结构', domain: '算法能力', weight: 0.55, stage: 2 },
]

test('every domain gets its own numbered line, colors cycle through the palette', () => {
  const lines = assignLines(stations)
  assert.equal(lines.length, 5)
  assert.deepEqual(lines.map((l) => l.number), [1, 2, 3, 4, 5])
  assert.equal(new Set(lines.map((l) => l.domain)).size, 5)
  lines.forEach((l, i) => assert.equal(l.color, LINE_COLORS[i % LINE_COLORS.length]))
  // 有近期待学站点的线排在前面，只剩已掌握站点的线排最后
  assert.equal(lines[lines.length - 1].domain, '工程工具')
})

test('missing domain falls back to 其他 instead of breaking the map', () => {
  const lines = assignLines([{ skillId: 1, name: 'X', domain: null, weight: 0.5, stage: 1 }])
  assert.equal(lines[0].domain, '其他')
})

test('columns run left to right: arrived first, then stages in order', () => {
  const map = layoutMetro(stations)
  assert.deepEqual(map.columns.map((c) => c.stage), [0, 1, 2])
  assert.equal(map.columns[0].label, '已到达')
  for (let i = 1; i < map.columns.length; i++) {
    assert.ok(map.columns[i].x >= map.columns[i - 1].x + map.columns[i - 1].width)
  }
  const xOf = (name) => map.stops.find((s) => s.name === name).x
  assert.ok(xOf('Java基础') < xOf('面向对象'))
  assert.ok(xOf('面向对象') < xOf('Spring Boot'))
})

test('stations of one domain sit on one line; different domains on different lines', () => {
  const map = layoutMetro(stations)
  const yOf = (name) => map.stops.find((s) => s.name === name).y
  assert.equal(yOf('Java基础'), yOf('面向对象'))
  assert.equal(yOf('MyBatis'), yOf('Spring Boot'))
  assert.notEqual(yOf('Java基础'), yOf('MySQL'))
  for (const line of map.lines) {
    for (const stop of map.stops.filter((s) => s.domain === line.domain)) {
      assert.equal(stop.y, line.y)
      assert.ok(stop.x > line.x1 && stop.x < line.x2)
    }
  }
})

test('two stations of the same domain in the same column never overlap', () => {
  const map = layoutMetro(stations)
  const tools = map.stops.filter((s) => s.domain === '工程工具')
  assert.equal(tools.length, 2)
  assert.ok(Math.abs(tools[0].x - tools[1].x) >= map.slotW)
})

test('terminus sits right of every stop and inside the canvas', () => {
  const map = layoutMetro(stations)
  for (const s of map.stops) assert.ok(s.x < map.terminus.x)
  assert.ok(map.terminus.x + map.terminus.width <= map.width)
  for (const line of map.lines) assert.ok(line.y > map.terminus.y && line.y < map.terminus.y + map.terminus.height)
})

test('next station: explicit id wins, otherwise the heaviest skill of the earliest stage', () => {
  assert.equal(pickNextStation(stations, 7).skillId, 7)
  // 已掌握的技能不能当下一站
  assert.equal(pickNextStation(stations, 2).skillId, 3)
  assert.equal(pickNextStation(stations, null).skillId, 3)
  const map = layoutMetro(stations, { nextSkillId: 7 })
  assert.deepEqual(map.stops.filter((s) => s.state === 'next').map((s) => s.name), ['Redis'])
  assert.equal(map.stops.filter((s) => s.state === 'mastered').length, 4)
})

test('fitWidth stretches the columns to fill the card, never shrinks below natural size', () => {
  const natural = layoutMetro(stations)
  const wide = layoutMetro(stations, { fitWidth: natural.width + 300 })
  assert.equal(wide.width, natural.width + 300)
  assert.ok(wide.columns.every((c, i) => c.width > natural.columns[i].width))
  for (const s of wide.stops) {
    const col = wide.columns.find((c) => c.stage === (s.mastered ? 0 : s.stage))
    assert.ok(s.x > col.x && s.x < col.x + col.width)
  }
  const narrow = layoutMetro(stations, { fitWidth: 200 })
  assert.equal(narrow.width, natural.width)
})

test('an empty path still produces a valid canvas', () => {
  const map = layoutMetro([])
  assert.equal(map.stops.length, 0)
  assert.ok(map.width > 0 && map.height > 0)
})
