import request from '@/utils/request'
import type { PageResult } from './audio'

export interface QaRecordDTO {
  id: number
  audioId: number
  question: string
  answer: string
  tags: string
  confidence: number
  createTime: string
}

export interface InterviewSummaryDTO {
  id: number
  audioId: number
  summary: string
  keywords: string
  createTime: string
}

export interface InterviewDetailDTO {
  id: number
  fileName: string
  fileUrl: string
  duration: number
  status: string
  createTime: string
  transcript: string
  qaList: QaRecordDTO[]
  summary: InterviewSummaryDTO
}

export function getInterviewList(page = 1, size = 10) {
  return request.get<any, { data: PageResult<InterviewDetailDTO> }>('/interview/list', {
    params: { page, size }
  })
}

export function getInterviewDetail(id: number) {
  return request.get<any, { data: InterviewDetailDTO }>(`/interview/${id}`)
}

export function getQaList(audioId: number, page = 1, size = 10) {
  return request.get<any, { data: PageResult<QaRecordDTO> }>(`/interview/${audioId}/qa`, {
    params: { page, size }
  })
}

export function updateQa(qaId: number, data: { question?: string; answer?: string; tags?: string[] }) {
  return request.put(`/interview/qa/${qaId}`, data)
}

export function deleteQa(qaId: number) {
  return request.delete(`/interview/qa/${qaId}`)
}

export interface GenerateExperienceRequest {
  title?: string
}

export interface GenerateExperienceResponse {
  title: string
  content: string
  fileUrl: string
}

export function generateExperience(audioId: number, data?: GenerateExperienceRequest) {
  return request.post<any, { data: GenerateExperienceResponse }>(`/interview/${audioId}/generate-experience`, data || {})
}

export interface StreamProcessEvent {
  type: 'status' | 'qa_chunk_start' | 'qa_stream' | 'qa_chunk_end' | 'summary_start' | 'summary_stream' | 'summary_end' | 'done' | 'error' | 'warning'
  data: string
}

export type StreamCallback = (event: StreamProcessEvent) => void

export function streamProcessAudio(audioId: number, onEvent: StreamCallback): () => void {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  const eventSource = new EventSource(`${baseUrl}/api/stream/process/${audioId}`, {
    withCredentials: true
  })
  
  eventSource.addEventListener('status', (e) => {
    onEvent({ type: 'status', data: e.data })
  })
  
  eventSource.addEventListener('qa_chunk_start', (e) => {
    onEvent({ type: 'qa_chunk_start', data: e.data })
  })
  
  eventSource.addEventListener('qa_stream', (e) => {
    onEvent({ type: 'qa_stream', data: e.data })
  })
  
  eventSource.addEventListener('qa_chunk_end', (e) => {
    onEvent({ type: 'qa_chunk_end', data: e.data })
  })
  
  eventSource.addEventListener('summary_start', (e) => {
    onEvent({ type: 'summary_start', data: e.data })
  })
  
  eventSource.addEventListener('summary_stream', (e) => {
    onEvent({ type: 'summary_stream', data: e.data })
  })
  
  eventSource.addEventListener('summary_end', (e) => {
    onEvent({ type: 'summary_end', data: e.data })
  })
  
  eventSource.addEventListener('done', (e) => {
    onEvent({ type: 'done', data: e.data })
    eventSource.close()
  })
  
  eventSource.addEventListener('error', (e) => {
    onEvent({ type: 'error', data: e.data })
    eventSource.close()
  })
  
  eventSource.addEventListener('warning', (e) => {
    onEvent({ type: 'warning', data: e.data })
  })
  
  eventSource.onerror = () => {
    onEvent({ type: 'error', data: '连接失败' })
    eventSource.close()
  }
  
  return () => {
    eventSource.close()
  }
}
