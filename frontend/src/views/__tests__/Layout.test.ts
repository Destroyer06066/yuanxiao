import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { defineComponent, h } from 'vue'
import Layout from '../common/Layout.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  getCurrentUser: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('@/api/axios', () => ({
  default: { get: vi.fn().mockResolvedValue({ data: { data: { records: [] } } }), post: vi.fn(), patch: vi.fn() },
}))

vi.mock('@/api/student', () => ({
  searchStudents: vi.fn().mockResolvedValue({ data: { data: [] } }),
}))

vi.mock('@/router', () => ({
  default: { push: vi.fn() },
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ path: '/dashboard', query: {} }),
}))

vi.mock('@element-plus/icons-vue', () => {
  const s = { render: () => null }
  return {
    HomeFilled: s, OfficeBuilding: s, User: s, Reading: s,
    DataLine: s, RefreshRight: s, Stamp: s, Check: s,
    DataBoard: s, Avatar: s, Bell: s, Expand: s, Fold: s,
    ArrowDown: s, SwitchButton: s, School: s,
    TrendCharts: s, Setting: s, Document: s, Key: s,
  }
})

// Slot-rendering stubs that render ALL slots (default + named like #title)
const slot = (tag: string) => defineComponent({
  name: tag,
  inheritAttrs: false,
  render() {
    const children: any[] = []
    if (this.$slots) {
      for (const fn of Object.values(this.$slots)) {
        if (typeof fn === 'function') children.push(fn())
      }
    }
    return h('div', { class: tag }, children)
  },
})

// Override global stubs with slot-rendering versions (PascalCase to match setup.ts)
const stubs: Record<string, any> = {
  ElContainer: slot('el-container'),
  ElAside: slot('el-aside'),
  ElHeader: slot('el-header'),
  ElMain: slot('el-main'),
  ElMenu: slot('el-menu'),
  ElMenuItem: slot('el-menu-item'),
  ElSubMenu: slot('el-sub-menu'),
  ElIcon: slot('el-icon'),
  ElDropdown: slot('el-dropdown'),
  ElDropdownMenu: slot('el-dropdown-menu'),
  ElDropdownItem: slot('el-dropdown-item'),
  ElBadge: slot('el-badge'),
  ElAvatar: slot('el-avatar'),
  ElSelect: slot('el-select'),
  ElOption: slot('el-option'),
  ElButton: slot('el-button'),
  'router-view': true,
}

describe('Layout Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders with OP_ADMIN role showing admin menus', () => {
    const authStore = useAuthStore()
    authStore.token = 'test-token'
    authStore.userInfo = {
      accountId: '1',
      username: 'admin',
      role: 'OP_ADMIN',
      schoolId: null,
      realName: '系统管理员',
      requirePasswordChange: false,
      accessToken: 'test-token',
    }

    const wrapper = mount(Layout, {
      global: { stubs },
    })

    const text = wrapper.text()
    expect(text).toContain('院校管理平台')
    // Menu item titles are in named #title slots; html contains them
    const html = wrapper.html()
    expect(html).toContain('首页')
    expect(html).toContain('招生管理')
    expect(html).toContain('院校管理')
  })

  it('shows user realName', () => {
    const authStore = useAuthStore()
    authStore.token = 'test-token'
    authStore.userInfo = {
      accountId: '1',
      username: 'admin',
      role: 'OP_ADMIN',
      schoolId: null,
      realName: '张三管理',
      requirePasswordChange: false,
      accessToken: 'test-token',
    }

    const wrapper = mount(Layout, {
      global: { stubs },
    })

    expect(wrapper.text()).toContain('张三管理')
  })
})
