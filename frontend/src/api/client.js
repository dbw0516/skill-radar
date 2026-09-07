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
  listPostings: (categoryId, page = 0, size = 20) =>
    request(`/api/job-categories/${categoryId}/postings?page=${page}&size=${size}`),

  // ②技能差距分析引擎
  gapAnalysis: (categoryId, userId = 1) =>
    request(`/api/gap-analysis?categoryId=${categoryId}&userId=${userId}`),

  // ③学习路径规划引擎
  learningPath: (categoryId, userId = 1) =>
    request(`/api/learning-path?categoryId=${categoryId}&userId=${userId}`),

  // ④资料与测评匹配引擎
  skillResources: (skillId) => request(`/api/skills/${skillId}/resources`),
  skillQuestions: (skillId) => request(`/api/skills/${skillId}/questions`),
  submitQuiz: (payload) =>
    request('/api/quiz-attempts', { method: 'POST', body: JSON.stringify(payload) }),
}
