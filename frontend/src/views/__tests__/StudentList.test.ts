import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { defineComponent, h } from 'vue'
import StudentList from '../school/StudentList.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/student', () => ({
  queryStudents: vi.fn().mockResolvedValue({ data: { data: { records: [], total: 0 } } }),
  directAdmission: vi.fn(),
  conditionalAdmission: vi.fn(),
  batchReject: vi.fn(),
}))

vi.mock('@/api/school', () => ({
  getSchools: vi.fn().mockResolvedValue({ data: { data: { records: [] } } }),
}))

vi.mock('@/api/axios', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}))

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
  useRoute: () => ({ path: '/students', query: {} }),
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn(), info: vi.fn(), warning: vi.fn() },
  ElMessageBox: { confirm: vi.fn() },
}))

vi.mock('@element-plus/icons-vue', () => ({
  Download: { render: () => null },
}))

// Slot-rendering stubs (PascalCase to override setup.ts global stubs)
const slot = (tag: string) => defineComponent({
  name: tag,
  inheritAttrs: false,
  render() { return h('div', { class: tag }, this.$slots.default?.()) },
})

const stubs: Record<string, any> = {
  ElCard: slot('el-card'),
  ElForm: slot('el-form'),
  ElFormItem: slot('el-form-item'),
  ElTable: slot('el-table'),
  ElTableColumn: true,
  ElSelect: slot('el-select'),
  ElOption: slot('el-option'),
  ElButton: slot('el-button'),
  ElIcon: slot('el-icon'),
  ElTag: slot('el-tag'),
  ElInput: slot('el-input'),
  ElInputNumber: slot('el-input-number'),
  ElDatePicker: slot('el-date-picker'),
  ElCheckTag: slot('el-check-tag'),
  ElPagination: slot('el-pagination'),
  ElDialog: slot('el-dialog'),
}

describe('StudentList Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  function setupAuthStore() {
    const authStore = useAuthStore()
    authStore.token = 'test-token'
    authStore.userInfo = {
      accountId: '1', username: 'school', role: 'SCHOOL_ADMIN',
      schoolId: 'school-1', realName: '校管理', requirePasswordChange: false, accessToken: 'token',
    }
  }

  it('renders with table/list structure', () => {
    setupAuthStore()

    const wrapper = mount(StudentList, {
      global: {
        stubs,
        directives: { loading: () => {} },
      },
    })

    expect(wrapper.text()).toContain('考生列表')
    expect(wrapper.find('.el-table').exists()).toBe(true)
  })

  it('contains status filter area', () => {
    setupAuthStore()

    const wrapper = mount(StudentList, {
      global: {
        stubs,
        directives: { loading: () => {} },
      },
    })

    const html = wrapper.html()
    // Status filter select is present inside the form
    expect(wrapper.find('.el-select').exists()).toBe(true)
    // Quick filter tags
    expect(html).toContain('快捷筛选')
    expect(html).toContain('待处理')
    expect(html).toContain('查询')
    expect(html).toContain('重置')
  })
})
