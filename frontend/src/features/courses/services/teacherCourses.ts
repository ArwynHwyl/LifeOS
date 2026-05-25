import api from '@/services/api'
import {
  toAdminCourseCard,
  type AdminCourseSummaryDto,
  type AdminCourseDetailDto,
  type AdminCourseCardModel,
} from './adminCourses'

export interface TeacherCourseReviewDetailDto {
  course: AdminCourseDetailDto
  reviews: TeacherCourseReviewDto[]
}

export interface TeacherCourseReviewDto {
  id: number
  courseId: number
  reviewerId: string
  reviewerName: string
  decision: string
  feedback: string | null
  createdAt: string
  updatedAt: string
}

export async function listTeacherCourses(): Promise<AdminCourseSummaryDto[]> {
  const { data } = await api.get<AdminCourseSummaryDto[]>('/v1/teacher/course-reviews')
  return data
}

export async function getTeacherCourse(courseId: number | string): Promise<AdminCourseDetailDto> {
  const { data } = await api.get<TeacherCourseReviewDetailDto>(`/v1/teacher/course-reviews/${courseId}`)
  return data.course
}

export async function approveTeacherCourse(courseId: number | string): Promise<TeacherCourseReviewDto> {
  const { data } = await api.post<TeacherCourseReviewDto>(`/v1/teacher/course-reviews/${courseId}/approve`)
  return data
}

export async function rejectTeacherCourse(courseId: number | string): Promise<TeacherCourseReviewDto> {
  const { data } = await api.post<TeacherCourseReviewDto>(
    `/v1/teacher/course-reviews/${courseId}/request-revision`,
    { feedback: 'Revision requested. See discussion comments for details.', comments: [] },
  )
  return data
}

export const toTeacherCourseCard = toAdminCourseCard
export type TeacherCourseCardModel = AdminCourseCardModel
