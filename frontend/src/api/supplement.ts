import axios from './axios'
import type { Result } from './axios'

export interface SupplementRound {
  roundId: string
  roundNumber: number
  startTime: string
  endTime: string
  remark?: string
  status: string
  pushedCount: number
  admittedCount: number
  confirmedCount: number
}

export const getSupplementRounds = () =>
  axios.get<Result<SupplementRound[]>>('/v1/supplement/rounds')
