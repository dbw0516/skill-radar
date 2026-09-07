<script setup>
import { ref, onMounted } from 'vue'
import { api } from '@/api/client'
import { useUserProfileStore } from '@/stores/userProfile'

// 对应①岗位推荐引擎的展示层：目前先直接列出全部岗位类别，
// 后续按"专业 + 意向"过滤的逻辑放在后端 JobCategoryController 里做。
const categories = ref([])
const loading = ref(true)
const error = ref('')
const profile = useUserProfileStore()

onMounted(async () => {
  try {
    categories.value = await api.listJobCategories()
  } catch (e) {
    error.value = e.message + '——先确认后端是否已启动（mvn spring-boot:run）'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section>
    <h1>岗位推荐</h1>
    <p v-if="loading">加载中…</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <ul v-else>
      <li v-for="c in categories" :key="c.id">
        <button @click="profile.setTargetCategory(c.id)">
          {{ c.name }}
          <span v-if="profile.targetCategoryId === c.id">（当前目标）</span>
        </button>
        <p class="desc">{{ c.description }}</p>
      </li>
    </ul>
  </section>
</template>

<style scoped>
ul { list-style: none; padding: 0; }
li { margin-bottom: 1rem; }
.desc { color: #666; font-size: 0.9rem; margin: 0.25rem 0 0; }
.error { color: #b3261e; }
</style>
