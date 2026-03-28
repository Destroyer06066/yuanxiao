import { type Page, type Locator } from '@playwright/test';

export class Sidebar {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  menuItem(text: string): Locator {
    return this.page.locator('.el-menu-item, .el-sub-menu__title').filter({ hasText: text });
  }

  async navigateTo(menuText: string) {
    await this.menuItem(menuText).click();
    await this.page.waitForLoadState('networkidle');
  }

  async visibleMenuItems(): Promise<string[]> {
    const items = this.page.locator('.el-menu-item');
    return items.allTextContents();
  }
}
