import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs'

const app = fs.readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8')

test('theme picker exposes an explicit menu and selected state', () => {
  assert.match(app, /aria-haspopup="menu"/)
  assert.match(app, /:aria-expanded="themeMenuOpen"/)
  assert.match(app, /role="menu"/)
  assert.match(app, /role="menuitemradio"/)
  assert.match(app, /:aria-checked="theme === option.id"/)
  assert.match(app, /sunrise: '暖阳'/)
  assert.match(app, /mono: '墨白'/)
})
