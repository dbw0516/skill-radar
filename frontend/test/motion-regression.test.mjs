import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs'

function read(path) {
  return fs.readFileSync(new URL(path, import.meta.url), 'utf8')
}

test('app shell exposes a page transition with reduced-motion fallback', () => {
  const app = read('../src/App.vue')
  const css = read('../src/assets/main.css')
  assert.match(app, /<Transition\s+name="page"\s+mode="out-in">/)
  assert.match(css, /prefers-reduced-motion:\s*reduce/)
})

test('job and learning lists use staggered transitions', () => {
  const jobs = read('../src/views/JobListView.vue')
  const path = read('../src/views/PathView.vue')
  assert.match(jobs, /<TransitionGroup\b(?=[^>]*\bname="stagger")/)
  assert.match(path, /<TransitionGroup\b(?=[^>]*\bname="stage")/)
})

test('quiz result uses focused feedback state', () => {
  const quiz = read('../src/views/QuizView.vue')
  assert.match(quiz, /class="result feedback-focus"/)
  assert.match(quiz, /class="submit-button"/)
})
