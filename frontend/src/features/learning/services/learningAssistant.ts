import api from '@/services/api'

export type AssistantMode = 'EXPLAIN' | 'HINT'
export type AssistantFeedback = 'HELPFUL' | 'NOT_UNDERSTOOD'
export type AssistantMessageStatus = 'PENDING' | 'COMPLETED' | 'FAILED'

export interface AssistantSuggestion {
  key: string
  label: string
  mode: AssistantMode
  message: string
}

export interface AssistantMessage {
  id: number
  role: 'USER' | 'ASSISTANT'
  mode: AssistantMode | null
  content: string
  selectedText: string | null
  suggestionKey: string | null
  status: AssistantMessageStatus
  feedback: AssistantFeedback | null
  createdAt: string
}

export interface AssistantConversation {
  conversationId: number
  greeting: string
  context: {
    courseId: number
    courseTitle: string
    moduleId: number
    moduleTitle: string
    subTopicId: number
    subTopicTitle: string
  }
  suggestions: AssistantSuggestion[]
  messages: AssistantMessage[]
}

export interface AssistantWeakness {
  courseId: number
  courseTitle: string
  moduleId: number
  moduleTitle: string
  subTopicId: number
  subTopicTitle: string
  questionCount: number
  notUnderstoodCount: number
  lastAskedAt: string
}

export interface SendAssistantMessage {
  mode: AssistantMode
  message?: string
  suggestionKey?: string
  selectedText?: string
}

export async function getAssistantConversation(courseId: number | string, subTopicId: number) {
  const { data } = await api.get<AssistantConversation>(
    `/v1/learner/courses/${courseId}/subtopics/${subTopicId}/assistant`,
  )
  return data
}

export async function updateAssistantFeedback(messageId: number, feedback: AssistantFeedback) {
  const { data } = await api.put<{ messageId: number; feedback: AssistantFeedback }>(
    `/v1/learner/assistant/messages/${messageId}/feedback`,
    { feedback },
  )
  return data
}

export async function getAssistantWeaknesses() {
  const { data } = await api.get<AssistantWeakness[]>('/v1/learner/assistant/weaknesses')
  return data
}

interface StreamHandlers {
  onStart: (data: { conversationId: number; userMessageId: number; assistantMessageId: number }) => void
  onDelta: (text: string) => void
  onComplete: (data: { assistantMessageId: number; content: string; mode: AssistantMode }) => void
  onError: (message: string) => void
}

export async function streamAssistantMessage(
  courseId: number | string,
  subTopicId: number,
  payload: SendAssistantMessage,
  handlers: StreamHandlers,
  signal?: AbortSignal,
) {
  const baseUrl = String(api.defaults.baseURL || '/api').replace(/\/$/, '')
  const token = localStorage.getItem('token')
  const response = await fetch(
    `${baseUrl}/v1/learner/courses/${courseId}/subtopics/${subTopicId}/assistant/messages`,
    {
      method: 'POST',
      signal,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(payload),
    },
  )
  if (!response.ok || !response.body) {
    const body = await response.json().catch(() => null) as { error?: string } | null
    throw new Error(body?.error || 'Unable to reach the learning assistant.')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  const dispatch = (block: string) => {
    let event = ''
    const dataLines: string[] = []
    for (const line of block.split(/\r?\n/)) {
      if (line.startsWith('event:')) event = line.slice(6).trim()
      if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
    }
    if (!event || !dataLines.length) return
    const data = JSON.parse(dataLines.join('\n'))
    if (event === 'message-start') handlers.onStart(data)
    else if (event === 'content-delta') handlers.onDelta(data.text)
    else if (event === 'message-complete') handlers.onComplete(data)
    else if (event === 'error') handlers.onError(data.message || 'The assistant stopped responding.')
  }

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value, { stream: !done }).replace(/\r\n/g, '\n')
    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      dispatch(buffer.slice(0, boundary))
      buffer = buffer.slice(boundary + 2)
      boundary = buffer.indexOf('\n\n')
    }
    if (done) break
  }
  if (buffer.trim()) dispatch(buffer)
}
