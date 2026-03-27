import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from '@/api/axios'

interface Notification {
  id: string
  title: string
  content: string
  isRead: boolean
  createdAt: string
}

export const useNotificationStore = defineStore('notification', () => {
  const list = ref<Notification[]>([])
  const unreadCount = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  const hasUnread = computed(() => unreadCount.value > 0)

  async function fetchNotifications() {
    try {
      const res = await axios.get('/v1/notifications', {
        params: { page: 1, pageSize: 20 },
      })
      list.value = res.data.data.records
      unreadCount.value = list.value.filter((n: Notification) => !n.isRead).length
    } catch (e) {
      // ignore
    }
  }

  async function markRead(id: string) {
    try {
      await axios.patch(`/v1/notifications/${id}/read`)
      const n = list.value.find((n: Notification) => n.id === id)
      if (n && !n.isRead) {
        n.isRead = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch (e) {
      // ignore
    }
  }

  async function markAllRead() {
    try {
      await axios.post('/v1/notifications/read-all')
      list.value.forEach((n: Notification) => (n.isRead = true))
      unreadCount.value = 0
    } catch (e) {
      // ignore
    }
  }

  function startPolling() {
    fetchNotifications()
    if (!timer) {
      timer = setInterval(fetchNotifications, 30_000)
    }
  }

  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  return {
    list,
    unreadCount,
    hasUnread,
    fetchNotifications,
    markRead,
    markAllRead,
    startPolling,
    stopPolling,
  }
})
