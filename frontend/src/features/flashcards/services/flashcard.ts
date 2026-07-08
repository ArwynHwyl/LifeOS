import api from '@/services/api'

export interface FlashcardDeckSummaryDto {
  id: number
  title: string
  description: string | null
  tag: string
  cardCount: number
  sortOrder: number
}

export interface FlashcardCardDto {
  id: number
  front: string
  backText: string
  note: string | null
  example: string | null
  tags: string[]
}

export interface FlashcardDeckDetailDto {
  id: number
  title: string
  description: string | null
  tag: string
  cards: FlashcardCardDto[]
}

export interface SrsCardDto {
  srsCardId: number
  cardId: number
  front: string
  backText: string
  note: string | null
  example: string | null
  tags: string[]
  deckTitle: string
  deckTag: string
  dueAt: string
}

export interface SrsQueuePageDto {
  items: SrsCardDto[]
  page: number
  size: number
  totalDueNow: number
  totalTracked: number
  totalDueLaterToday: number
}

export type SrsOutcome = 'AGAIN' | 'HARD' | 'GOOD' | 'EASY'

export interface SrsReviewResultDto {
  srsCardId: number
  outcome: SrsOutcome
  nextDueAt: string
}

export async function listDecks() {
  const { data } = await api.get<FlashcardDeckSummaryDto[]>('/v1/learner/flashcards/decks')
  return data
}

export async function getDeckDetail(deckId: number) {
  const { data } = await api.get<FlashcardDeckDetailDto>(`/v1/learner/flashcards/decks/${deckId}`)
  return data
}

export async function getSrsQueuePage(page = 0, size = 5) {
  const { data } = await api.get<SrsQueuePageDto>('/v1/learner/flashcards/srs/queue', {
    params: { page, size },
  })
  return data
}

export async function getSrsDueSession() {
  const { data } = await api.get<SrsCardDto[]>('/v1/learner/flashcards/srs/due')
  return data
}

export async function submitSrsReview(srsCardId: number, outcome: SrsOutcome) {
  const { data } = await api.post<SrsReviewResultDto>(`/v1/learner/flashcards/srs/${srsCardId}/review`, { outcome })
  return data
}
