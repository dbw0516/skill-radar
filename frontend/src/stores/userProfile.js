import { defineStore } from 'pinia'

// 对应设计文档「用户画像怎么建」一节：细粒度技能状态 + 当前目标岗位类别。
// 目前是内存态占位，接入登录后改为从后端拉取。
export const useUserProfileStore = defineStore('userProfile', {
  state: () => ({
    targetCategoryId: null,
    masteredSkillIds: [], // 已掌握（自评或测评认证）的技能 id 列表
  }),
  actions: {
    setTargetCategory(categoryId) {
      this.targetCategoryId = categoryId
    },
    markMastered(skillId) {
      if (!this.masteredSkillIds.includes(skillId)) {
        this.masteredSkillIds.push(skillId)
      }
    },
  },
})
