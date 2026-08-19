import api from '@/services/api'
import type { InteractionType } from '@/features/courses/services/adminCourses'
import type { GamificationRewardDto } from '@/features/gamified/services/gamification'

export interface PublishedCourseDetailDto {
  id: number
  title: string
  description: string | null
  publishedAt: string
  modules: PublishedModuleDto[]
}

export interface PublishedModuleDto {
  id: number
  title: string
  description: string | null
  sortOrder: number
  interactionType: InteractionType
  interactionPrompt: string | null
  interactionConfig: string | null
  subTopics: PublishedSubTopicDto[]
}

export interface PublishedSubTopicDto {
  id: number
  title: string
  content: string | null
  contentHtml: string | null
  mascotPrompt: string | null
  sortOrder: number
  interactionType: InteractionType
  interactionPrompt: string | null
  interactionConfig: string | null
  interactiveProgress: InteractiveProgressDto | null
}

export type InteractiveProgressStatus = 'NOT_STARTED' | 'TRIED' | 'MASTERED'

export interface InteractiveProgressDto {
  subTopicId: number
  status: InteractiveProgressStatus
  attemptCount: number
  masteredAt: string | null
  updatedAt: string | null
  reward?: GamificationRewardDto
}

export interface LogicStepSubmissionDto {
  lawId?: string
  from?: string
  to?: string
}

export type LogicAttemptRequest =
  | {
      kind: 'SIMPLIFY'
      answer: string
      steps?: LogicStepSubmissionDto[]
    }
  | {
      kind: 'CIRCUIT'
      inputs: Record<string, boolean>
      answer: boolean
    }

export interface LogicAttemptResponse {
  subTopicId: number
  kind: 'SIMPLIFY' | 'CIRCUIT'
  correct: boolean
  status: Exclude<InteractiveProgressStatus, 'NOT_STARTED'>
  attemptCount: number
  masteredAt: string | null
  updatedAt: string | null
  feedback: string
  details: Record<string, unknown>
  reward: GamificationRewardDto
}

export type InteractiveAttemptRequest =
  | { answer: string }
  | { values: Record<string, number> }
  | { regionAnswers: Record<string, number> }
  | LogicAttemptRequest

export interface InteractiveAttemptResponse {
  subTopicId: number
  interactionType: InteractionType
  correct: boolean
  status: Exclude<InteractiveProgressStatus, 'NOT_STARTED'>
  attemptCount: number
  masteredAt: string | null
  updatedAt: string | null
  feedback: string
  details: Record<string, unknown>
  reward: GamificationRewardDto
}

export interface PublishedCourseSummaryDto {
  id: number
  title: string
  description: string | null
  coverId: string | null
  publishedAt: string
}

export async function listPublishedCourses() {
  const { data } = await api.get<PublishedCourseSummaryDto[]>('/v1/learner/courses')
  return data
}

export async function getPublishedCourse(courseId: number | string) {
  const { data } = await api.get<PublishedCourseDetailDto>(`/v1/learner/courses/${courseId}`)
  return data
}

export async function submitLogicAttempt(
  courseId: number | string,
  subTopicId: number | string,
  payload: LogicAttemptRequest,
) {
  const { data } = await api.post<LogicAttemptResponse>(
    `/v1/learner/courses/${courseId}/subtopics/${subTopicId}/logic-attempts`,
    payload,
  )
  return data
}

export async function submitInteractiveAttempt(
  courseId: number | string,
  subTopicId: number | string,
  payload: InteractiveAttemptRequest,
) {
  const { data } = await api.post<InteractiveAttemptResponse>(
    `/v1/learner/courses/${courseId}/subtopics/${subTopicId}/interactive-attempts`,
    payload,
  )
  return data
}

export async function updateInteractiveProgress(
  courseId: number | string,
  subTopicId: number | string,
  status: Exclude<InteractiveProgressStatus, 'NOT_STARTED'>,
) {
  const { data } = await api.put<InteractiveProgressDto>(
    `/v1/learner/courses/${courseId}/subtopics/${subTopicId}/interactive-progress`,
    { status },
  )
  return data
}
