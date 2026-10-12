<script setup>
defineProps({
  tabs: { type: Array, required: true }, // [{ key, label }]
  modelValue: { type: String, required: true },
})
defineEmits(['update:modelValue'])
</script>

<template>
  <div class="tab-bar" role="tablist" aria-label="页面分区">
    <button
      v-for="t in tabs" :key="t.key"
      type="button"
      class="tab" :class="{ active: modelValue === t.key }"
      role="tab"
      :aria-selected="modelValue === t.key"
      :tabindex="modelValue === t.key ? 0 : -1"
      @click="$emit('update:modelValue', t.key)"
    >
      {{ t.label }}
    </button>
  </div>
</template>

<style scoped>
.tab-bar { display: inline-flex; flex-wrap: wrap; gap: 4px; margin: 22px 0 24px; padding: 5px; border: var(--border-thick); border-radius: 999px; background: var(--paper); box-shadow: var(--shadow-sm); }
/* 选择器都带 :not(:disabled)，压过 main.css 里全局 button 的悬停/按下效果 */
.tab, .tab:hover:not(:disabled), .tab:active:not(:disabled) {
  min-height: 40px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  color: var(--ink);
  background: transparent;
  box-shadow: none;
  transform: none;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 900;
}
.tab:hover:not(:disabled) { background: var(--color-soft); }
.tab.active, .tab.active:hover:not(:disabled) { color: var(--yellow); background: var(--ink); }
</style>
