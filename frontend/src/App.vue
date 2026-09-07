<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <nav>
    <RouterLink to="/">岗位推荐</RouterLink>
    <RouterLink to="/gap">技能差距</RouterLink>
    <RouterLink to="/path">学习路径</RouterLink>
    <RouterLink to="/quiz">在线测评</RouterLink>
  </nav>
  <main>
    <RouterView />
  </main>
</template>

<style scoped>
nav {
  display: flex;
  gap: 1.5rem;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #e0e0e0;
}
nav a {
  text-decoration: none;
  color: #333;
}
nav a.router-link-exact-active {
  color: #2b6e5c;
  font-weight: 600;
}
main {
  padding: 1.5rem;
  max-width: 860px;
  margin: 0 auto;
}
</style>
