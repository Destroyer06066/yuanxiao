import axios from './axios'
import type { Result } from './axios'

export interface CheckinRecord {
  pushId: string
  candidateName: string
  majorName: string
  totalScore: number
  status: string
  statusDesc: string
  receiveTime?: string
  checkinTime?: string
}

export const getCheckinList = (params?: { status?: string; materialReceived?: boolean }) =>
  axios.get<Result<CheckinRecord[]>>('/v1/checkins', { params })

export const receiveMaterial = (pushId: string, note?: string) =>
  axios.post('/v1/material-receive', { pushId, note })

export const doCheckin = (pushId: string, note?: string) =>
  axios.post('/v1/checkin', { pushId, note })
