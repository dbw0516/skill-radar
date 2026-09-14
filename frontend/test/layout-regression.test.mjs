import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs'

const css = fs.readFileSync(new URL('../src/assets/main.css', import.meta.url), 'utf8')

test('app shell does not force a two-column desktop layout', () => {
  assert.doesNotMatch(css, /grid-template-columns:\s*1fr\s+1fr/)
  assert.doesNotMatch(css, /body\s*\{[^}]*display:\s*flex/s)
})
