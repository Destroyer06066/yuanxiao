import { config } from '@vue/test-utils';

// Polyfill localStorage if it's broken (e.g., happy-dom path issues)
if (typeof globalThis.localStorage === 'undefined' || typeof globalThis.localStorage.clear !== 'function') {
  const store: Record<string, string> = {};
  globalThis.localStorage = {
    getItem(key: string) { return store[key] ?? null; },
    setItem(key: string, value: string) { store[key] = String(value); },
    removeItem(key: string) { delete store[key]; },
    clear() { for (const k of Object.keys(store)) delete store[k]; },
    get length() { return Object.keys(store).length; },
    key(index: number) { return Object.keys(store)[index] ?? null; },
  } as Storage;
}

// Stub Element Plus components to avoid full rendering
config.global.stubs = {
  ElButton: true,
  ElInput: true,
  ElForm: true,
  ElFormItem: true,
  ElTable: true,
  ElTableColumn: true,
  ElDialog: true,
  ElMessage: true,
  ElMessageBox: true,
  ElSelect: true,
  ElOption: true,
  ElPagination: true,
  ElCard: true,
  ElTag: true,
  ElIcon: true,
  ElMenu: true,
  ElMenuItem: true,
  ElSubMenu: true,
  ElDropdown: true,
  ElBadge: true,
  ElEmpty: true,
  ElStatistic: true,
  ElLink: true,
  ElAvatar: true,
  ElAside: true,
  ElContainer: true,
  ElHeader: true,
  ElMain: true,
  ElDropdownMenu: true,
  ElDropdownItem: true,
  ElDatePicker: true,
  ElInputNumber: true,
  ElCheckTag: true,
  transition: false,
};
