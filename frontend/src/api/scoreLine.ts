import axios from './axios'
import type { Result } from './axios'

export interface ScoreLine {
  lineId: string
  majorId: string
  majorName: string
  year: number
  subject: string
  minScore: number
}

export interface CreateScoreLineRequest {
  majorId: string
  year: number
  subject: string
  minScore: number
}

export const getScoreLines = () =>
  axios.get<Result<ScoreLine[]>>('/v1/score-lines')

export const createScoreLine = (data: CreateScoreLineRequest) =>
  axios.post('/v1/score-lines', data)

export const updateScoreLine = (lineId: string, data: CreateScoreLineRequest) =>
  axios.put(`/v1/score-lines/${lineId}`, data)

export const deleteScoreLine = (lineId: string) =>
  axios.delete(`/v1/score-lines/${lineId}`)
