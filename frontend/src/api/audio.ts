import request from '@/utils/request'

export interface AudioUploadResponse {
  audioId: number
  fileUrl: string
  fileName: string
}

export interface AudioRecordDTO {
  id: number
  fileName: string
  fileUrl: string
  duration: number
  status: string
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  pages: number
  current: number
  size: number
}

export function uploadAudio(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, { data: AudioUploadResponse }>('/audio/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getAudioList(page = 1, size = 10) {
  return request.get<any, { data: PageResult<AudioRecordDTO> }>('/audio/list', {
    params: { page, size }
  })
}

export function getAudioDetail(id: number) {
  return request.get<any, { data: AudioRecordDTO }>(`/audio/${id}`)
}

export function deleteAudio(id: number) {
  return request.delete(`/audio/${id}`)
}

export function processAudio(audioId: number) {
  return request.post(`/process/audio/${audioId}`)
}
