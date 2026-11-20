<template>
  <el-tag :type="meta.type as any" effect="light" size="small" round>{{ meta.label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { statusMeta } from '../utils/format'

const props = defineProps<{ status?: string }>()
const meta = computed(() => statusMeta(props.status || ''))
</script>
