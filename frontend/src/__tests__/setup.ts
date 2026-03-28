import { config } from '@vue/test-utils';

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
  transition: false,
};
