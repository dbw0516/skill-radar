import { defineStore } from 'pinia'
import { useAuthStore } from './auth'
import { api } from '@/api/client'

// 对应设计文档「用户画像怎么建」一节：细粒度技能状态 + 当前目标岗位类别。
// targetCategoryId 现在会持久化到后端（User.targetCategoryId），不再只是内存态——
// 登录时从 authStore.user 同步过来，选择时顺带写回后端，换个电脑登录也能接着用。
export const useUserProfileStore = defineStore('userProfile', {
  state: () => ({
    targetCategoryId: null,
    masteredSkillIds: [],
  }),
  actions: {
    syncFromUser(user) {
      this.targetCategoryId = user?.targetCategoryId ?? null
    },
    async setTargetCategory(categoryId) {
      this.targetCategoryId = categoryId
      const auth = useAuthStore()
      if (!auth.userId) return
      try {
        const updated = await api.setTargetCategory(auth.userId, categoryId)
        auth.setUser(updated)
      } catch {
        // 写回后端失败不影响当前页面继续用这个选择，最多下次登录时丢失
      }
    },
    markMastered(skillId) {
      if (!this.masteredSkillIds.includes(skillId)) {
        this.masteredSkillIds.push(skillId)
      }
    },
    reset() {
      this.targetCategoryId = null
      this.masteredSkillIds = []
    },
  },
})
