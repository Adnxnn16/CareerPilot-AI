import { test, expect } from '@playwright/test';

test.describe('Applications Tracking (Feature 5)', () => {
  
  test('log in -> create application -> drag to new column -> assert persisted', async ({ page }) => {
    // 1. Log in
    await page.goto('/login');
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL(/\//);
    
    page.on('console', msg => console.log('BROWSER CONSOLE:', msg.text()));
    page.on('pageerror', err => console.log('BROWSER ERROR:', err.message));
    
    // 2. Go to a job and create an application
    await page.goto('/jobs/00000000-0000-0000-0000-000000000001');
    try {
      await page.waitForSelector('#save-to-tracker-btn', { timeout: 5000 });
    } catch (e) {
      console.log(await page.content());
    }
    await page.click('#save-to-tracker-btn');
    // Ensure the dialog opens
    await page.waitForSelector('text=Save to Tracker', { state: 'visible' });
    await page.fill('textarea#app-notes', 'Playwright test note');
    await page.click('#save-to-tracker-submit');

    // 3. Go to applications board
    await page.goto('/applications');
    
    // Check it is in the SAVED column
    const card = page.locator('div[role="button"]', { hasText: 'Software Engineer' }).first();
    await expect(card).toBeVisible();

    // 4. Drag card to APPLIED column using KeyboardSensor
    await card.focus();
    await page.keyboard.press('Space'); // Pick up the card
    await page.waitForTimeout(200);
    
    // Press ArrowRight to move to the next column (APPLIED)
    await page.keyboard.press('ArrowRight');
    await page.waitForTimeout(200);
    
    await page.keyboard.press('Space'); // Drop the card
    
    // Wait for API call to finish before reloading
    await page.waitForTimeout(1000);

    // 5. Reload page to assert persisted status
    await page.reload();
    
    // The card should now be under APPLIED
    const appliedCol = page.getByTestId('dropzone-APPLIED');
    await expect(appliedCol.locator('text=Software Engineer').first()).toBeVisible();
  });
});
