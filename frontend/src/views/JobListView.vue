<script setup>
import { ref, onMounted, watch } from 'vue'
import { api } from '@/api/client'
import { useUserProfileStore } from '@/stores/userProfile'
import { useAuthStore } from '@/stores/auth'
import TabBar from '@/components/TabBar.vue'

// 对应①岗位推荐引擎的展示层：先列出全部岗位类别，选中一个后展示该类别下的具体招聘信息。
// 有意向就业地区的话，地点匹配的岗位会排在前面（后端 preferLocation 参数）。
// "我的收藏"是单独一个 tab，不跟岗位浏览挤在一起。
const activeTab = ref('browse')
const categories = ref([])
const postings = ref([])
const loading = ref(true)
const postingsLoading = ref(false)
const error = ref('')
const profile = useUserProfileStore()
const auth = useAuthStore()

// postingId -> 我和这条岗位的技能匹配度（只有抽取出技能的 JD 才有）
const matches = ref({})

const favorites = ref([])
const favoritesLoading = ref(false)
const favoritesError = ref('')

onMounted(async () => {
  try {
    categories.value = await api.listJobCategories()
  } catch (e) {
    error.value = e.message + '——先确认后端是否已启动（mvn spring-boot:run）'
  } finally {
    loading.value = false
  }
})

// 坑：这里以前是一个函数同时做两件事（设置目标类别 + 拉岗位列表），点击分类 chip
// 时直接调用它，而下面的 watch 又在监听 targetCategoryId 变化——setTargetCategory
// 同步改了 store 里的值，watch 马上也跟着触发，同一次点击实际上发了两次一模一样的
// /postings 请求（用 fetch 打日志实测过）。现在拆开：点击只管"选中哪个类别"，
// 拉数据只归 watch 一处管，不会重复触发。
async function loadPostings(id) {
  postingsLoading.value = true
  try {
    const page = await api.listPostings(id, { preferLocation: auth.user?.targetLocation, size: 10 })
    postings.value = page.content || []
    matches.value = {}
    if (auth.isLoggedIn && postings.value.length) {
      api.postingMatches(auth.userId, postings.value.map((p) => p.id))
        .then((m) => { matches.value = m })
        .catch(() => {})
    }
  } catch (e) {
    error.value = e.message
  } finally {
    postingsLoading.value = false
  }
}

function selectCategory(id) {
  profile.setTargetCategory(id)
}

watch(
  () => profile.targetCategoryId,
  (id) => { if (id) loadPostings(id) },
  { immediate: true }
)

async function loadFavorites() {
  if (!auth.isLoggedIn) return
  favoritesLoading.value = true
  favoritesError.value = ''
  try {
    favorites.value = await api.listFavorites(auth.userId)
  } catch (e) {
    favoritesError.value = e.message
  } finally {
    favoritesLoading.value = false
  }
}
watch(activeTab, (t) => { if (t === 'favorites') loadFavorites() })

function isLocationMatch(loc) {
  const target = auth.user?.targetLocation
  return target && loc && loc.includes(target)
}
</script>

