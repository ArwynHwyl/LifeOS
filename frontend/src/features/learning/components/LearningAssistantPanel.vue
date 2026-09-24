<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import ToraMascot from '@/components/tora/ToraMascot.vue'
import {
  getAssistantConversation,
  getAssistantWeaknesses,
  streamAssistantMessage,
  updateAssistantFeedback,
  type AssistantConversation,
  type AssistantFeedback,
  type AssistantMessage,
  type AssistantMode,
  type AssistantSuggestion,
  type AssistantWeakness,
} from '@/features/learning/services/learningAssistant'

const props = defineProps<{
  courseId: string | number
  subTopicId: number
  open: boolean
  selectedText?: string
}>()
const emit = defineEmits<{ 'update:open': [value: boolean]; 'clear-selection': [] }>()

const conversation = ref<AssistantConversation | null>(null)
const messages = ref<AssistantMessage[]>([])
const input = ref('')
const mode = ref<AssistantMode>('EXPLAIN')
const pendingSuggestionKey = ref<string | null>(null)
const loading = ref(false)
const sending = ref(false)
const error = ref('')
const conversationError = ref('')
const weaknesses = ref<AssistantWeakness[]>([])
const showWeaknesses = ref(false)
const weaknessesLoaded = ref(false)
const weaknessesLoading = ref(false)
const weaknessesError = ref('')
const feedbackPendingIds = ref(new Set<number>())
const showSuggestions = ref(false)
const messageList = ref<HTMLElement | null>(null)
const composerInput = ref<HTMLTextAreaElement | null>(null)
let controller: AbortController | null = null

watch(() => [props.open, props.subTopicId] as const, async ([isOpen]) => {
  controller?.abort()
  controller = null
  conversation.value = null
  messages.value = []
  input.value = ''
  pendingSuggestionKey.value = null
  showSuggestions.value = false
  error.value = ''
  conversationError.value = ''
  weaknesses.value = []
  showWeaknesses.value = false
  weaknessesLoaded.value = false
  weaknessesError.value = ''
  if (isOpen) await loadConversation()
}, { immediate: true })

watch(() => props.selectedText, (value) => {
  if (value) {
    mode.value = 'EXPLAIN'
    pendingSuggestionKey.value = null
  }
})

onBeforeUnmount(() => controller?.abort())

