<script setup>
// 对应④资料与测评匹配引擎的展示层（在线测评答题）。
// 待后端提供 GET /api/questions?skillId=...、POST /api/quiz-attempts 后接入。
// 提交后端会判分并在通过时把 user_skills.status 更新为 quiz_verified——
// 这一步就是设计文档里反复提到的"回写技能画像"闭环。
</script>

<template>
  <section>
    <h1>在线测评</h1>
    <p>TODO：选择一个技能 -&gt; 拉取题目 -&gt; 提交作答 -&gt; 展示判分结果。</p>
  </section>
</template>
