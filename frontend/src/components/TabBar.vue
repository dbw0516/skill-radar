<script setup>
defineProps({
  tabs: { type: Array, required: true }, // [{ key, label }]
  modelValue: { type: String, required: true },
})
defineEmits(['update:modelValue'])
</script>

<template>
  <div class="tab-bar">
    <button
      v-for="t in tabs" :key="t.key"
      type="button"
      class="tab" :class="{ active: modelValue === t.key }"
      @click="$emit('update:modelValue', t.key)"
    >
      {{ t.label }}
    </button>
  </div>
</template>

<style scoped>
.tab-bar { display: flex; gap: 0.25rem; border-bottom: 1px solid #dbdee4; margin-bottom: 1.2rem; }
.tab {
  padding: 0.55rem 1rem;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 0.92rem;
  color: #5b6472;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
.tab:hover { color: #1c232e; }
.tab.active { color: #2b6e5c; border-bottom-color: #2b6e5c; font-weight: 600; }
</style>
