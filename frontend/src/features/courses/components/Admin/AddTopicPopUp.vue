<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  close: []
  save: [topic: { name: string; content: string }]
}>()

const name = ref('')
const content = ref('')

function reset() { name.value = ''; content.value = '' }
function close() { emit('close') }

function onSubmit() {
  emit('save', { name: name.value.trim(), content: content.value.trim() })
  close()
}

function onKeydown(e: KeyboardEvent) { if (e.key === 'Escape') close() }

watch(() => props.open, (open) => { if (!open) reset() })
</script>

<template>
  <Teleport to="body">
    <Transition enter-active-class="transition duration-200 ease-out" enter-from-class="opacity-0" enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in" leave-from-class="opacity-100" leave-to-class="opacity-0">
      <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-4" @keydown="onKeydown">
        <div class="absolute inset-0 bg-slate-950/50 backdrop-blur-sm" aria-hidden="true" @click="close" />

        <Transition enter-active-class="transition duration-200 ease-out" enter-from-class="opacity-0 scale-95 translate-y-2"
          enter-to-class="opacity-100 scale-100 translate-y-0" leave-active-class="transition duration-150 ease-in"
          leave-from-class="opacity-100 scale-100 translate-y-0" leave-to-class="opacity-0 scale-95 translate-y-2">
          <div v-if="open" role="dialog" aria-modal="true" aria-labelledby="add-topic-title"
            class="relative z-10 flex max-h-[min(90vh,580px)] w-full max-w-lg flex-col overflow-hidden rounded-2xl bg-white shadow-2xl shadow-slate-900/20 ring-1 ring-slate-900/[0.06]"
            @click.stop>

            <header class="shrink-0 border-b border-slate-100 px-6 py-5">
              <div class="flex items-start justify-between gap-4">
                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-[#5b4cfa]/10">
                    <svg class="h-4 w-4 text-[#5b4cfa]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                      <polyline points="14 2 14 8 20 8" />
                      <line x1="12" y1="18" x2="12" y2="12" />
                      <line x1="9" y1="15" x2="15" y2="15" />
                    </svg>
                  </div>
                  <div>
                    <h2 id="add-topic-title" class="text-[15px] font-bold text-slate-900">New Topic</h2>
                    <p class="text-[11px] text-slate-400">Add a topic name and its content.</p>
                  </div>
                </div>
                <button type="button"
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                  aria-label="Close" @click="close">
                  <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M18 6 6 18M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </header>

            <form class="flex min-h-0 flex-1 flex-col" @submit.prevent="onSubmit">
              <div class="min-h-0 flex-1 space-y-4 overflow-y-auto px-6 py-5">
                <div>
                  <label for="new-topic-name" class="mb-1.5 block text-[12px] font-semibold text-slate-600">Topic name</label>
                  <input id="new-topic-name" v-model="name" type="text" required placeholder="e.g. Vertex Form"
                    class="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-[13px] text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-[#5b4cfa]/50 focus:bg-white focus:ring-2 focus:ring-[#5b4cfa]/10" />
                </div>
                <div>
                  <label for="new-topic-content" class="mb-1.5 block text-[12px] font-semibold text-slate-600">Content</label>
                  <textarea id="new-topic-content" v-model="content" rows="9" required placeholder="Lesson notes, examples, and explanations…"
                    class="w-full resize-y rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-[13px] text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-[#5b4cfa]/50 focus:bg-white focus:ring-2 focus:ring-[#5b4cfa]/10" />
                </div>
              </div>

              <footer class="flex shrink-0 gap-2.5 border-t border-slate-100 px-6 py-4">
                <button type="button"
                  class="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-[13px] font-semibold text-slate-600 transition hover:bg-slate-50"
                  @click="close">Cancel</button>
                <button type="submit"
                  class="flex-1 rounded-xl bg-[#5b4cfa] px-4 py-2.5 text-[13px] font-semibold text-white shadow-md shadow-[#5b4cfa]/25 transition hover:bg-[#4d3ee0]">
                  Add topic
                </button>
              </footer>
            </form>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
