import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  workers: 1,
  reporter: 'list',
  use: { baseURL: 'http://127.0.0.1:8080', browserName: 'chromium' },
  webServer: {
    command: 'sh ../run-demo.sh',
    url: 'http://127.0.0.1:8080',
    reuseExistingServer: true,
    timeout: 120000,
  },
});
