export const THEMES = ['growth', 'blueprint', 'night', 'sunrise', 'mono']

export function nextTheme(theme) {
  const index = THEMES.indexOf(theme)
  return THEMES[(index + 1) % THEMES.length]
}

export function readTheme(storage, key = 'skill-radar-theme') {
  const value = storage?.getItem(key)
  return THEMES.includes(value) ? value : THEMES[0]
}

export function writeTheme(storage, theme, key = 'skill-radar-theme') {
  const value = THEMES.includes(theme) ? theme : THEMES[0]
  storage?.setItem(key, value)
  return value
}
