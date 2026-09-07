<script setup>
import { useUserProfileStore } from '@/stores/userProfile'

// 对应②技能差距分析引擎的展示层（技能差距雷达图）。
// 待后端提供 GET /api/gap-analysis?categoryId=... 后，在这里请求并渲染。
// 可视化建议用 ECharts 雷达图，按「用户画像怎么建」一节的 5 个技能大类聚合展示。
const profile = useUserProfileStore()
</script>

<template>
  <section>
    <h1>技能差距</h1>
    <p v-if="!profile.targetCategoryId">请先在"岗位推荐"页选择一个目标岗位。</p>
    <p v-else>TODO：调用差距分析接口，按技能大类渲染雷达图（已掌握 vs 待学）。</p>
  </section>
</template>
