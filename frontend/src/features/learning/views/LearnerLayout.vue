<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import LearnerNav from '../components/LearnerNav.vue'

const route = useRoute()

const activeTab = computed<'courses' | 'flashcards' | 'dashboard'>(() => {
  const path = route.path
  if (path.includes('/flashcards')) return 'flashcards'
  if (path.includes('/dashboard')) return 'dashboard'
  return 'courses'
})
</script>

<template>
  <div class="learning-app flex flex-col h-screen overflow-hidden bg-lm-bg">
    <LearnerNav :active="activeTab" />
    <router-view class="flex-1 min-h-0" />
  </div>
</template>
