import { type Page, type Locator } from '@playwright/test';

export class StatisticsPage {
  readonly page: Page;
  readonly kpiCards: Locator;
  readonly charts: Locator;

  constructor(page: Page) {
    this.page = page;
    this.kpiCards = page.locator('.el-statistic, .el-card').filter({ hasText: /待处理|已录取|已确认|已报到/ });
    this.charts = page.locator('canvas, .echarts, [_echarts_instance_]');
  }

  async goto() {
    await this.page.goto('/statistics');
    await this.page.waitForLoadState('networkidle');
  }
}
