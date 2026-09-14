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
.tab-bar { display: inline-flex; flex-wrap: wrap; gap: 4px; padding: 4px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface-raised); margin: 18px 0 22px; }
.tab {
  min-height: 40px;
  padding: 0.55rem 1rem;
  border: 1px solid transparent;
  border-radius: calc(var(--radius-md) - 2px);
  background: transparent;
  cursor: pointer;
  font-size: 0.92rem;
  color: var(--color-muted);
  font-weight: 650;
}
.tab:hover { color: var(--color-heading); background: var(--color-soft); box-shadow: none; }
.tab.active { color: var(--color-on-primary); border-color: var(--color-primary); background: var(--color-primary); box-shadow: 0 3px 8px color-mix(in srgb, var(--color-primary) 18%, transparent); }
</style>
