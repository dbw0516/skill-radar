import assert from 'node:assert/strict'
import test from 'node:test'
import { THEMES, nextTheme, readTheme, writeTheme } from '../src/theme.js'

test('offers five distinct theme choices', () => {
  assert.deepEqual(THEMES, ['growth', 'blueprint', 'night', 'sunrise', 'mono'])
  assert.equal(nextTheme('growth'), 'blueprint')
  assert.equal(nextTheme('blueprint'), 'night')
  assert.equal(nextTheme('night'), 'sunrise')
  assert.equal(nextTheme('sunrise'), 'mono')
  assert.equal(nextTheme('mono'), 'growth')
})

test('reads only supported persisted themes', () => {
  const storage = { getItem: () => 'sunrise' }
  assert.equal(readTheme(storage), 'sunrise')
  assert.equal(readTheme({ getItem: () => 'mono' }), 'mono')
  assert.equal(readTheme({ getItem: () => 'unknown' }), 'growth')
})

test('writes a supported theme and falls back for invalid values', () => {
  const values = {}
  const storage = { setItem: (key, value) => { values[key] = value } }
  assert.equal(writeTheme(storage, 'blueprint'), 'blueprint')
  assert.equal(values['skill-radar-theme'], 'blueprint')
  assert.equal(writeTheme(storage, 'unknown'), 'growth')
})
