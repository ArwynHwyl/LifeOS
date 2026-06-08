<template>
  <div class="flex flex-col gap-4">
    <div>
      <MonoLabel :style="{ marginBottom: '6px' }">Title</MonoLabel>
      <input v-model="title" class="w-full box-border font-display text-[14px] px-3.5 py-2.5 border-2 border-lm-line-soft rounded-[10px] bg-lm-bg-soft outline-none text-lm-ink" />
    </div>
    
    <div>
      <MonoLabel :style="{ marginBottom: '6px' }">Content</MonoLabel>
      <RichTextEditor v-model="contentHtml" :sub-topic-id="subTopic?.id" />
    </div>

    <div>
      <MonoLabel :style="{ marginBottom: '6px' }">Mascot prompt</MonoLabel>
      <textarea
        v-model="mascotPrompt"
        class="w-full min-h-[76px] box-border resize-y font-display text-[14px] px-3.5 py-2.5 border-2 border-lm-line-soft rounded-[10px] bg-lm-bg-soft outline-none text-lm-ink"
        maxlength="1000"
        placeholder="Short quote shown beside the mascot in the learner lesson."
      />
    </div>

    <div v-if="intType !== 'NONE'">
      <MonoLabel :style="{ marginBottom: '6px' }">Interaction prompt</MonoLabel>
      <input v-model="prompt" class="w-full box-border font-display text-[14px] px-3.5 py-2.5 border-2 border-lm-line-soft rounded-[10px] bg-lm-bg-soft outline-none text-lm-ink" placeholder="What do learners need to do?" />
    </div>

    <InteractiveEditor
      :type="intType"
      :config="intConfig"
      @change="onConfigChange"
      @type-change="onTypeChange"
    />

    <div class="flex gap-2.5 pt-1">
      <button @click="onSave" :disabled="!title.trim()" class="inline-flex items-center gap-2 px-5 py-2.5 font-display text-[14px] font-bold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg cursor-pointer shadow-stamp-sm transition-opacity duration-150 disabled:cursor-not-allowed disabled:opacity-45">
        <AdminIcon name="check" :size="16" /> Save
      </button>
      <button @click="$emit('cancel')" class="inline-flex items-center gap-2 px-5 py-2.5 font-display text-[14px] font-bold border-2 border-lm-line rounded-full bg-lm-surface text-lm-ink cursor-pointer">
        Cancel
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import MonoLabel from '../MonoLabel.vue'
import AdminIcon from '../AdminIcon.vue'
import RichTextEditor from './RichTextEditor.vue'
import InteractiveEditor from './InteractiveEditor.vue'

const props = defineProps({
  subTopic: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['save', 'cancel'])

const title = ref(props.subTopic?.title ?? '')
const contentHtml = ref(props.subTopic?.contentHtml || props.subTopic?.content || '')
const mascotPrompt = ref(props.subTopic?.mascotPrompt ?? '')
const intType = ref(props.subTopic?.interactionType ?? 'NONE')
const prompt = ref(props.subTopic?.interactionPrompt ?? '')
const intConfig = ref(props.subTopic?.interactionConfig ? JSON.parse(JSON.stringify(props.subTopic.interactionConfig)) : null)

watch(() => props.subTopic, (newVal) => {
  title.value = newVal?.title ?? ''
  contentHtml.value = newVal?.contentHtml || newVal?.content || ''
  mascotPrompt.value = newVal?.mascotPrompt ?? ''
  intType.value = newVal?.interactionType ?? 'NONE'
  prompt.value = newVal?.interactionPrompt ?? ''
  intConfig.value = newVal?.interactionConfig ? JSON.parse(JSON.stringify(newVal.interactionConfig)) : null
}, { deep: true })

function onConfigChange(newConfig) {
  intConfig.value = newConfig
}

function onTypeChange(newType) {
  intType.value = newType
  intConfig.value = null
}

function onSave() {
  if (!title.value.trim()) return
  emit('save', {
    ...props.subTopic,
    title: title.value,
    contentHtml: contentHtml.value,
    mascotPrompt: mascotPrompt.value,
    interactionType: intType.value,
    interactionPrompt: prompt.value,
    interactionConfig: intConfig.value
  })
}
</script>
