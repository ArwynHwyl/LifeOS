<script setup lang="ts">
import { computed } from 'vue'
import SubTopicEditor from '@/features/courses/components/Admin/editor/SubTopicEditor.vue'
import type {
  AdminSubTopicDto,
  InteractionType,
} from '@/features/courses/services/adminCourses'

const props = defineProps<{
  subTopic: AdminSubTopicDto
}>()

const emit = defineEmits<{
  save: [{
    title: string
    content: string
    interactionType: InteractionType
    interactionPrompt: string | null
    interactionConfig: string | null
  }]
  cancel: []
  error: [message: string]
}>()

const editableSubTopic = computed(() => ({
  ...props.subTopic,
  interactionConfig: parseConfig(props.subTopic.interactionConfig),
}))

function handleSave(payload: {
  title: string
  contentHtml?: string
  content?: string
  interactionType: InteractionType
  interactionPrompt?: string | null
  interactionConfig?: unknown
}) {
  emit('save', {
    title: payload.title.trim(),
    content: payload.contentHtml ?? payload.content ?? '',
    interactionType: payload.interactionType,
    interactionPrompt: payload.interactionType === 'NONE' ? null : payload.interactionPrompt?.trim() || null,
    interactionConfig: payload.interactionType === 'NONE' ? null : stringifyConfig(payload.interactionConfig),
  })
}

function parseConfig(value: string | null) {
  if (!value) return null
  try {
    return JSON.parse(value) as unknown
  } catch {
    return value
  }
}

function stringifyConfig(value: unknown) {
  if (value == null || value === '') return null
  if (typeof value === 'string') {
    try {
      JSON.parse(value)
      return value
    } catch {
      emit('error', 'Interaction config must be valid JSON.')
      return null
    }
  }
  return JSON.stringify(value)
}
</script>

<template>
  <SubTopicEditor
    :sub-topic="editableSubTopic"
    @save="handleSave"
    @cancel="emit('cancel')"
  />
</template>
