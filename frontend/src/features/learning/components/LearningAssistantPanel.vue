<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import toraMascotUrl from '@/assets/tora-mascot.svg'
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
const loading = ref(false)
const sending = ref(false)
const error = ref('')
const weaknesses = ref<AssistantWeakness[]>([])
const showWeaknesses = ref(false)
const messageList = ref<HTMLElement | null>(null)
let controller: AbortController | null = null

watch(() => [props.open, props.subTopicId] as const, async ([isOpen]) => {
  controller?.abort()
  controller = null
  conversation.value = null
  messages.value = []
  error.value = ''
  if (isOpen) await loadConversation()
}, { immediate: true })

watch(() => props.selectedText, (value) => {
  if (value) mode.value = 'EXPLAIN'
})

onBeforeUnmount(() => controller?.abort())

async function loadConversation() {
  loading.value = true
  try {
    const [assistantConversation, weaknessSummary] = await Promise.all([
      getAssistantConversation(props.courseId, props.subTopicId),
      getAssistantWeaknesses(),
    ])
    conversation.value = assistantConversation
    weaknesses.value = weaknessSummary
    messages.value = conversation.value.messages
    await scrollToBottom()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unable to open Tora.'
  } finally {
    loading.value = false
  }
}

function close() {
  emit('update:open', false)
}

async function askSuggestion(suggestion: AssistantSuggestion) {
  mode.value = suggestion.mode
  await send({ suggestionKey: suggestion.key, displayText: suggestion.label })
}

