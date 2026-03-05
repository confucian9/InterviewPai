import request from '@/utils/request'
import type { QaRecordDTO } from './interview'

export interface Tag {
  id: number
  name: string
}

export function getAllTags() {
  return request.get<any, { data: Tag[] }>('/tags')
}

export function searchByKeyword(keyword: string) {
  return request.get<any, { data: QaRecordDTO[] }>('/tags/search', {
    params: { keyword }
  })
}

export function searchByTag(tag: string) {
  return request.get<any, { data: QaRecordDTO[] }>(`/tags/search/${tag}`)
}

export function getRandomQuestions(count = 10) {
  return request.get<any, { data: QaRecordDTO[] }>('/review/random', {
    params: { count }
  })
}

export function getQuestionsByTag(tag: string, count = 10) {
  return request.get<any, { data: QaRecordDTO[] }>(`/review/tag/${tag}`, {
    params: { count }
  })
}

export function getAllQuestionsForReview() {
  return request.get<any, { data: QaRecordDTO[] }>('/review/all')
}