<template>
  <section>
    <div class="page-header">
      <div>
        <h1>岗位推荐</h1>
        <p class="lead">先选一个目标岗位，下面列出这类岗位的真实招聘信息，以及你和每条 JD 的技能匹配度。</p>
      </div>
      <RouterLink v-if="profile.targetCategoryId" to="/quiz" class="quiz-entry"><span class="ai-badge">AI</span>进入在线测评 →</RouterLink>
    </div>
    <TabBar
      :tabs="[{ key: 'browse', label: '岗位浏览' }, { key: 'favorites', label: '我的收藏' }]"
      v-model="activeTab"
    />

    <template v-if="activeTab === 'browse'">
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

        <p v-if="auth.isLoggedIn && !auth.user?.targetLocation" class="hint">
          去<RouterLink to="/profile">个人中心</RouterLink>填一下意向就业地区，能让地点匹配的岗位优先展示。
        </p>

        <div v-if="profile.targetCategoryId" class="postings">
          <p v-if="postingsLoading">加载岗位信息中…</p>
          <p v-else-if="!postings.length">这个类别暂时没有招聘信息。</p>
          <TransitionGroup v-else tag="ul" name="stagger">
            <li v-for="p in postings" :key="p.id" class="posting">
              <RouterLink :to="`/postings/${p.id}`" class="posting-link">
                <div class="posting-head">
                  <strong>{{ p.title }}</strong>
                  <span class="salary">{{ p.salaryText || '薪资未标注' }}</span>
                </div>
                <div class="posting-meta">
                  {{ p.companyName }} · {{ p.location || '地点未标注' }}
                  <span v-if="isLocationMatch(p.location)" class="match">地点匹配</span>
                  <span
                    v-if="matches[p.id] != null" class="skill-match" :class="{ good: matches[p.id] >= 70 }"
                    title="你已掌握的技能覆盖了这条 JD 要求技能的比例"
                  >技能匹配 {{ matches[p.id] }}%</span>
                </div>
              </RouterLink>
            </li>
          </TransitionGroup>
          <RouterLink to="/gap" class="cta">看这个岗位的技能差距 →</RouterLink>
        </div>
      </template>
    </template>

    <template v-else>
      <p v-if="!auth.isLoggedIn" class="hint"><RouterLink to="/login">登录</RouterLink>后可以收藏岗位、方便以后回来看。</p>
      <p v-else-if="favoritesLoading">加载中…</p>
      <p v-else-if="favoritesError" class="error">{{ favoritesError }}</p>
      <p v-else-if="!favorites.length" class="hint">还没有收藏的岗位——去岗位详情页点"收藏"试试。</p>
      <TransitionGroup v-else tag="ul" name="stagger">
        <li v-for="p in favorites" :key="p.id" class="posting">
          <RouterLink :to="`/postings/${p.id}`" class="posting-link">
            <div class="posting-head">
              <strong>{{ p.title }}</strong>
              <span class="salary">{{ p.salaryText || '薪资未标注' }}</span>
            </div>
            <div class="posting-meta">{{ p.companyName }} · {{ p.location || '地点未标注' }}</div>
          </RouterLink>
        </li>
      </TransitionGroup>
    </template>
  </section>
</template>

<style scoped>
.page-header { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 16px 20px; margin-bottom: 8px; }
.page-header h1 {
  margin: 0;
  color: var(--ink);
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 3.2rem);
  font-weight: 400;
  line-height: 1.1;
}
.lead { margin: 8px 0 0; color: var(--color-muted); font-weight: 700; }
.quiz-entry {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 52px;
  padding: 0 22px 0 14px;
  border: var(--border-thick);
  border-radius: var(--radius-md);
  color: var(--paper);
  background: var(--ink);
  box-shadow: 5px 5px 0 var(--ai);
  font-size: 1.05rem;
  font-weight: 900;
  text-decoration: none;
  transition: transform 140ms ease, box-shadow 140ms ease;
}
.quiz-entry:hover { color: var(--paper); text-decoration: none; transform: translate(-2px, -2px); box-shadow: 7px 7px 0 var(--ai); }
.chip-row { margin: 8px 0 22px; }
.cat-chip { font-size: 0.95rem; }
.hint { margin-bottom: 18px; padding: 12px 16px; border: var(--border-thick); border-radius: var(--radius-md); background: var(--color-soft); color: var(--ink); font-size: 0.95rem; font-weight: 700; }
.postings ul, ul { list-style: none; padding: 0; margin: 0; }
.posting-link:hover .posting-head strong { text-decoration: underline; text-decoration-thickness: 3px; text-decoration-color: var(--yellow); text-underline-offset: 3px; }
.posting-head { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.posting-head strong { color: var(--ink); font-size: 1.12rem; font-weight: 900; }
.salary { flex: 0 0 auto; padding: 2px 12px; border: 2.5px solid var(--ink); border-radius: 999px; background: var(--yellow); color: var(--ink); font-size: 0.92rem; font-weight: 900; white-space: nowrap; font-variant-numeric: tabular-nums; }
.posting-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 8px; color: var(--color-muted); font-size: 0.92rem; font-weight: 700; }
.match, .skill-match { padding: 2px 10px; border: 2.5px solid var(--ink); border-radius: 999px; color: var(--ink); font-size: 0.8rem; font-weight: 900; }
.match { background: var(--blue); }
.skill-match { background: var(--paper); }
.skill-match.good { background: var(--green); box-shadow: 2px 2px 0 var(--ink); }
.error { padding: 12px 14px; border: var(--border-thick); border-radius: var(--radius-md); background: #ffe3e8; }
@media (max-width: 560px) { .posting-head { align-items: flex-start; flex-direction: column; gap: 6px; } }
</style>
