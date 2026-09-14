import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs'

function read(path) {
  return fs.readFileSync(new URL(path, import.meta.url), 'utf8')
}

test('blueprint design tokens define a clear surface and semantic color system', () => {
  const css = read('../src/assets/base.css')
  assert.match(css, /--color-surface-raised:/)
  assert.match(css, /--color-on-primary:/)
  assert.match(css, /--color-focus:/)
  assert.match(css, /--radius-md:\s*8px/)
})

test('global component rules provide accessible controls and responsive panels', () => {
  const css = read('../src/assets/main.css')
  assert.match(css, /min-height:\s*44px/)
  assert.match(css, /button:active/)
  assert.match(css, /main\s+section\s*\{/)
  assert.match(css, /@media\s*\(max-width:\s*760px\)/)
})

test('tab navigation exposes selected state semantics', () => {
  const tabs = read('../src/components/TabBar.vue')
  assert.match(tabs, /role="tablist"/)
  assert.match(tabs, /role="tab"/)
  assert.match(tabs, /:aria-selected="modelValue === t.key"/)
})
