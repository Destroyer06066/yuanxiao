import { type Page, type Locator } from '@playwright/test';

export class SchoolManagePage {
  readonly page: Page;
  readonly table: Locator;
  readonly addButton: Locator;
  readonly formDialog: Locator;
  readonly schoolNameInput: Locator;

  constructor(page: Page) {
    this.page = page;
    this.table = page.locator('.el-table');
    this.addButton = page.getByRole('button', { name: /新增院校/ });
    this.formDialog = page.locator('.el-dialog');
    this.schoolNameInput = page.locator('.el-dialog').getByPlaceholder('请输入院校名称');
  }

  async goto() {
    await this.page.goto('/admin/schools');
    await this.page.waitForLoadState('networkidle');
  }

  async rowCount(): Promise<number> {
    return this.page.locator('.el-table__row').count();
  }
}
