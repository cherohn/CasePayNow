import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.goto('/');
  await expect(page.getByLabel('Perfil fictício')).toBeEnabled();
  await expect(page.getByRole('button', { name: /Lead #101/ })).toBeVisible();
});

test('updates the Java record and confirms version 2', async ({ page }) => {
  await page.getByRole('button', { name: 'Salvar alteração' }).click();
  await expect(page.getByRole('status')).toContainText('Lead 101 atualizado');
  await expect(page.getByLabel('Versão carregada')).toHaveValue('2');
  await expect(page.getByRole('button', { name: 'Salvar alteração' })).toBeDisabled();
  const records = await (await page.request.get('/api/leads')).json();
  expect(records[0].status).toBe('sent_to_funder');
  expect(records[0].version).toBe(2);
});

test('denies another agent and cross-tenant access, then permits the manager', async ({ page }) => {
  await page.getByLabel('ID do lead').fill('102');
  await page.getByRole('button', { name: 'Salvar alteração' }).click();
  await expect(page.getByRole('status')).toContainText('sem permissão');
  await expect(page.locator('.http-code').first()).toHaveText('404');
  await expect(page.getByLabel('ID do lead')).toHaveValue('102');
  await page.getByLabel('Perfil fictício').selectOption('manager-a');
  await expect(page.getByRole('button', { name: /Lead #102/ })).toBeVisible();
  await page.getByLabel('ID do lead').fill('201');
  await page.getByRole('button', { name: 'Salvar alteração' }).click();
  await expect(page.locator('.http-code').first()).toHaveText('404');
  await page.getByRole('button', { name: /Lead #102/ }).click();
  await page.getByLabel('Novo status').selectOption('cannot_fund');
  await page.getByRole('button', { name: 'Salvar alteração' }).click();
  await expect(page.getByRole('status')).toContainText('Lead 102 atualizado');
  const records = await (await page.request.get('/api/leads')).json();
  expect(records.find(row => row.id === 101).version).toBe(1);
  expect(records.find(row => row.id === 102).status).toBe('cannot_fund');
});

test('conflict preserves the draft and requires explicit review', async ({ page }) => {
  await page.locator('summary').click();
  await page.getByLabel('Novo status').selectOption('cannot_fund');
  await page.getByRole('button', { name: 'Simular outra edição no servidor' }).click();
  await expect(page.getByRole('status')).toContainText('A versão mudou');
  await page.getByRole('button', { name: 'Salvar alteração' }).click();
  await expect(page.locator('.http-code').first()).toHaveText('409');
  await expect(page.getByLabel('Novo status')).toHaveValue('cannot_fund');
  await expect(page.getByLabel('Versão carregada')).toHaveValue('1');
  await expect(page.getByRole('button', { name: 'Salvar alteração' })).toBeDisabled();
  await page.getByRole('button', { name: 'Usar dados atuais após revisão' }).click();
  await expect(page.getByLabel('Versão carregada')).toHaveValue('2');
  const records = await (await page.request.get('/api/leads')).json();
  expect(records[0].status).toBe('contact_lawyer');
  expect(records[0].version).toBe(2);
});

test('503 preserves input and does not change the stored state', async ({ page }) => {
  await page.locator('summary').click();
  await page.getByLabel('Simular erro 503 antes de salvar').check();
  await page.getByLabel('Novo status').selectOption('cannot_fund');
  await page.getByRole('button', { name: 'Salvar alteração' }).click();
  await expect(page.locator('.http-code').first()).toHaveText('503');
  await expect(page.getByLabel('Novo status')).toHaveValue('cannot_fund');
  await expect(page.getByLabel('Versão carregada')).toHaveValue('1');
  await expect(page.getByRole('button', { name: 'Salvar alteração' })).toBeEnabled();
  const records = await (await page.request.get('/api/leads')).json();
  expect(records[0].version).toBe(1);
  expect(records[0].status).toBe('contact_lawyer');
});

test('network failure preserves the draft and permits retry', async ({ page }) => {
  await page.route('**/api/leads/update', route => route.abort());
  await page.getByLabel('Novo status').selectOption('cannot_fund');
  await page.getByRole('button', { name: 'Salvar alteração' }).click();
  await expect(page.getByRole('status')).toContainText('Não foi possível confirmar');
  await expect(page.getByLabel('Novo status')).toHaveValue('cannot_fund');
  await expect(page.getByLabel('Versão carregada')).toHaveValue('1');
  await expect(page.getByRole('button', { name: 'Salvar alteração' })).toBeEnabled();
  const records = await (await page.request.get('/api/leads')).json();
  expect(records[0].version).toBe(1);
});

test('double submission sends a single request', async ({ page }) => {
  let calls = 0;
  await page.route('**/api/leads/update', async route => {
    calls++;
    await new Promise(resolve => setTimeout(resolve, 250));
    await route.continue();
  });
  await page.locator('form').evaluate(form => { form.requestSubmit(); form.requestSubmit(); });
  await expect(page.getByRole('status')).toContainText('Lead 101 atualizado');
  expect(calls).toBe(1);
  const records = await (await page.request.get('/api/leads')).json();
  expect(records[0].version).toBe(2);
});

test('unknown and anonymous profiles are rejected', async ({ page }) => {
  for (const [profile, status] of [['unknown', '403'], ['anonymous', '401']]) {
    await page.getByLabel('Perfil fictício').selectOption(profile);
    await expect(page.getByRole('status')).toContainText('Perfil de teste alterado');
    await page.getByRole('button', { name: 'Salvar alteração' }).click();
    await expect(page.locator('.http-code').first()).toHaveText(status);
    await expect(page.getByLabel('Versão carregada')).toHaveValue('1');
  }
  await page.getByLabel('Perfil fictício').selectOption('agent-a');
  await expect(page.getByRole('button', { name: /Lead #101/ })).toBeVisible();
  const records = await (await page.request.get('/api/leads')).json();
  expect(records[0].version).toBe(1);
});

test('HTTP adapter preserves JSON types and rejects CSRF', async ({ page }) => {
  const { csrf } = await (await page.request.get('/api/session')).json();
  const before = await (await page.request.get('/api/leads')).json();
  for (const data of [
    '{"id":true,"status":"cannot_fund","version":1}',
    '{"id":"101","status":"cannot_fund","version":1}',
    '{"id":101,"status":"cannot_fund","version":1.0}',
    '{"id":101,"status":"cannot_fund","version":1,"role":"manager"}',
    '{broken',
  ]) {
    const response = await page.request.post('/api/leads/update', {
      headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf }, data,
    });
    expect(response.status()).toBe(422);
    expect(await (await page.request.get('/api/leads')).json()).toEqual(before);
  }
  const denied = await page.request.post('/api/leads/update', { data: { id: 101, status: 'cannot_fund', version: 1 } });
  expect(denied.status()).toBe(403);
  expect(await (await page.request.get('/api/leads')).json()).toEqual(before);
});

test('mobile page fits the viewport and has no script errors', async ({ page }) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByRole('heading', { name: 'Atualização de leads' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  expect(errors).toEqual([]);
});
