<script setup>
import { ref, onMounted, watch } from 'vue'
import { api } from '@/api/client'
import { useUserProfileStore } from '@/stores/userProfile'

// 对应①岗位推荐引擎的展示层：先列出全部岗位类别，选中一个后展示该类别下的具体招聘信息。
// 按"专业 + 意向"过滤的逻辑目前还没做，先展示全部类别。
const categories = ref([])
const postings = ref([])
const loading = ref(true)
const postingsLoading = ref(false)
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

async function selectCategory(id) {
  profile.setTargetCategory(id)
  postingsLoading.value = true
  try {
    const page = await api.listPostings(id, 0, 10)
    postings.value = page.content || []
  } catch (e) {
    error.value = e.message
  } finally {
    postingsLoading.value = false
  }
}

watch(
  () => profile.targetCategoryId,
  (id) => { if (id) selectCategory(id) },
  { immediate: true }
)
</script>

<template>
  <section>
    <h1>岗位推荐</h1>
    <p v-if="loading">加载中…</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <template v-else>
      <div class="chip-row">
        <button
          v-for="c in categories" :key="c.id"
          class="cat-chip" :class="{ active: profile.targetCategoryId === c.id }"
          @click="selectCategory(c.id)"
        >
          {{ c.name }}
        </button>
      </div>

      <div v-if="profile.targetCategoryId" class="postings">
        <p v-if="postingsLoading">加载岗位信息中…</p>
        <p v-else-if="!postings.length">这个类别暂时没有招聘信息。</p>
        <ul v-else>
          <li v-for="p in postings" :key="p.id" class="posting">
            <div class="posting-head">
              <strong>{{ p.title }}</strong>
              <span class="salary">{{ p.salaryText || '薪资未标注' }}</span>
            </div>
            <div class="posting-meta">{{ p.companyName }} · {{ p.location || '地点未标注' }}</div>
          </li>
        </ul>
        <RouterLink to="/gap" class="cta">看这个岗位的技能差距 →</RouterLink>
      </div>
    </template>
  </section>
</template>

<style scoped>
.chip-row { display: flex; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 1.5rem; }
.cat-chip { padding: 0.4rem 0.9rem; border-radius: 999px; border: 1px solid #dbdee4; background: #fff; cursor: pointer; font-size: 0.88rem; }
.cat-chip.active { background: #2b6e5c; border-color: #2b6e5c; color: #fff; }
.postings ul { list-style: none; padding: 0; }
.posting { padding: 0.7rem 0; border-bottom: 1px solid #eee; }
.posting-head { display: flex; justify-content: space-between; gap: 1rem; }
.salary { color: #ae5f1c; font-size: 0.9rem; white-space: nowrap; }
.posting-meta { color: #666; font-size: 0.85rem; margin-top: 0.15rem; }
.cta { display: inline-block; margin-top: 1rem; color: #2b6e5c; font-weight: 600; text-decoration: none; }
.error { color: #b3261e; }
</style>
