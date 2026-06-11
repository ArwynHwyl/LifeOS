import api from '@/services/api'
import { DEFAULT_COVER_ID } from '@/features/courses/constants/courseCoverPresets'
import type { InteractiveTemplate } from '@/features/courses/types/interactive'
import type { CourseStatus } from '@/types/types'

const COURSE_COVER_STORAGE_KEY = 'lifeosCourseCovers'

export type BackendCourseStatus = 'DRAFT' | 'PENDING_REVIEW' | 'NEED_REVISION' | 'PUBLISHED'

export interface AdminCourseSummaryDto {
  id: number
  title: string
  description: string | null
  coverId: string | null
  status: BackendCourseStatus
  createdById: string
  createdByName: string
  approvedById: string | null
  approvedByName: string | null
  publishedAt: string | null
  moduleCount: number
  createdAt: string
  updatedAt: string
}

export interface AdminCourseDetailDto extends AdminCourseSummaryDto {
  modules: AdminModuleDto[]
  documentSources: AdminDocumentSourceDto[]
}

export interface AdminModuleDto {
  id: number
  courseId: number
  title: string
  description: string | null
  sortOrder: number
  contentDepth: string
  interactionType: InteractionType
  interactionPrompt: string | null
  interactionConfig: string | null
  createdAt: string
  updatedAt: string
  subTopics: AdminSubTopicDto[]
}

export type InteractionType = 'NONE' | 'GRAPH_2D' | 'FORMULA_EXPLORER' | 'VISUAL_LAYER' | 'LOGIC_FLOW' | 'QUIZ' | 'OTHER'

