<script setup>
import { useUserProfileStore } from '@/stores/userProfile'

// 对应③学习路径规划引擎的展示层（学习路径时间线）。
// 待后端提供 GET /api/learning-path?categoryId=... 后，按阶段（拓扑排序分层）渲染。
// 每个技能节点下附对应的学习资料（learning_resources 表）入口。
const profile = useUserProfileStore()
</script>

<template>
  <section>
    <h1>学习路径</h1>
    <p v-if="!profile.targetCategoryId">请先在"岗位推荐"页选择一个目标岗位。</p>
    <p v-else>TODO：调用学习路径接口，按阶段展示技能节点 + 对应学习资料。</p>
  </section>
</template>