async function sendCustom() {
  const text = input.value.trim()
  if (!text) return
  input.value = ''
  await send({ message: text, displayText: text })
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
  try {
    const result = await updateAssistantFeedback(message.id, feedback)
    message.feedback = result.feedback
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unable to save feedback.'
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight
}
</script>

<template>
  <button v-if="!open" class="tora-launcher" type="button" aria-label="Ask Tora" @click="emit('update:open', true)">
    <span>Ask Tora</span><img :src="toraMascotUrl" alt="" />
  </button>
  <Transition name="assistant-panel">
    <aside v-if="open" class="assistant-panel" aria-label="AI learning assistant">
      <header class="assistant-header">
        <div class="assistant-avatar"><img :src="toraMascotUrl" alt="" /></div>
        <div><strong>Tora</strong><span>AI LEARNING COMPANION</span></div>
        <button type="button" aria-label="Close assistant" @click="close">×</button>
      </header>

      <div ref="messageList" class="assistant-messages">
        <div v-if="loading" class="assistant-state">Opening your study notes...</div>
        <template v-else>
          <button v-if="weaknesses.length" type="button" class="assistant-review-toggle" @click="showWeaknesses = !showWeaknesses">
            <span>↻</span> {{ weaknesses.length }} {{ weaknesses.length === 1 ? 'topic' : 'topics' }} to review
          </button>
          <div v-if="showWeaknesses && weaknesses.length" class="assistant-review-list">
            <div v-for="item in weaknesses.slice(0, 3)" :key="item.subTopicId">
              <strong>{{ item.subTopicTitle }}</strong>
              <span>Asked {{ item.questionCount }} times · Still unclear {{ item.notUnderstoodCount }} times</span>
            </div>
          </div>
          <div class="assistant-bubble assistant-bubble--assistant">{{ conversation?.greeting }}</div>
          <div v-if="selectedText" class="assistant-selection">
            <span>Selected text</span><p>“{{ selectedText }}”</p>
            <button type="button" @click="emit('clear-selection')">Clear</button>
          </div>
          <div v-if="messages.length === 0" class="assistant-suggestions">
            <button v-for="suggestion in conversation?.suggestions" :key="suggestion.key" type="button"
              :disabled="sending" @click="askSuggestion(suggestion)">{{ suggestion.label }}</button>
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
              <button :class="{ active: message.feedback === 'HELPFUL' }" @click="setFeedback(message, 'HELPFUL')">Helpful</button>
              <button :class="{ active: message.feedback === 'NOT_UNDERSTOOD' }" @click="setFeedback(message, 'NOT_UNDERSTOOD')">Still unclear</button>
            </div>
          </div>
        </template>
      </div>

      <div v-if="error" class="assistant-error">{{ error }}</div>
      <footer class="assistant-composer">
        <div class="assistant-modes">
          <button :class="{ active: mode === 'EXPLAIN' }" @click="mode = 'EXPLAIN'">Explain</button>
          <button :class="{ active: mode === 'HINT' }" @click="mode = 'HINT'">Hint</button>
        </div>
        <form @submit.prevent="sendCustom">
          <textarea v-model="input" maxlength="1000" rows="2" :disabled="sending" placeholder="Ask about this lesson..."
            @keydown.enter.exact.prevent="sendCustom" />
          <button type="submit" :disabled="sending || !input.trim()" aria-label="Send question">↑</button>
        </form>
      </footer>
    </aside>
  </Transition>
</template>

<style scoped>
.tora-launcher{position:fixed;right:1.5rem;bottom:1.5rem;z-index:80;display:flex;align-items:center;gap:.55rem;height:64px;border:2px solid #1d1b17;border-radius:999px;background:#ffd333;padding:.15rem .35rem .15rem 1rem;color:#1d1b17;font-weight:950;box-shadow:4px 5px 0 #1d1b17;cursor:pointer}.tora-launcher img{width:54px;height:54px;object-fit:contain}.assistant-panel{position:fixed;right:1rem;bottom:1rem;z-index:110;display:grid;grid-template-rows:auto minmax(0,1fr) auto auto;width:min(410px,calc(100vw - 2rem));height:min(720px,calc(100vh - 2rem));overflow:hidden;border:2px solid #1d1b17;border-radius:24px;background:#fffdf8;box-shadow:7px 9px 0 #1d1b17;color:#1d1b17}.assistant-header{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:.7rem;border-bottom:2px solid #1d1b17;background:#ffd333;padding:.7rem .9rem}.assistant-avatar{width:45px;height:45px;border:2px solid #1d1b17;border-radius:50%;background:#fff;overflow:hidden}.assistant-avatar img{width:100%;height:100%;object-fit:contain}.assistant-header strong{display:block;font-size:1rem;font-weight:950}.assistant-header span{display:block;font-family:ui-monospace,monospace;font-size:9px;font-weight:900;letter-spacing:.1em}.assistant-header>button{width:34px;height:34px;border:2px solid #1d1b17;border-radius:50%;background:#fff;font-size:1.4rem;line-height:1;cursor:pointer}.assistant-messages{min-height:0;overflow-y:auto;padding:1rem;background:radial-gradient(circle,#e8dfce 1px,transparent 1px);background-size:17px 17px}.assistant-state{padding:2rem;text-align:center;font-weight:850}.assistant-review-toggle{display:flex;width:100%;align-items:center;gap:.4rem;margin-bottom:.6rem;border:1.5px solid #1d1b17;border-radius:10px;background:#d3edc9;padding:.45rem .6rem;color:#1d1b17;font-size:.68rem;font-weight:900;cursor:pointer}.assistant-review-list{display:grid;gap:.3rem;margin:-.25rem 0 .7rem}.assistant-review-list div{display:grid;border-left:3px solid #3a7d44;background:#fff;padding:.35rem .5rem}.assistant-review-list strong{font-size:.7rem}.assistant-review-list span{color:#6b6660;font-size:.58rem}.assistant-message{display:flex;flex-direction:column;margin:.75rem 0}.assistant-message--user{align-items:flex-end}.assistant-message--assistant{align-items:flex-start}.assistant-bubble{max-width:86%;border:1.5px solid #1d1b17;border-radius:16px;padding:.7rem .8rem;white-space:pre-wrap;font-size:.83rem;line-height:1.5;box-shadow:2px 2px 0 #1d1b17}.assistant-bubble--assistant{align-self:flex-start;background:#fff}.assistant-bubble--user{background:#fff0a8}.assistant-selection{position:relative;margin:.6rem 0;border-left:4px solid #c44a1a;background:#fff;padding:.6rem .75rem;font-size:.75rem}.assistant-selection span{font-family:ui-monospace,monospace;font-size:9px;font-weight:900;text-transform:uppercase}.assistant-selection p{margin:.2rem 2.5rem 0 0;display:-webkit-box;overflow:hidden;-webkit-line-clamp:3;-webkit-box-orient:vertical}.assistant-selection button{position:absolute;right:.4rem;top:.4rem;border:0;background:none;color:#8c3322;font-size:.7rem;font-weight:800;cursor:pointer}.assistant-message__quote{max-width:85%;margin-bottom:.25rem;border-left:3px solid #c44a1a;padding:.25rem .5rem;color:#6b6660;font-size:.68rem}.assistant-suggestions{display:flex;flex-wrap:wrap;gap:.4rem;margin:.8rem 0}.assistant-suggestions button,.assistant-modes button,.assistant-feedback button{border:1.5px solid #1d1b17;border-radius:999px;background:#fff;padding:.35rem .6rem;color:#1d1b17;font-size:.68rem;font-weight:850;cursor:pointer}.assistant-suggestions button:hover{background:#ffd333}.assistant-feedback{display:flex;align-items:center;flex-wrap:wrap;gap:.3rem;margin-top:.4rem;color:#777066;font-size:.6rem}.assistant-feedback button{padding:.2rem .4rem;font-size:.6rem}.assistant-feedback button.active{background:#d3edc9}.assistant-feedback button:last-child.active{background:#ffd5d0}.assistant-error{border-top:1px solid #e5b1a8;background:#fff0ed;padding:.45rem .8rem;color:#8c3322;font-size:.7rem;font-weight:750}.assistant-composer{border-top:2px solid #1d1b17;background:#f6f0e7;padding:.65rem}.assistant-modes{display:flex;gap:.35rem;margin-bottom:.45rem}.assistant-modes button.active{background:#ffd333}.assistant-composer form{display:grid;grid-template-columns:1fr auto;gap:.4rem}.assistant-composer textarea{resize:none;border:1.5px solid #1d1b17;border-radius:12px;background:#fff;padding:.55rem .65rem;color:#1d1b17;font:inherit;font-size:.78rem;outline:none}.assistant-composer form button{align-self:stretch;width:42px;border:2px solid #1d1b17;border-radius:12px;background:#1d1b17;color:#fff;font-size:1.2rem;font-weight:900;cursor:pointer}.assistant-composer form button:disabled{opacity:.35}.assistant-thinking{display:flex;gap:.25rem;padding:.25rem}.assistant-thinking i{width:6px;height:6px;border-radius:50%;background:#1d1b17;animation:think 1s infinite alternate}.assistant-thinking i:nth-child(2){animation-delay:.2s}.assistant-thinking i:nth-child(3){animation-delay:.4s}@keyframes think{to{transform:translateY(-5px);opacity:.35}}.assistant-panel-enter-active,.assistant-panel-leave-active{transition:transform .22s ease,opacity .22s ease}.assistant-panel-enter-from,.assistant-panel-leave-to{transform:translateY(18px) scale(.97);opacity:0}@media(max-width:600px){.assistant-panel{inset:.5rem;width:auto;height:auto;border-radius:18px}.tora-launcher span{display:none}.tora-launcher{padding:.15rem}.assistant-panel{box-shadow:4px 5px 0 #1d1b17}}
</style>
