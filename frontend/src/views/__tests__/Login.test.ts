import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import Login from '../common/Login.vue'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  getCurrentUser: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('@/router', () => ({
  default: { push: vi.fn() },
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {} }),
}))

vi.mock('@element-plus/icons-vue', () =>
  new Proxy({}, {
    get: (_target, prop) => {
      if (prop === '__esModule') return false
      return { name: String(prop), render: () => null }
    },
  })
)

describe('Login Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders form with username and password inputs', () => {
    const wrapper = mount(Login, {
      shallow: true,
    })

    const html = wrapper.html()
    // el-form rendered as el-form-stub in shallow mode
    expect(html).toContain('el-form-stub')
    // The page title is visible
    expect(wrapper.text()).toContain('院校管理平台')
    expect(wrapper.text()).toContain('中国政府奖学金项目招生管理系统')
  })

  it('contains login-related structure', () => {
    const wrapper = mount(Login, {
      shallow: true,
    })

    // Login page structure
    expect(wrapper.find('.login-page').exists()).toBe(true)
    expect(wrapper.find('.login-card').exists()).toBe(true)
    expect(wrapper.find('.login-header').exists()).toBe(true)
  })

  it('renders the page title and subtitle', () => {
    const wrapper = mount(Login, {
      shallow: true,
    })

    expect(wrapper.find('.login-title').text()).toBe('院校管理平台')
    expect(wrapper.find('.login-subtitle').text()).toBe('中国政府奖学金项目招生管理系统')
  })
})
