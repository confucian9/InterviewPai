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
