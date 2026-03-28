import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './specs',
  timeout: 30_000,
  expect: { timeout: 5_000 },
  fullyParallel: false,
  retries: 1,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL: 'http://localhost:5174',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'op_admin',
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/op_admin.json',
      },
      dependencies: ['setup'],
    },
    {
      name: 'school_admin',
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/school_admin.json',
      },
      dependencies: ['setup'],
    },
    {
      name: 'school_staff',
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/school_staff.json',
      },
      dependencies: ['setup'],
    },
    {
      name: 'setup',
      testMatch: /global\.setup\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  outputDir: './test-results',
});