async function loadConversation() {
  loading.value = true
  conversationError.value = ''
  try {
    const assistantConversation = await getAssistantConversation(props.courseId, props.subTopicId)
    conversation.value = assistantConversation
    messages.value = conversation.value.messages
    showSuggestions.value = messages.value.length === 0
  } catch (err) {
    conversationError.value = err instanceof Error ? err.message : 'Unable to open Tora.'
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

async function loadWeaknesses(force = false) {
  if (weaknessesLoading.value || (weaknessesLoaded.value && !force)) return
  weaknessesLoading.value = true
  weaknessesError.value = ''
  try {
    weaknesses.value = await getAssistantWeaknesses()
    weaknessesLoaded.value = true
  } catch (err) {
    weaknessesError.value = err instanceof Error ? err.message : 'Unable to load topics to review.'
  } finally {
    weaknessesLoading.value = false
  }
}

async function toggleWeaknesses() {
  showWeaknesses.value = !showWeaknesses.value
  if (showWeaknesses.value) await loadWeaknesses()
}

function close() {
  emit('update:open', false)
}

async function askSuggestion(suggestion: AssistantSuggestion) {
  mode.value = suggestion.mode
  input.value = suggestion.message
  pendingSuggestionKey.value = suggestion.key
  showSuggestions.value = false
  await nextTick()
  composerInput.value?.focus()
}

function selectMode(nextMode: AssistantMode) {
  mode.value = nextMode
  pendingSuggestionKey.value = null
}

async function sendCustom() {
  const text = input.value.trim()
  if (!text) return
  const suggestionKey = pendingSuggestionKey.value || undefined
  input.value = ''
  pendingSuggestionKey.value = null
  await send({ message: text, suggestionKey, displayText: text })
}

async function send(payload: { message?: string; suggestionKey?: string; displayText: string }) {
  if (sending.value) return
  sending.value = true
  error.value = ''
  controller = new AbortController()
  const tempUserId = -Date.now()
  const tempAssistantId = tempUserId - 1
  messages.value.push({
    id: tempUserId, role: 'USER', mode: mode.value, content: payload.displayText,
    selectedText: props.selectedText || null, suggestionKey: payload.suggestionKey || null,
    status: 'COMPLETED', feedback: null, createdAt: new Date().toISOString(),
  })
  messages.value.push({
    id: tempAssistantId, role: 'ASSISTANT', mode: mode.value, content: '', selectedText: null,
    suggestionKey: null, status: 'PENDING', feedback: null, createdAt: new Date().toISOString(),
  })
  await scrollToBottom()
  try {
    await streamAssistantMessage(props.courseId, props.subTopicId, {
      mode: mode.value,
      message: payload.message,
      suggestionKey: payload.suggestionKey,
      selectedText: props.selectedText || undefined,
    }, {
      onStart(data) {
        const user = messages.value.find((item) => item.id === tempUserId)
        const assistant = messages.value.find((item) => item.id === tempAssistantId)
        if (user) user.id = data.userMessageId
        if (assistant) assistant.id = data.assistantMessageId
      },
      onDelta(text) {
        const assistant = messages.value.find((item) => item.id === tempAssistantId)
          ?? messages.value[messages.value.length - 1]
        if (assistant) assistant.content += text
        void scrollToBottom()
      },
      onComplete(data) {
        const assistant = messages.value.find((item) => item.id === data.assistantMessageId)
        if (assistant) {
          assistant.content = data.content
          assistant.status = 'COMPLETED'
        }
        weaknessesLoaded.value = false
        if (showWeaknesses.value) void loadWeaknesses(true)
      },
      onError(message) { throw new Error(message) },
    }, controller.signal)
    emit('clear-selection')
  } catch (err) {
    const assistant = messages.value.find((item) => item.id === tempAssistantId)
      ?? messages.value[messages.value.length - 1]
    if (assistant?.role === 'ASSISTANT') assistant.status = 'FAILED'
    if ((err as Error).name !== 'AbortError') {
      error.value = err instanceof Error ? err.message : 'Tora stopped responding. Please try again.'
    }
  } finally {
    sending.value = false
    controller = null
  }
}

async function setFeedback(message: AssistantMessage, feedback: AssistantFeedback) {
  if (feedbackPendingIds.value.has(message.id)) return
  feedbackPendingIds.value.add(message.id)
  try {
    const result = await updateAssistantFeedback(message.id, feedback)
    message.feedback = result.feedback
    weaknessesLoaded.value = false
    if (showWeaknesses.value) await loadWeaknesses(true)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unable to save feedback.'
  } finally {
    feedbackPendingIds.value.delete(message.id)
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight
}
</script>

<template>
  <button v-if="!open" class="tora-launcher" type="button" aria-label="Ask Tora" @click="emit('update:open', true)">
    <span>Ask Tora</span><ToraMascot crop="head" :size="50" :track="false" />
  </button>
  <Transition name="assistant-panel">
    <aside v-if="open" class="assistant-panel" aria-label="AI learning assistant">
      <header class="assistant-header">
        <div class="assistant-avatar"><ToraMascot crop="head" :size="38" :track="false" /></div>
        <div><strong>Tora</strong><span>AI LEARNING COMPANION</span></div>
        <button type="button" aria-label="Close assistant" @click="close">×</button>
      </header>

      <div ref="messageList" class="assistant-messages">
        <div v-if="loading" class="assistant-state">Opening your study notes...</div>
        <div v-else-if="conversationError" class="assistant-state assistant-state--error">
          <span>{{ conversationError }}</span>
          <button type="button" @click="loadConversation">Try again</button>
        </div>
        <template v-else>
          <div v-if="conversation" class="assistant-context" aria-label="Current lesson context">
            <span>{{ conversation.context.courseTitle }}</span><b aria-hidden="true">›</b>
            <span>{{ conversation.context.moduleTitle }}</span><b aria-hidden="true">›</b>
            <strong>{{ conversation.context.subTopicTitle }}</strong>
          </div>
          <button type="button" class="assistant-review-toggle" :aria-expanded="showWeaknesses" @click="toggleWeaknesses">
            <span>↻</span>
            <template v-if="weaknessesLoaded">
              {{ weaknesses.length }} {{ weaknesses.length === 1 ? 'topic' : 'topics' }} to review
            </template>
            <template v-else>Topics to review</template>
          </button>
          <div v-if="showWeaknesses" class="assistant-review-list">
            <div v-if="weaknessesLoading" class="assistant-review-state">Loading review topics...</div>
            <div v-else-if="weaknessesError" class="assistant-review-state assistant-review-state--error">
              <span>{{ weaknessesError }}</span>
              <button type="button" @click="loadWeaknesses(true)">Try again</button>
            </div>
            <div v-else-if="weaknesses.length === 0" class="assistant-review-state">
              No repeated topics to review yet.
            </div>
            <template v-else>
              <div v-for="item in weaknesses" :key="item.subTopicId">
                <strong>{{ item.subTopicTitle }}</strong>
                <span>{{ item.courseTitle }} · {{ item.moduleTitle }}</span>
                <span>Asked {{ item.questionCount }} times · Still unclear {{ item.notUnderstoodCount }} times</span>
              </div>
            </template>
          </div>
          <div v-if="conversation" class="assistant-bubble assistant-bubble--assistant">{{ conversation.greeting }}</div>
          <div v-if="selectedText" class="assistant-selection">
            <span>Selected text</span><p>“{{ selectedText }}”</p>
            <button type="button" @click="emit('clear-selection')">Clear</button>
          </div>
          <div v-for="message in messages" :key="message.id" class="assistant-message"
            :class="`assistant-message--${message.role.toLowerCase()}`">
            <div v-if="message.selectedText" class="assistant-message__quote">“{{ message.selectedText }}”</div>
            <div class="assistant-bubble" :class="`assistant-bubble--${message.role.toLowerCase()}`">
              <span v-if="message.status === 'PENDING' && !message.content" class="assistant-thinking"><i/><i/><i/></span>
              <span v-else-if="message.status === 'FAILED'">I couldn't finish that response. Please try again.</span>
              <span v-else>{{ message.content }}</span>
            </div>
            <div v-if="message.role === 'ASSISTANT' && message.status === 'COMPLETED'" class="assistant-feedback">
              <span>Was this helpful?</span>
              <button :class="{ active: message.feedback === 'HELPFUL' }"
                :aria-pressed="message.feedback === 'HELPFUL'"
                :disabled="feedbackPendingIds.has(message.id)"
                @click="setFeedback(message, 'HELPFUL')">Helpful</button>
              <button :class="{ active: message.feedback === 'NOT_UNDERSTOOD' }"
                :aria-pressed="message.feedback === 'NOT_UNDERSTOOD'"
                :disabled="feedbackPendingIds.has(message.id)"
                @click="setFeedback(message, 'NOT_UNDERSTOOD')">Still unclear</button>
            </div>
          </div>
        </template>
      </div>

      <div v-if="error" class="assistant-error">{{ error }}</div>
      <footer class="assistant-composer">
        <div v-if="conversation?.suggestions.length" class="assistant-quick-actions">
          <button type="button" class="assistant-quick-actions__toggle" :aria-expanded="showSuggestions"
            @click="showSuggestions = !showSuggestions">
            <span>Quick questions</span><span aria-hidden="true">{{ showSuggestions ? '−' : '+' }}</span>
          </button>
          <div v-if="showSuggestions" class="assistant-suggestions">
            <button v-for="suggestion in conversation?.suggestions" :key="suggestion.key" type="button"
              :disabled="sending" @click="askSuggestion(suggestion)">{{ suggestion.label }}</button>
          </div>
        </div>
        <div class="assistant-modes">
          <button :class="{ active: mode === 'EXPLAIN' }" @click="selectMode('EXPLAIN')">Explain</button>
          <button :class="{ active: mode === 'HINT' }" @click="selectMode('HINT')">Hint</button>
        </div>
        <form @submit.prevent="sendCustom">
          <textarea ref="composerInput" v-model="input" maxlength="1000" rows="2" :disabled="sending"
            placeholder="Ask about this lesson..." @input="pendingSuggestionKey = null"
            @keydown.enter.exact.prevent="sendCustom" />
          <button type="submit" :disabled="sending || !input.trim()" aria-label="Send question">↑</button>
        </form>
      </footer>
    </aside>
  </Transition>
</template>

<style scoped>
.tora-launcher{position:fixed;right:1.5rem;bottom:1.5rem;z-index:80;display:flex;align-items:center;gap:.55rem;height:60px;border:none;border-radius:999px;background:var(--color-lx-macaw);padding:.15rem .35rem .15rem 1.1rem;color:#fff;font-weight:800;font-family:var(--font-body);box-shadow:0 4px 0 var(--color-lx-macaw-dark);cursor:pointer;transition:transform .08s,box-shadow .08s}
.tora-launcher:active{transform:translateY(4px);box-shadow:0 0 0 transparent}
.tora-launcher .tora{flex-shrink:0}
.assistant-panel{position:fixed;right:1rem;bottom:1rem;z-index:110;display:grid;grid-template-rows:auto minmax(0,1fr) auto auto;width:min(410px,calc(100vw - 2rem));height:min(720px,calc(100vh - 2rem));overflow:hidden;border:none;border-radius:24px;background:#fff;box-shadow:0 32px 64px -24px rgba(0,0,0,0.35);color:var(--color-lx-ink);font-family:var(--font-body)}
.assistant-header{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:.7rem;background:var(--color-lx-macaw);padding:.7rem .9rem}
.assistant-avatar{width:42px;height:42px;border:none;border-radius:50%;background:#fff;overflow:hidden}
.assistant-avatar{display:grid;place-items:center}
.assistant-header strong{display:block;font-size:1rem;font-weight:800;color:#fff;font-family:var(--font-display)}
.assistant-header span{display:block;font-family:var(--font-mono);font-size:9px;font-weight:700;letter-spacing:.1em;color:rgba(255,255,255,.8)}
.assistant-header>button{width:32px;height:32px;border:none;border-radius:50%;background:rgba(255,255,255,.2);color:#fff;font-size:1.3rem;line-height:1;cursor:pointer}
.assistant-messages{min-height:0;overflow-y:auto;padding:1rem;background:var(--color-lx-surface-soft)}
.assistant-state{padding:2rem;text-align:center;font-weight:700}
.assistant-review-toggle{display:flex;width:100%;align-items:center;gap:.4rem;margin-bottom:.6rem;border:none;border-radius:12px;background:rgba(88,204,2,.12);padding:.5rem .65rem;color:var(--color-lx-feather-dark);font-size:.68rem;font-weight:800;cursor:pointer}
.assistant-review-list{display:grid;gap:.3rem;margin:-.25rem 0 .7rem}
.assistant-review-list div{display:grid;border-left:3px solid var(--color-lx-feather);background:#fff;border-radius:0 8px 8px 0;padding:.35rem .5rem}
.assistant-review-list strong{font-size:.7rem}
.assistant-review-list span{color:var(--color-lx-ink-faint);font-size:.58rem}
.assistant-message{display:flex;flex-direction:column;margin:.75rem 0}
.assistant-message--user{align-items:flex-end}
.assistant-message--assistant{align-items:flex-start}
.assistant-bubble{max-width:86%;border:none;border-radius:16px;padding:.7rem .8rem;white-space:pre-wrap;font-size:.83rem;line-height:1.5;box-shadow:0 1px 2px rgba(0,0,0,.06)}
.assistant-bubble--assistant{align-self:flex-start;background:#fff}
.assistant-bubble--user{background:rgba(28,176,246,.14);color:var(--color-lx-ink)}
.assistant-selection{position:relative;margin:.6rem 0;border-left:4px solid var(--color-lx-fox);background:#fff;border-radius:0 10px 10px 0;padding:.6rem .75rem;font-size:.75rem}
.assistant-selection span{font-family:var(--font-mono);font-size:9px;font-weight:800;text-transform:uppercase;color:var(--color-lx-fox-dark)}
.assistant-selection p{margin:.2rem 2.5rem 0 0;display:-webkit-box;overflow:hidden;-webkit-line-clamp:3;-webkit-box-orient:vertical}
.assistant-selection button{position:absolute;right:.4rem;top:.4rem;border:0;background:none;color:#dc2626;font-size:.7rem;font-weight:700;cursor:pointer}
.assistant-message__quote{max-width:85%;margin-bottom:.25rem;border-left:3px solid var(--color-lx-fox);padding:.25rem .5rem;color:var(--color-lx-ink-faint);font-size:.68rem}
.assistant-suggestions{display:flex;flex-wrap:wrap;gap:.4rem;margin:.8rem 0}
.assistant-suggestions button,.assistant-modes button,.assistant-feedback button{border:1px solid var(--color-lx-line);border-radius:999px;background:#fff;padding:.35rem .65rem;color:var(--color-lx-ink);font-size:.68rem;font-weight:700;cursor:pointer;transition:background .12s}
.assistant-suggestions button:hover{background:rgba(28,176,246,.1);border-color:var(--color-lx-macaw)}
.assistant-feedback{display:flex;align-items:center;flex-wrap:wrap;gap:.3rem;margin-top:.4rem;color:var(--color-lx-ink-faint);font-size:.6rem}
.assistant-feedback button{padding:.2rem .45rem;font-size:.6rem}
.assistant-feedback button.active{background:rgba(88,204,2,.14);border-color:var(--color-lx-feather)}
.assistant-feedback button:last-child.active{background:#fef2f2;border-color:#fca5a5}
.assistant-error{border-top:1px solid #fecaca;background:#fef2f2;padding:.45rem .8rem;color:#dc2626;font-size:.7rem;font-weight:600}
.assistant-composer{border-top:1px solid var(--color-lx-line);background:#fff;padding:.65rem}
.assistant-modes{display:flex;gap:.35rem;margin-bottom:.45rem}
.assistant-modes button.active{background:var(--color-lx-macaw);border-color:var(--color-lx-macaw);color:#fff}
.assistant-composer form{display:grid;grid-template-columns:1fr auto;gap:.4rem}
.assistant-composer textarea{resize:none;border:1px solid var(--color-lx-line);border-radius:12px;background:var(--color-lx-surface-soft);padding:.55rem .65rem;color:var(--color-lx-ink);font:inherit;font-size:.78rem;outline:none}
.assistant-composer form button{align-self:stretch;width:42px;border:none;border-radius:12px;background:var(--color-lx-macaw);color:#fff;font-size:1.2rem;font-weight:700;cursor:pointer;box-shadow:0 3px 0 var(--color-lx-macaw-dark);transition:transform .08s,box-shadow .08s}
.assistant-composer form button:active{transform:translateY(3px);box-shadow:0 0 0 transparent}
.assistant-composer form button:disabled{opacity:.35}
.assistant-thinking{display:flex;gap:.25rem;padding:.25rem}
.assistant-thinking i{width:6px;height:6px;border-radius:50%;background:var(--color-lx-ink-faint);animation:think 1s infinite alternate}
.assistant-thinking i:nth-child(2){animation-delay:.2s}
.assistant-thinking i:nth-child(3){animation-delay:.4s}
@keyframes think{to{transform:translateY(-5px);opacity:.35}}
.assistant-panel-enter-active,.assistant-panel-leave-active{transition:transform .22s ease,opacity .22s ease}
.assistant-panel-enter-from,.assistant-panel-leave-to{transform:translateY(18px) scale(.97);opacity:0}
@media(max-width:600px){.assistant-panel{inset:.5rem;width:auto;height:auto;border-radius:18px}.tora-launcher span{display:none}.tora-launcher{padding:.15rem}}
.assistant-quick-actions{margin-bottom:.5rem}
.assistant-quick-actions__toggle{display:flex;width:100%;align-items:center;justify-content:space-between;border:0;background:transparent;padding:.1rem .15rem;color:var(--color-lx-ink-soft);font-size:.65rem;font-weight:800;cursor:pointer}
.assistant-quick-actions .assistant-suggestions{gap:.35rem;margin:.4rem 0 .1rem}
.assistant-state{display:grid;justify-items:center;gap:.7rem}
.assistant-state--error{color:#dc2626}
.assistant-state button,.assistant-review-state button{border:1px solid var(--color-lx-line);border-radius:999px;background:#fff;padding:.3rem .7rem;color:var(--color-lx-ink);font-size:.68rem;font-weight:700;cursor:pointer}
.assistant-context{display:flex;align-items:center;flex-wrap:wrap;gap:.25rem;margin-bottom:.55rem;color:var(--color-lx-ink-faint);font-size:.6rem}
.assistant-context strong{color:var(--color-lx-ink)}
.assistant-context b{font-weight:500}
.assistant-review-list>div:not(.assistant-review-state){display:grid;border-left:3px solid var(--color-lx-feather);background:#fff;padding:.35rem .5rem}
.assistant-review-state{display:grid;justify-items:start;gap:.4rem;border-left:3px solid var(--color-lx-ink-faint);background:#fff;padding:.5rem;color:var(--color-lx-ink-faint);font-size:.65rem}
.assistant-review-state--error{border-left-color:var(--color-lx-fox);color:#dc2626}
</style>