export interface AdminSubTopicDto {
  id: number
  moduleId: number
  title: string
  content: string
  contentHtml: string
  mascotPrompt: string | null
  assets: AdminSubTopicAssetDto[]
  sortOrder: number
  sourceType: string
  pageStart: number | null
  pageEnd: number | null
  interactionType: InteractionType
  interactionPrompt: string | null
  interactionConfig: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminSubTopicAssetDto {
  id: number
  subTopicId: number
  fileName: string
  fileType: string
  fileSizeBytes: number
  altText: string | null
  storagePath: string
  fileUrl: string
  fileUrlExpiresAt: string
  createdAt: string
}

export interface CourseCreatePayload {
  title: string
  description?: string | null
  coverId?: string
  pdfFile?: File | null
  aiEnabled?: boolean
  prompt?: string
}

export interface AdminDocumentSourceDto {
  id: number
  courseId: number
  fileName: string
  displayName: string
  fileType: string
  fileSizeBytes: number
  storagePath: string
  uploadedById: string
  uploadedByName: string
  pageCount: number | null
  createdAt: string
  updatedAt: string
}

export interface DocumentUploadUrlResponse {
  uploadUrl: string
  storagePath: string
  expiresAt: string
  method: 'PUT'
  headers: Record<string, string>
}

export interface DocumentPreviewDto {
  documentId: number
  fileUrl: string
  fileUrlExpiresAt: string
  pageStart: number
  pageEnd: number
  pageCount: number
  pages: Array<{ pageNumber: number; text: string }>
}

export interface AiGenerationLogDto {
  id: number
  courseId: number
  moduleId: number | null
  type: 'COURSE_OUTLINE' | 'MODULE_DRAFT'
  requestedById: string
  documentSourceId: number
  pageStart: number
  pageEnd: number
  requirements: string | null
  prompt: string
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'
  rawResponse: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminCourseCardModel {
  id: string
  title: string
  description: string
  coverId: string
  status: CourseStatus
  moduleCount: number
  lastEdited: string
  createdBy: string
}

export async function listAdminCourses() {
  const { data } = await api.get<AdminCourseSummaryDto[]>('/v1/admin/courses')
  return data
}

export async function getAdminCourse(courseId: number | string) {
  const { data } = await api.get<AdminCourseDetailDto>(`/v1/admin/courses/${courseId}`)
  return data
}

export async function listInteractiveTemplates() {
  const { data } = await api.get<InteractiveTemplate[]>('/v1/admin/interactive-templates')
  return data
}

export async function createAdminCourse(payload: CourseCreatePayload) {
  if (payload.aiEnabled && !payload.pdfFile) {
    throw new Error('AI generation requires a PDF source.')
  }
  const { data } = await api.post<AdminCourseDetailDto>('/v1/admin/courses', {
    title: payload.title,
    description: payload.description?.trim() || null,
    coverId: payload.coverId ?? null,
    modules: [],
  })
  if (payload.pdfFile) {
    const documentSource = await uploadCourseDocument(data.id, payload.pdfFile)
    data.documentSources = [documentSource, ...data.documentSources]
    if (payload.aiEnabled) {
      await requestCourseOutlineGeneration({
        courseId: data.id,
        documentSourceId: documentSource.id,
        prompt: payload.prompt?.trim() || DEFAULT_AI_COURSE_OUTLINE_PROMPT,
      })
    }
  }
  return data
}

export async function listCourseDocuments(courseId: number | string) {
  const { data } = await api.get<AdminDocumentSourceDto[]>(`/v1/admin/courses/${courseId}/documents`)
  return data
}

export async function uploadCourseDocument(courseId: number | string, file: File) {
  const upload = await requestCourseDocumentUploadUrl(courseId, file)
  const putResponse = await fetch(upload.uploadUrl, {
    method: upload.method,
    headers: upload.headers,
    body: file,
  })
  if (!putResponse.ok) {
    throw new Error(`PDF upload failed with status ${putResponse.status}`)
  }

  const { data } = await api.post<AdminDocumentSourceDto>(`/v1/admin/courses/${courseId}/documents/complete`, {
    fileName: file.name,
    displayName: file.name.replace(/\.pdf$/i, ''),
    fileType: file.type || 'application/pdf',
    fileSizeBytes: file.size,
    storagePath: upload.storagePath,
}, { timeout: 120_000 })
  return data
}

export async function createCourseModule(courseId: number | string, payload: {
  title: string
  description?: string | null
  sortOrder: number
  contentDepth?: 'LOW' | 'MEDIUM' | 'HIGH'
}) {
  const { data } = await api.post<AdminModuleDto>(`/v1/admin/courses/${courseId}/modules`, {
    title: payload.title,
    description: payload.description?.trim() || null,
    sortOrder: payload.sortOrder,
    contentDepth: payload.contentDepth ?? 'MEDIUM',
    subTopics: [],
  })
  return data
}

export async function createModuleSubTopic(moduleId: number | string, payload: {
  title: string
  content?: string | null
  mascotPrompt?: string | null
  sortOrder: number
  pageStart?: number | null
  pageEnd?: number | null
}) {
  const { data } = await api.post<AdminSubTopicDto>(`/v1/admin/modules/${moduleId}/subtopics`, {
    title: payload.title,
    content: payload.content?.trim() || null,
    mascotPrompt: payload.mascotPrompt?.trim() || null,
    sortOrder: payload.sortOrder,
    pageStart: payload.pageStart ?? null,
    pageEnd: payload.pageEnd ?? null,
  })
  return data
}

export async function updateModuleSubTopic(subTopicId: number | string, payload: {
  title: string
  content?: string | null
  mascotPrompt?: string | null
  sortOrder: number
  pageStart?: number | null
  pageEnd?: number | null
  interactionType?: InteractionType
  interactionPrompt?: string | null
  interactionConfig?: string | null
}) {
  const { data } = await api.put<AdminSubTopicDto>(`/v1/admin/subtopics/${subTopicId}`, {
    title: payload.title,
    content: payload.content?.trim() || null,
    mascotPrompt: payload.mascotPrompt?.trim() || null,
    sortOrder: payload.sortOrder,
    pageStart: payload.pageStart ?? null,
    pageEnd: payload.pageEnd ?? null,
    interactionType: payload.interactionType ?? 'NONE',
    interactionPrompt: payload.interactionPrompt?.trim() || null,
    interactionConfig: payload.interactionConfig?.trim() || null,
  })
  return data
}

export async function refreshSubTopicAssets(subTopicId: number | string) {
  const { data } = await api.get<AdminSubTopicAssetDto[]>(`/v1/admin/subtopics/${subTopicId}/assets`)
  return data
}

export async function requestCourseDocumentUploadUrl(courseId: number | string, file: File) {
  const { data } = await api.post<DocumentUploadUrlResponse>(`/v1/admin/courses/${courseId}/documents/upload-url`, {
    fileName: file.name,
    fileType: file.type || 'application/pdf',
    fileSizeBytes: file.size,
  })
  return data
}

export async function uploadSubTopicImage(subTopicId: number | string, file: File, altText?: string) {
  const { data: upload } = await api.post<DocumentUploadUrlResponse>(
    `/v1/admin/subtopics/${subTopicId}/images/upload-url`,
    {
      fileName: file.name,
      fileType: file.type,
      fileSizeBytes: file.size,
    },
  )
  const putResponse = await fetch(upload.uploadUrl, {
    method: upload.method,
    headers: upload.headers,
    body: file,
  })
  if (!putResponse.ok) {
    throw new Error(`Image upload failed with status ${putResponse.status}`)
  }

  const { data } = await api.post<AdminSubTopicAssetDto>(
    `/v1/admin/subtopics/${subTopicId}/images/complete`,
    {
      fileName: file.name,
      fileType: file.type,
      fileSizeBytes: file.size,
      storagePath: upload.storagePath,
      altText: altText?.trim() || null,
    },
  )
  return data
}

export async function previewCourseDocument(documentId: number | string, pageStart: number, pageEnd: number) {
  const { data } = await api.get<DocumentPreviewDto>(`/v1/admin/documents/${documentId}/preview`, {
    params: { pageStart, pageEnd },
  })
  return data
}

export async function requestModuleAiGeneration(payload: {
  moduleId: number | string
  documentSourceId: number
  pageStart: number
  pageEnd: number
  requirements: string
}) {
  const { data } = await api.post<{ log: AiGenerationLogDto; generatedSubTopics: unknown[] }>(
    `/v1/admin/modules/${payload.moduleId}/ai-generations`,
    {
      documentSourceId: payload.documentSourceId,
      pageStart: payload.pageStart,
      pageEnd: payload.pageEnd,
      requirements: payload.requirements,
    },
  )
  return data
}

export async function requestCourseOutlineGeneration(payload: {
  courseId: number | string
  documentSourceId: number
  prompt: string
  pageStart?: number | null
  pageEnd?: number | null
}) {
  const { data } = await api.post<AiGenerationLogDto>(
    `/v1/admin/courses/${payload.courseId}/ai-outline-generations`,
    {
      documentSourceId: payload.documentSourceId,
      pageStart: payload.pageStart ?? null,
      pageEnd: payload.pageEnd ?? null,
      prompt: payload.prompt,
    },
  )
  return data
}

export async function getAiGenerationLog(logId: number | string) {
  const { data } = await api.get<AiGenerationLogDto>(`/v1/admin/ai-generations/${logId}`)
  return data
}

export async function listAiGenerationLogs(courseId: number | string) {
  const { data } = await api.get<AiGenerationLogDto[]>(`/v1/admin/courses/${courseId}/ai-generations`)
  return data
}

export const DEFAULT_AI_COURSE_OUTLINE_PROMPT = `Create a complete editable draft outline for a Math for Software Engineering course.

Use the PDF as the factual source. Organize the course into coherent modules with concise subtopics.

Prioritize concepts that help software engineering learners reason about programs, algorithms, graphics, data, and AI systems: logic, functions, discrete structures, linear algebra, probability, optimization, and complexity.

For each module and subtopic, suggest interactive or visual learning ideas only when they genuinely improve learning. Use interactionType values from NONE, QUIZ, GRAPH_2D, FORMULA_EXPLORER, or VISUAL_LAYER.

When interactionType is not NONE, include both an implementation-oriented interactionPrompt and a valid interactionConfig JSON object. interactionConfig.type must exactly match interactionType.`

export async function deleteAdminSubTopic(subTopicId: number | string) {
  await api.delete(`/v1/admin/subtopics/${subTopicId}`)
}

export async function deleteAdminCourse(courseId: number | string) {
  await api.delete(`/v1/admin/courses/${courseId}`)
}

export async function submitAdminCourseForReview(courseId: number | string) {
  const { data } = await api.post<AdminCourseDetailDto>(`/v1/admin/courses/${courseId}/submit-review`)
  return data
}

export async function updateAdminCourse(courseId: number | string, payload: { title: string; description?: string | null; coverId?: string }) {
  const { data } = await api.put<AdminCourseSummaryDto>(`/v1/admin/courses/${courseId}`, {
    title: payload.title,
    description: payload.description?.trim() || null,
    coverId: payload.coverId ?? null,
  })
  return data
}

export function toAdminCourseCard(course: AdminCourseSummaryDto | AdminCourseDetailDto): AdminCourseCardModel {
  return {
    id: String(course.id),
    title: course.title,
    description: course.description ?? '',
    coverId: course.coverId ?? getCourseCover(course.id),
    status: toCourseStatus(course.status),
    moduleCount: 'modules' in course ? course.modules.length : course.moduleCount,
    lastEdited: formatRelativeDate(course.updatedAt),
    createdBy: course.createdByName,
  }
}

function toCourseStatus(status: BackendCourseStatus): CourseStatus {
  if (status === 'PUBLISHED') return 'published'
  if (status === 'PENDING_REVIEW') return 'pending'
  if (status === 'NEED_REVISION') return 'revision'
  return 'draft'
}

function getCourseCover(courseId: number) {
  return readCourseCovers()[String(courseId)] ?? DEFAULT_COVER_ID
}

function saveCourseCover(courseId: number, coverId: string) {
  const covers = readCourseCovers()
  covers[String(courseId)] = coverId
  localStorage.setItem(COURSE_COVER_STORAGE_KEY, JSON.stringify(covers))
}

function readCourseCovers(): Record<string, string> {
  const raw = localStorage.getItem(COURSE_COVER_STORAGE_KEY)
  if (!raw) return {}
  try {
    return JSON.parse(raw) as Record<string, string>
  } catch {
    return {}
  }
}

function formatRelativeDate(value: string) {
  const date = new Date(value)
  const time = date.getTime()
  if (Number.isNaN(time)) return 'recently'

  const diffMs = Date.now() - time
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diffMs < minute) return 'just now'
  if (diffMs < hour) return `${Math.floor(diffMs / minute)} min ago`
  if (diffMs < day) return `${Math.floor(diffMs / hour)} hours ago`
  if (diffMs < 2 * day) return 'yesterday'
  if (diffMs < 7 * day) return `${Math.floor(diffMs / day)} days ago`

  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
}

export interface CourseReviewCommentDto {
  id: number
  moduleId: number | null
  subTopicId: number | null
  feedback: string
  createdAt: string
  updatedAt: string
  resolved: boolean
  resolvedAt: string | null
}

export interface CourseReviewDto {
  id: number
  courseId: number
  reviewerId: string
  reviewerName: string
  decision: string
  feedback: string | null
  createdAt: string
  updatedAt: string
  comments: CourseReviewCommentDto[]
}

export async function getReviewComments(courseId: number | string): Promise<CourseReviewDto[]> {
  const { data } = await api.get<CourseReviewDto[]>(`/v1/admin/courses/${courseId}/reviews`)
  return data
}

export async function setReviewCommentResolved(
  courseId: number | string,
  commentId: number | string,
  resolved: boolean
) {
  const { data } = await api.patch<CourseReviewCommentDto>(
    `/v1/admin/courses/${courseId}/review-comments/${commentId}/resolved`,
    { resolved }
  )
  return data
}

export async function deleteAdminModule(moduleId: number | string): Promise<void> {
  await api.delete(`/v1/admin/modules/${moduleId}`)
}
