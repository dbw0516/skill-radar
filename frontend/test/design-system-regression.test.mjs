import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs'

function read(path) {
  return fs.readFileSync(new URL(path, import.meta.url), 'utf8')
}

test('design tokens define a clear surface and semantic color system', () => {
  const css = read('../src/assets/base.css')
  assert.match(css, /--color-surface-raised:/)
  assert.match(css, /--color-on-primary:/)
  assert.match(css, /--color-focus:/)
  assert.match(css, /--radius-md:\s*12px/)
})

test('metro x neo-brutalism is the only theme: hard shadows, thick borders, no theme switching', () => {
  const css = read('../src/assets/base.css')
  const app = read('../src/App.vue')
  // 硬投影：只位移、不模糊
  assert.match(css, /--shadow-lg:\s*8px 8px 0 /)
  assert.match(css, /--border-thick:\s*3px solid/)
  // 四套旧主题和切换菜单都已删除
  assert.doesNotMatch(css, /\[data-theme/)
  assert.doesNotMatch(app, /theme/i)
  assert.equal(fs.existsSync(new URL('../src/theme.js', import.meta.url)), false)
})

test('fonts ship with the app instead of loading from the network at demo time', () => {
  const main = read('../src/main.js')
  assert.match(main, /@fontsource\/noto-sans-sc/)
  assert.match(main, /@fontsource\/zcool-qingke-huangyou/)
  assert.match(main, /@fontsource\/archivo-black/)
})

test('AI features carry a visible AI marker in nav, quiz and interview', () => {
  const css = read('../src/assets/main.css')
  const app = read('../src/App.vue')
  const quiz = read('../src/views/QuizView.vue')
  const interview = read('../src/views/InterviewView.vue')
  assert.match(css, /\.ai-badge\s*\{/)
  assert.match(app, /label: '在线测评', ai: true/)
  assert.match(app, /label: '模拟面试', ai: true/)
  assert.match(quiz, /class="ai-badge">AI 判分</)
  assert.match(quiz, /class="ai-banner"/)
  assert.match(interview, /class="ai-badge">AI 面试官</)
})

test('learning path page is drawn as a metro map', () => {
  const path = read('../src/views/PathView.vue')
  assert.match(path, /<MetroMap\b/)
  assert.match(path, /时刻表/)
})

test('global component rules provide accessible controls and responsive panels', () => {
  const css = read('../src/assets/main.css')
  assert.match(css, /min-height:\s*44px/)
  assert.match(css, /button:active/)
  assert.match(css, /main\s+section\s*\{/)
  assert.match(css, /@media\s*\(max-width:\s*760px\)/)
})

test('chip hover rules outrank the global button hover so the label stays readable', () => {
  const css = read('../src/assets/main.css')
  // button:hover:not(:disabled) 是 (0,2,1)；chip 的 hover 规则若只有 (0,2,0) 会被它盖掉背景色
  assert.match(css, /\.cat-chip:hover:not\(:disabled\)/)
  assert.match(css, /\.cat-chip\.active:hover:not\(:disabled\)/)
})

test('tab navigation exposes selected state semantics', () => {
  const tabs = read('../src/components/TabBar.vue')
  assert.match(tabs, /role="tablist"/)
  assert.match(tabs, /role="tab"/)
  assert.match(tabs, /:aria-selected="modelValue === t.key"/)
})
