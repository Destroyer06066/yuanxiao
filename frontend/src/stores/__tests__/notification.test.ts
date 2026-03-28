import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '../notification'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}))

describe('Notification Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('initial state: empty list, 0 unread', () => {
    const store = useNotificationStore()
    expect(store.list).toEqual([])
    expect(store.unreadCount).toBe(0)
    expect(store.hasUnread).toBe(false)
  })

  it('fetchNotifications updates list from API', async () => {
    const axios = (await import('@/api/axios')).default
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        data: {
          records: [
            { id: '1', title: 'Test', content: 'Content', isRead: false, createdAt: '2026-01-01' },
            { id: '2', title: 'Read', content: 'Content2', isRead: true, createdAt: '2026-01-02' },
          ],
        },
      },
    })

    const store = useNotificationStore()
    await store.fetchNotifications()

    expect(store.list).toHaveLength(2)
    expect(store.unreadCount).toBe(1)
    expect(store.hasUnread).toBe(true)
  })

  it('markAllRead resets unread count', async () => {
    const axios = (await import('@/api/axios')).default
    vi.mocked(axios.post).mockResolvedValue({})

    const store = useNotificationStore()
    // Set up some unread state
    store.list = [
      { id: '1', title: 'T1', content: 'C1', isRead: false, createdAt: '2026-01-01' },
      { id: '2', title: 'T2', content: 'C2', isRead: false, createdAt: '2026-01-02' },
    ] as any
    store.unreadCount = 2

    await store.markAllRead()

    expect(store.unreadCount).toBe(0)
    expect(store.list.every((n: any) => n.isRead)).toBe(true)
  })

  it('startPolling/stopPolling manage timer', async () => {
    const axios = (await import('@/api/axios')).default
    vi.mocked(axios.get).mockResolvedValue({
      data: { data: { records: [] } },
    })

    const store = useNotificationStore()
    store.startPolling()

    // fetchNotifications called immediately
    expect(axios.get).toHaveBeenCalledTimes(1)

    // Advance 30s - should call again
    await vi.advanceTimersByTimeAsync(30_000)
    expect(axios.get).toHaveBeenCalledTimes(2)

    store.stopPolling()

    // Advance another 30s - should NOT call again
    await vi.advanceTimersByTimeAsync(30_000)
    expect(axios.get).toHaveBeenCalledTimes(2)
  })
})
