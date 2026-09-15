const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function request(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    let message = `请求失败 (${res.status})`
    try {
      const body = await res.json()
      if (body?.message) message = body.message
    } catch {
      // 响应体不是 JSON（比如后端没启动时网关/浏览器给的错误页），用上面的默认提示
    }
    throw new Error(message)
  }
  if (res.status === 204) return null
  // 不只判 204——某些接口是 void 方法，Spring Boot 默认给 200 + 空 body（不是 204），
  // 这种情况 res.json() 拿空字符串解析会直接抛异常，调用方大多不会预料到，容易被静默吞掉。
  const text = await res.text()
  return text ? JSON.parse(text) : null
}

export const api = {
  listSkills: () => request('/api/skills'),
  listMajors: () => request('/api/majors'),
  listJobCategories: () => request('/api/job-categories'),
  listPostings: (categoryId, { preferLocation, page = 0, size = 20 } = {}) => {
    const params = new URLSearchParams({ page, size })
    if (preferLocation) params.set('preferLocation', preferLocation)
    return request(`/api/job-categories/${categoryId}/postings?${params}`)
  },
  postingDetail: (id) => request(`/api/postings/${id}`),

  // 收藏
  listFavorites: (userId) => request(`/api/users/${userId}/favorites`),
  checkFavorite: (userId, postingId) => request(`/api/users/${userId}/favorites/${postingId}`),
  addFavorite: (userId, postingId) => request(`/api/users/${userId}/favorites/${postingId}`, { method: 'PUT' }),
  removeFavorite: (userId, postingId) => request(`/api/users/${userId}/favorites/${postingId}`, { method: 'DELETE' }),

  // 登录注册
  register: (payload) => request('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
  login: (payload) => request('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  setTargetCategory: (userId, categoryId) =>
    request(`/api/auth/users/${userId}/target-category`, {
      method: 'PUT',
      body: JSON.stringify({ categoryId }),
    }),
  updateProfile: (userId, payload) =>
    request(`/api/auth/users/${userId}/profile`, { method: 'PUT', body: JSON.stringify(payload) }),

  // 技能自评清单（个人中心）
  listUserSkills: (userId) => request(`/api/users/${userId}/skills`),
  setUserSkills: (userId, skillIds) =>
    request(`/api/users/${userId}/skills`, { method: 'PUT', body: JSON.stringify({ skillIds }) }),
  removeUserSkill: (userId, skillId) =>
    request(`/api/users/${userId}/skills/${skillId}`, { method: 'DELETE' }),

  // 错题本
  listWrongQuestions: (userId) => request(`/api/users/${userId}/wrong-questions`),

  // ②技能差距分析引擎
  gapAnalysis: (categoryId, userId) =>
    request(`/api/gap-analysis?categoryId=${categoryId}&userId=${userId}`),

  // ③学习路径规划引擎
  learningPath: (categoryId, userId) =>
    request(`/api/learning-path?categoryId=${categoryId}&userId=${userId}`),

  // ④资料与测评匹配引擎
  skillResources: (skillId) => request(`/api/skills/${skillId}/resources`),
  skillQuestions: (skillId) => request(`/api/skills/${skillId}/questions`),
  submitQuiz: (payload) =>
    request('/api/quiz-attempts', { method: 'POST', body: JSON.stringify(payload) }),
}
