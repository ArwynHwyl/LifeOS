import api from '@/services/api'
import type { GamificationRewardDto } from '@/features/gamified/services/gamification'

export interface AssessmentSummaryDto {
  id: number
  title: string
  description: string | null
  tag: string
  questionCount: number
  sortOrder: number
  attempted: boolean
  score: number | null
  totalQuestions: number | null
}

export interface AssessmentOptionDto {
  id: number
  optionText: string
}

export interface AssessmentQuestionDto {
  id: number
  questionText: string
  options: AssessmentOptionDto[]
}

export interface AssessmentOptionResultDto {
  id: number
  optionText: string
  correct: boolean
  selected: boolean
}

export interface AssessmentQuestionResultDto {
  id: number
  questionText: string
  correct: boolean
  options: AssessmentOptionResultDto[]
}

export interface AssessmentResultDto {
  assessmentId: number
  title: string
  score: number
  totalQuestions: number
  percentage: number
  submittedAt: string
  questionResults: AssessmentQuestionResultDto[]
  reward: GamificationRewardDto
}

export interface AssessmentDetailDto {
  id: number
  title: string
  description: string | null
  attempted: boolean
  questions: AssessmentQuestionDto[] | null
  result: AssessmentResultDto | null
}

export interface AnswerSubmission {
  questionId: number
  selectedOptionId: number | null
}

export async function listAssessments() {
  const { data } = await api.get<AssessmentSummaryDto[]>('/v1/learner/assessments')
  return data
}

export async function getAssessmentDetail(assessmentId: number) {
  const { data } = await api.get<AssessmentDetailDto>(`/v1/learner/assessments/${assessmentId}`)
  return data
}

export async function submitAssessment(assessmentId: number, answers: AnswerSubmission[]) {
  const { data } = await api.post<AssessmentResultDto>(`/v1/learner/assessments/${assessmentId}/submit`, { answers })
  return data
}
