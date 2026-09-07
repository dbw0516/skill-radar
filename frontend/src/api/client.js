const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function request(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    throw new Error(`请求失败：${path} (${res.status})`)
  }
  return res.status === 204 ? null : res.json()
}

export const api = {
  listSkills: () => request('/api/skills'),
  listJobCategories: () => request('/api/job-categories'),
  // 后续按同样的模式补充：差距分析、学习路径、测评提交等接口
}
