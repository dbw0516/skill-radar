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

  // 学习进度条 + 打卡/徽章激励（按用户当前目标岗位算）
  progress: (userId) => request(`/api/users/${userId}/progress`),

  // 错题本 + 间隔复习（复习题提交走 submitQuizStream，mode: 'review'）
  listWrongQuestions: (userId) => request(`/api/users/${userId}/wrong-questions`),
  reviewQuestions: (userId, skillId) =>
    request(`/api/users/${userId}/wrong-questions/review${skillId ? `?skillId=${skillId}` : ''}`),

  // 学习资料阅读记录（计入技能的部分进度）
  listViewedResources: (userId) => request(`/api/users/${userId}/resource-views`),
  markResourceViewed: (userId, resourceId) =>
    request(`/api/users/${userId}/resource-views/${resourceId}`, { method: 'PUT' }),

  // 岗位匹配度
  postingMatch: (postingId, userId) => request(`/api/postings/${postingId}/match?userId=${userId}`),
  postingMatches: (userId, ids) => request(`/api/users/${userId}/posting-matches?ids=${ids.join(',')}`),

  // AI 模拟面试
  startInterview: (userId, categoryId) =>
    request('/api/interview/sessions', { method: 'POST', body: JSON.stringify({ userId, categoryId }) }),
  getInterview: (id, userId) => request(`/api/interview/sessions/${id}?userId=${userId}`),
  // timing：限时模式下的 { seconds, timeLimit, timedOut }，不限时传空对象
  answerInterview: (id, userId, answer, timing = {}) =>
    request(`/api/interview/sessions/${id}/answer`, { method: 'POST', body: JSON.stringify({ userId, answer, ...timing }) }),
  finishInterview: (id, userId) =>
    request(`/api/interview/sessions/${id}/finish`, { method: 'POST', body: JSON.stringify({ userId }) }),
  listInterviews: (userId) => request(`/api/users/${userId}/interview-sessions`),

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
  submitQuizStream,
}

/**
 * 交卷并接收判分进度：后端以 Server-Sent Events 推送 progress {done, total}，最后推 result（完整结果）
 * 或 error {message}。EventSource 只支持 GET，所以用 fetch 读流、自己按空行切事件。
 */
async function submitQuizStream(payload, onProgress) {
  const res = await fetch(`${BASE_URL}/api/quiz-attempts/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(payload),
  })
  if (!res.ok || !res.body) throw new Error(`提交失败 (${res.status})`)

  const reader = res.body.pipeThrough(new TextDecoderStream()).getReader()
  let buffer = ''
  for (;;) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += value.replace(/\r\n/g, '\n')
    let sep
    while ((sep = buffer.indexOf('\n\n')) >= 0) {
      const block = buffer.slice(0, sep)
      buffer = buffer.slice(sep + 2)
      let event = 'message'
      const dataLines = []
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^ /, ''))
      }
      if (!dataLines.length) continue
      const data = JSON.parse(dataLines.join('\n'))
      if (event === 'progress') onProgress?.(data)
      else if (event === 'result') return data
      else if (event === 'error') throw new Error(data.message)
    }
  }
  throw new Error('连接中断，没有收到判分结果，请重新提交')
}
