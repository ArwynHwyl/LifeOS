import api from '@/services/api'
import type { InteractionType } from '@/features/courses/services/adminCourses'

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
  sortOrder: number
  interactionType: InteractionType
  interactionPrompt: string | null
  interactionConfig: string | null
}

export async function getPublishedCourse(courseId: number | string) {
  const { data } = await api.get<PublishedCourseDetailDto>(`/v1/learner/courses/${courseId}`)
  return data
}
