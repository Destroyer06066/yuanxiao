import { type Page, type Locator } from '@playwright/test';

export class StudentListPage {
  readonly page: Page;
  readonly table: Locator;
  readonly statusFilter: Locator;
  readonly admitButton: Locator;
  readonly rejectButton: Locator;
  readonly batchRejectButton: Locator;
  readonly pagination: Locator;

  constructor(page: Page) {
    this.page = page;
    this.table = page.locator('.el-table');
    this.statusFilter = page.locator('.el-select').first();
    this.admitButton = page.getByRole('button', { name: '录取' });
    this.rejectButton = page.getByRole('button', { name: '拒绝' });
    this.batchRejectButton = page.getByRole('button', { name: '批量拒绝' });
    this.pagination = page.locator('.el-pagination');
  }

  async goto() {
    await this.page.goto('/students');
    await this.page.waitForLoadState('networkidle');
  }

  async rowCount(): Promise<number> {
    return this.page.locator('.el-table__row').count();
  }

  async getRowText(index: number): Promise<string> {
    return (await this.page.locator('.el-table__row').nth(index).textContent()) ?? '';
  }
}
